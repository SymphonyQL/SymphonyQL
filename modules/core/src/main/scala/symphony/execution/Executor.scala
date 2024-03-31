package symphony.execution

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.control.*

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*

import symphony.parser.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.OperationType.*
import symphony.schema.*

object Executor {

  def executeRequest(
    request: ExecutionRequest
  )(implicit
    actorSystem: ActorSystem,
    ec: ExecutionContext
  ): ExecutionOutputValue = {

    val errors = mutable.ListBuffer.empty[SymphonyQLError]

    def loopExecuteStage(
      stage: Stage,
      currentField: ExecutionField,
      arguments: Map[String, SymphonyQLInputValue],
      path: List[SymphonyQLPathValue]
    ): ExecutionStage =
      stage match
        case s: Stage.SourceStage       =>
          reduceSourceStage(s, currentField, arguments, path)
        case s: Stage.ObjectStage       => reduceObjectStage(s, currentField, arguments, path)
        case Stage.ListStage(stages)    =>
          reduceListStage(
            stages.zipWithIndex.map { (stage, i) =>
              loopExecuteStage(stage, currentField, arguments, SymphonyQLPathValue.Index(i) :: path)
            },
            Types.listOf(currentField.fieldType).fold(false)(_.isNullable)
          )
        case p @ PureStage(value)       =>
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
        case Stage.FutureStage(future)  =>
          ExecutionStage.FutureStage(future.map(loopExecuteStage(_, currentField, arguments, path)))
        case Stage.FunctionStage(stage) => loopExecuteStage(handleError(stage(arguments)), currentField, Map(), path)

    end loopExecuteStage

    def reduceListStage(list: List[ExecutionStage], areItemsNullable: Boolean): ExecutionStage =
      if (list.forall(_.isInstanceOf[PureStage]))
        PureStage(SymphonyQLOutputValue.ListValue(list.asInstanceOf[List[PureStage]].map(_.value)))
      else ExecutionStage.ListStage(list, areItemsNullable)

    def reduceSourceStage(
      stage: Stage.SourceStage,
      currentField: ExecutionField,
      arguments: Map[String, SymphonyQLInputValue],
      path: List[SymphonyQLPathValue]
    ): ExecutionStage =
      val source = stage.source
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
    end reduceSourceStage

    def reduceObjectStage(
      stage: Stage.ObjectStage,
      currentField: ExecutionField,
      arguments: Map[String, SymphonyQLInputValue],
      path: List[SymphonyQLPathValue]
    ): ExecutionStage =
      val name           = stage.name
      val _fields        = stage.fields
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
    end reduceObjectStage

    val executionStage = loopExecuteStage(request.stage, request.currentField, Map(), List.empty)
    ExecutionOutputValue(drainExecutionStages(executionStage, errors), errors)
  }

  private def handleError(stage: => Stage): Stage =
    try stage
    catch {
      case NonFatal(e) => Stage.SourceStage(Source.failed(e))
    }

  private def drainExecutionStages(
    stage: ExecutionStage,
    errors: mutable.ListBuffer[SymphonyQLError]
  ): Source[SymphonyQLOutputValue, NotUsed] =
    stage match
      case ExecutionStage.FutureStage(future)                                                     =>
        Source
          .future(future)
          .flatMapConcat(drainExecutionStages(_, errors))
          .recoverWith(handleError(errors, _, true, List.empty))
      case ExecutionStage.SourceStage(source)                                                     =>
        Source.single(
          SymphonyQLOutputValue.StreamValue(
            source.flatMapConcat(drainExecutionStages(_, errors)).recoverWith(handleError(errors, _, true, List.empty))
          )
        )
      case ExecutionStage.ListStage(stages, areItemsNullable)                                     =>
        Source
          .zipN(
            stages.map(
              drainExecutionStages(_, errors).recoverWith(handleError(errors, _, areItemsNullable, List.empty))
            )
          )
          .map(s => SymphonyQLOutputValue.ListValue(s.toList))
      case ExecutionStage.ObjectStage(stages: List[(String, ExecutionStage, ExecutionFieldInfo)]) =>
        Source
          .zipN(
            stages.map(kv =>
              drainExecutionStages(kv._2, errors)
                .recoverWith(handleError(errors, _, kv._3.details.fieldType.isNullable, kv._3.path))
                .map(s => kv._1 -> s)
            )
          )
          .map(s => SymphonyQLOutputValue.ObjectValue(s.toList))
      case PureStage(value)                                                                       => Source.single(value)
  end drainExecutionStages

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

  private def filterFields(field: ExecutionField, typeName: String): List[ExecutionField] =
    field.fields.filter(_.condition.forall(_.contains(typeName)))

  private def mapExecutionError(cause: Throwable, path: List[SymphonyQLPathValue]): SymphonyQLError.ExecutionError =
    cause match {
      case e: SymphonyQLError.ExecutionError => e.copy(path = path.reverse)
      case other                             =>
        SymphonyQLError.ExecutionError("Execution failure", innerThrowable = Option(other), path = path.reverse)
    }

  private def handleError(
    errors: mutable.ListBuffer[SymphonyQLError],
    error: Throwable,
    isNullable: Boolean,
    path: List[SymphonyQLPathValue]
  ): Source[SymphonyQLOutputValue, NotUsed] =
    if (isNullable) {
      errors.append(mapExecutionError(error, path))
      Source.single(NullValue)
    } else Source.failed(mapExecutionError(error, path))
}
