package symphony.execution

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.parser.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.OperationType.*
import symphony.parser.adt.Selection.*
import symphony.schema.*

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext

object Executor {

  def executeRequest(
    request: ExecutionRequest
  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Source[SymphonyQLOutputValue, NotUsed] = {

    def loopExecuteStage(
      stage: Stage,
      currentField: ExecutionField,
      arguments: Map[String, SymphonyQLInputValue]
    ): ExecutionStage =
      stage match
        case Stage.FutureStage(future)        =>
          ExecutionStage.FutureStage(future.map(loopExecuteStage(_, currentField, arguments)))
        case Stage.SourceStage(source)   =>
          if (request.operationType == OperationType.Subscription) {
            ExecutionStage.SourceStage(source.map(loopExecuteStage(_, currentField, arguments)))
          } else {
            val future = source.runWith(Sink.seq[Stage]).map(s => Stage.ListStage(s.toList))
            loopExecuteStage(
              Stage.FutureStage(future),
              currentField,
              arguments
            )
          }
        case Stage.FunctionStage(stage)       => loopExecuteStage(stage(arguments), currentField, Map())
        case Stage.ListStage(stages)          =>
          if (stages.forall(_.isInstanceOf[PureStage]))
            PureStage(SymphonyQLOutputValue.ListValue(stages.asInstanceOf[List[PureStage]].map(_.value)))
          else ExecutionStage.ListStage(stages.map(loopExecuteStage(_, currentField, arguments)))
        case Stage.ObjectStage(name, _fields) =>
          val filteredFields = filterFields(currentField, name)
          val fields         = filteredFields.map {
            case ExecutionField(name @ "__typename", _, _, alias, _, _, _, _) =>
              alias.getOrElse(name) -> PureStage(StringValue(name))
            case f @ ExecutionField(name, _, _, alias, _, _, args, _)         =>
              val arguments = extractVariables(args, request.variableDefinitions, request.variableValues)
              alias.getOrElse(name) -> _fields
                .get(name)
                .map(loopExecuteStage(_, f, arguments))
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
            case EnumValue(v) =>
              val obj = filterFields(currentField, v).collectFirst {
                case ExecutionField(name @ "__typename", _, _, alias, _, _, _, _) =>
                  SymphonyQLOutputValue.ObjectValue(List(alias.getOrElse(name) -> StringValue(v)))
                case f: ExecutionField if f.name == "_" =>
                  NullValue  
              }
              obj.fold(p)(PureStage(_))
            case _ => p
          }

    val executionStage = loopExecuteStage(request.stage, request.currentField, Map())
    drainExecutionStages(executionStage)
  }

  private def drainExecutionStages(stage: ExecutionStage): Source[SymphonyQLOutputValue, NotUsed] =
    stage match
      case ExecutionStage.FutureStage(future)                                 =>
        Source.future(future).flatMapConcat(drainExecutionStages)
      case ExecutionStage.SourceStage(source)                            =>
        val sourceStage = source.flatMapConcat(drainExecutionStages)
        Source.single(SymphonyQLOutputValue.StreamValue(sourceStage))
      case ExecutionStage.ListStage(stages)                                   =>
        val sourceList = stages.map(drainExecutionStages)
        Source.zipN(sourceList).map(s => SymphonyQLOutputValue.ListValue(s.toList))
      case ExecutionStage.ObjectStage(stages: List[(String, ExecutionStage)]) =>
        val sourceList = stages.map(kv => drainExecutionStages(kv._2).map(s => kv._1 -> s))
        Source.zipN(sourceList).map(s => SymphonyQLOutputValue.ObjectValue(s.toList))
      case PureStage(value)                                                   => Source.single(value)

  private def extractVariables(
    arguments: Map[String, SymphonyQLInputValue],
    variableDefinitions: List[VariableDefinition],
    variableValues: Map[String, SymphonyQLInputValue]
  ): Map[String, SymphonyQLInputValue] = {
    def extractVariable(value: SymphonyQLInputValue): SymphonyQLInputValue =
      value match {
        case SymphonyQLInputValue.ListValue(values)   => SymphonyQLInputValue.ListValue(values.map(extractVariable))
        case SymphonyQLInputValue.ObjectValue(fields) =>
          SymphonyQLInputValue.ObjectValue(fields.map { case (k, v) => k -> extractVariable(v) })
        case SymphonyQLInputValue.VariableValue(name) =>
          lazy val defaultInputValue = (for {
            definition <- variableDefinitions.find(_.name == name)
            inputValue <- definition.defaultValue
          } yield inputValue).getOrElse(NullValue)
          variableValues.getOrElse(name, defaultInputValue)
        case value: SymphonyQLValue                   => value
      }

    arguments.map { case (k, v) => k -> extractVariable(v) }
  }

  private def filterFields(field: ExecutionField, typeName: String): List[ExecutionField] =
    field.fields.filter(_.condition.forall(_.contains(typeName)))
}
