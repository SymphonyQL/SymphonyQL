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
import scala.util.control.*

object Executor {

  def executeRequest(
    request: ExecutionRequest
  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): ExecutionOutputValue = {

    val errors = mutable.ListBuffer.empty[SymphonyQLError]

    def handleError(step: => Stage): Stage =
      try step
      catch {
        case NonFatal(e) => Stage.SourceStage(Source.failed(e))
      }

    def loopExecuteStage(
      stage: Stage,
      currentField: ExecutionField,
      arguments: Map[String, SymphonyQLInputValue],
      path: List[SymphonyQLPathValue]
    ): ExecutionStage =
      stage match
        case Stage.FutureStage(future)        =>
          ExecutionStage.FutureStage(future.map(loopExecuteStage(_, currentField, arguments, path)))
        case Stage.SourceStage(source)        =>
          if (request.operationType == OperationType.Subscription) {
            ExecutionStage.SourceStage(source.map(loopExecuteStage(_, currentField, arguments, path)))
          } else {
            val future = source.runWith(Sink.seq[Stage]).map(s => Stage.ListStage(s.toList))
            loopExecuteStage(
              Stage.FutureStage(future),
              currentField,
              arguments,
              path
            )
          }
        case Stage.FunctionStage(stage)       => loopExecuteStage(handleError(stage(arguments)), currentField, Map(), path)
        case Stage.ListStage(stages)          =>
          reduceList(
            stages.zipWithIndex.map { (stage, i) =>
              loopExecuteStage(stage, currentField, arguments, SymphonyQLPathValue.Index(i) :: path)
            },
            Types.listOf(currentField.fieldType).fold(false)(_.isNullable)
          )
        case Stage.ObjectStage(name, _fields) =>
          val filteredFields = filterFields(currentField, name)
          val fields         = filteredFields.map {
            case f @ ExecutionField(name @ "__typename", _, _, alias, _, _, _, directives) =>
              (alias.getOrElse(name), PureStage(StringValue(name)), executionFieldInfo(f, path, directives))
            case f @ ExecutionField(name, _, _, alias, _, _, args, directives)             =>
              val arguments = extractVariables(args, request.variableDefinitions, request.variableValues)
              (
                alias.getOrElse(name),
                _fields
                  .get(name)
                  .map(loopExecuteStage(_, f, arguments, SymphonyQLPathValue.Key(f.alias.getOrElse(f.name)) :: path))
                  .getOrElse(Stage.NullStage),
                executionFieldInfo(f, path, directives)
              )
          }
          if (fields.map(_._2).forall(_.isInstanceOf[PureStage]))
            PureStage(
              SymphonyQLOutputValue.ObjectValue(
                fields.asInstanceOf[List[(String, PureStage, ExecutionFieldInfo)]].map(kv => kv._1 -> kv._2.value)
              )
            )
          else ExecutionStage.ObjectStage(fields)
        case p @ PureStage(value)             =>
          value match {
            case EnumValue(v) =>
              val obj = filterFields(currentField, v).collectFirst {
                case ExecutionField(name @ "__typename", _, _, alias, _, _, _, _) =>
                  SymphonyQLOutputValue.ObjectValue(List(alias.getOrElse(name) -> StringValue(v)))
                case f: ExecutionField if f.name == "_"                           =>
                  NullValue
              }
              obj.fold(p)(PureStage(_))
            case _            => p
          }

    val executionStage = loopExecuteStage(request.stage, request.currentField, Map(), List.empty)
    ExecutionOutputValue(drainExecutionStages(executionStage, errors), errors)
  }

  private def drainExecutionStages(
    stage: ExecutionStage,
    errors: mutable.ListBuffer[SymphonyQLError]
  ): Source[SymphonyQLOutputValue, NotUsed] =

    def handleError(
      error: Throwable,
      isNullable: Boolean,
      path: List[SymphonyQLPathValue]
    ): Source[SymphonyQLOutputValue, NotUsed] =
      if (isNullable) {
        errors.append(mapExecutionError(error, path))
        Source.single(NullValue)
      } else Source.failed(mapExecutionError(error, path))

    stage match
      case ExecutionStage.FutureStage(future)                                                     =>
        Source
          .future(future)
          .flatMapConcat(drainExecutionStages(_, errors))
          .recoverWith(handleError(_, true, List.empty))
      case ExecutionStage.SourceStage(source)                                                     =>
        val sourceStage =
          source.flatMapConcat(drainExecutionStages(_, errors)).recoverWith(handleError(_, true, List.empty))
        Source.single(SymphonyQLOutputValue.StreamValue(sourceStage))
      case ExecutionStage.ListStage(stages, areItemsNullable)                                     =>
        val sourceList =
          stages.map(drainExecutionStages(_, errors).recoverWith(handleError(_, areItemsNullable, List.empty)))
        Source.zipN(sourceList).map(s => SymphonyQLOutputValue.ListValue(s.toList))
      case ExecutionStage.ObjectStage(stages: List[(String, ExecutionStage, ExecutionFieldInfo)]) =>
        val sourceList = stages.map(kv =>
          drainExecutionStages(kv._2, errors)
            .recoverWith(handleError(_, kv._3.details.fieldType.isNullable, kv._3.path))
            .map(s => kv._1 -> s)
        )
        Source.zipN(sourceList).map(s => SymphonyQLOutputValue.ObjectValue(s.toList))
      case PureStage(value)                                                                       => Source.single(value)

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

  private def executionFieldInfo(
    field: ExecutionField,
    path: List[SymphonyQLPathValue],
    fieldDirectives: List[Directive]
  ): ExecutionFieldInfo =
    ExecutionFieldInfo(field.alias.getOrElse(field.name), path, field, fieldDirectives)

  private def reduceList(list: List[ExecutionStage], areItemsNullable: Boolean): ExecutionStage =
    if (list.forall(_.isInstanceOf[PureStage]))
      PureStage(SymphonyQLOutputValue.ListValue(list.asInstanceOf[List[PureStage]].map(_.value)))
    else ExecutionStage.ListStage(list, areItemsNullable)

  private def filterFields(field: ExecutionField, typeName: String): List[ExecutionField] =
    field.fields.filter(_.condition.forall(_.contains(typeName)))

  private def mapExecutionError(cause: Throwable, path: List[SymphonyQLPathValue]): SymphonyQLError.ExecutionError =
    cause match {
      case e: SymphonyQLError.ExecutionError => e.copy(path = path.reverse)
      case other                             =>
        SymphonyQLError.ExecutionError("Execution failure", innerThrowable = Option(other), path = path.reverse)
    }
}
