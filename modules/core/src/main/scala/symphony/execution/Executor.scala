package symphony.execution

import scala.collection.immutable.ListMap
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.SymphonyQLSchema
import symphony.parser.*
import symphony.parser.SymphonyQLError.ExecutionError
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.OperationType.*
import symphony.parser.adt.Selection.*
import symphony.schema.*

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Executor {

  def execute(
    document: Document,
    schema: SymphonyQLSchema,
    operationName: Option[String] = None,
    variables: Map[String, SymphonyQLInputValue] = Map()
  )(implicit actorSystem: ActorSystem): Source[SymphonyQLOutputValue, NotUsed] = {
    val fragments = document.definitions.collect { case fragment: FragmentDefinition =>
      fragment.name -> fragment
    }.toMap
    getOperation(operationName, document) match {
      case Left(error) => Source.failed(ExecutionError(error))
      case Right(op)   =>
        op.operationType match {
          case Query        =>
            schema.query match
              case Some(q) =>
                executeStage(
                  q.stage,
                  op.selectionSet,
                  fragments,
                  op.variableDefinitions,
                  variables,
                  Query
                )
              case None    => Source.failed(ExecutionError("Queries are not supported on this schema"))
          case Mutation     =>
            schema.mutation match {
              case Some(m) =>
                executeStage(
                  m.stage,
                  op.selectionSet,
                  fragments,
                  op.variableDefinitions,
                  variables,
                  Mutation
                )
              case None    => Source.failed(ExecutionError("Mutations are not supported on this schema"))
            }
          case Subscription =>
            schema.subscription match {
              case Some(s) =>
                executeStage(
                  s.stage,
                  op.selectionSet,
                  fragments,
                  op.variableDefinitions,
                  variables,
                  Subscription
                )
              case None    => Source.failed(ExecutionError("Subscriptions are not supported on this schema"))
            }
        }
    }
  }

  private def drainExecutionStages(stage: ExecutionStage): Source[SymphonyQLOutputValue, NotUsed] =
    stage match
      case ExecutionStage.FutureStage(future)                                 =>
        Source.future(future).flatMapConcat(drainExecutionStages)
      case ExecutionStage.StreamStage(source)                                 =>
        val sourceStage = source.flatMapConcat(drainExecutionStages)
        Source.single(SymphonyQLOutputValue.StreamValue(sourceStage))
      case ExecutionStage.ListStage(stages)                                   =>
        val sourceList = stages.map(drainExecutionStages)
        Source.zipN(sourceList).map(s => SymphonyQLOutputValue.ListValue(s.toList))
      case ExecutionStage.ObjectStage(stages: List[(String, ExecutionStage)]) =>
        val sourceList = stages.map(kv => drainExecutionStages(kv._2).map(s => kv._1 -> s))
        Source.zipN(sourceList).map(s => SymphonyQLOutputValue.ObjectValue(s.toList))
      case PureStage(value)                                                   => Source.single(value)

  private def executeStage(
    stage: Stage,
    selectionSet: List[Selection],
    fragments: Map[String, FragmentDefinition],
    variableDefinitions: List[VariableDefinition],
    variableValues: Map[String, SymphonyQLInputValue],
    operationType: OperationType
  )(implicit actorSystem: ActorSystem): Source[SymphonyQLOutputValue, NotUsed] = {

    import actorSystem.dispatcher

    def loopExecuteStage(
      stage: Stage,
      selections: List[Selection],
      arguments: Map[String, SymphonyQLInputValue]
    ): ExecutionStage =
      stage match
        case Stage.FutureStage(future) =>
          ExecutionStage.FutureStage(future.map(loopExecuteStage(_, selections, arguments)))
        case Stage.StreamStage(source) =>
          if (operationType == OperationType.Subscription) {
            ExecutionStage.StreamStage(source.map(loopExecuteStage(_, selections, arguments)))
          } else {
            val future = source
              .runWith(Sink.fold(List.empty[Stage])((l, a) => a :: l))
              .map(_.reverse)
              .map(Stage.ListStage.apply)
            loopExecuteStage(
              Stage.FutureStage(future),
              selections,
              arguments
            )
          }

        case Stage.FunctionStage(stage)       => loopExecuteStage(stage(arguments), selections, Map())
        case Stage.ListStage(stages)          =>
          if (stages.forall(_.isInstanceOf[PureStage]))
            PureStage(SymphonyQLOutputValue.ListValue(stages.asInstanceOf[List[PureStage]].map(_.value)))
          else ExecutionStage.ListStage(stages.map(loopExecuteStage(_, selections, arguments)))
        case Stage.ObjectStage(name, _fields) =>
          val mergedSelections = mergeSelections(selections, name, fragments, variableValues)
          val fields           = mergedSelections.map {
            case Selection.Field(alias, name @ "__typename", _, _, _) =>
              alias.getOrElse(name) -> PureStage(StringValue(name))
            case Selection.Field(alias, name, args, _, selectionSet)  =>
              val arguments = extractVariables(args, variableDefinitions, variableValues)
              alias.getOrElse(name) -> _fields
                .get(name)
                .map(loopExecuteStage(_, selectionSet, arguments))
                .getOrElse(Stage.NullStage)
          }
          if (fields.map(_._2).forall(_.isInstanceOf[PureStage]))
            PureStage(
              SymphonyQLOutputValue.ObjectValue(
                fields.asInstanceOf[List[(String, PureStage)]].map(kv => kv._1 -> kv._2.value)
              )
            )
          else ExecutionStage.ObjectStage(fields)
        case p @ PureStage(value)             =>
          value match {
            case EnumValue(v) if selections.collectFirst { case Selection.Field(_, "__typename", _, _, _) =>
                  true
                }.nonEmpty =>
              val mergedSelections = mergeSelections(selections, v, fragments, variableValues)
              val obj              = mergedSelections.collectFirst { case Selection.Field(alias, name @ "__typename", _, _, _) =>
                SymphonyQLOutputValue.ObjectValue(List(alias.getOrElse(name) -> StringValue(v)))
              }
              obj.fold(p)(PureStage(_))
            case _ => p
          }
    val executionStage = loopExecuteStage(stage, selectionSet, Map())
    drainExecutionStages(executionStage)
  }

  private def extractVariables(
    arguments: Map[String, SymphonyQLInputValue],
    variableDefinitions: List[VariableDefinition],
    variableValues: Map[String, SymphonyQLInputValue]
  ): Map[String, SymphonyQLInputValue] =
    arguments.map { (k, v) =>
      k -> (v match {
        case SymphonyQLInputValue.VariableValue(name) =>
          variableValues.get(name) orElse variableDefinitions.find(_.name == name).flatMap(_.defaultValue) getOrElse v
        case value                                    => value
      })
    }

  private def mergeSelections(
    selections: List[Selection],
    name: String,
    fragments: Map[String, FragmentDefinition],
    variableValues: Map[String, SymphonyQLInputValue]
  ): List[Field] = {
    val fields = selections.flatMap {
      case field: Field                   => List(field)
      case InlineFragment(tpc, _, select) =>
        val isAvailableType = tpc.fold(true)(_.name == name)
        if (isAvailableType) mergeSelections(select, name, fragments, variableValues) else List.empty
      case FragmentSpread(spreadName, _)  =>
        fragments.get(spreadName) match {
          case Some(fragment) if fragment.typeCondition.name == name =>
            mergeSelections(fragment.selectionSet, name, fragments, variableValues)
          case _                                                     => List.empty
        }
    }
    fields
      .foldLeft(ListMap.empty[String, Field]) { (result, field) =>
        result.updated(
          field.name,
          result
            .get(field.name)
            .fold(field)(f => f.copy(selectionSet = f.selectionSet ++ field.selectionSet))
        )
      }
      .values
      .toList
  }

  private def getOperation(
    operationName: Option[String] = None,
    document: Document
  ): Either[String, OperationDefinition] =
    operationName match {
      case Some(name) =>
        document.definitions.collectFirst { case op: OperationDefinition if op.name.contains(name) => op }
          .toRight(s"Unknown operation $name.")
      case None       =>
        document.definitions.collect { case op: OperationDefinition => op } match {
          case head :: Nil => Right(head)
          case _           => Left("Operation name is required.")
        }
    }

}
