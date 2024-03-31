package symphony.execution

import symphony.parser.*
import symphony.parser.SymphonyQLValue
import symphony.parser.adt.introspection.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.*
import symphony.parser.adt.*
import symphony.schema.Types

final case class ExecutionField(
  name: String,
  fieldType: __Type,
  parentType: Option[__Type],
  alias: Option[String] = None,
  fields: List[ExecutionField] = Nil,
  condition: Option[String] = None,
  arguments: Map[String, SymphonyQLInputValue] = Map(),
  directives: List[Directive] = List.empty
)

object ExecutionField {
  def apply(
    selectionSet: List[Selection],
    fragments: Map[String, FragmentDefinition],
    variableValues: Map[String, SymphonyQLInputValue],
    fieldType: __Type
  ): ExecutionField = {
    def loop(selectionSet: List[Selection], fieldType: __Type): ExecutionField = {
      val fieldList = List.newBuilder[ExecutionField]
      val innerType = Types.innerType(fieldType)
      selectionSet.foreach {
        case Field(alias, name, arguments, directives, selectionSet) if checkDirectives(directives, variableValues) =>
          val selected = innerType
            .fields(__DeprecatedArgs(Some(true)))
            .flatMap(_.find(_.name == name))

          val schemaDirectives = selected.flatMap(_.directives).getOrElse(Nil)

          val t = selected.fold(Types.string)(_.`type`()) // default only case where it's not found is __typename

          val field = loop(selectionSet, t)
          fieldList +=
            ExecutionField(
              name,
              t,
              Some(innerType),
              alias,
              field.fields,
              None,
              arguments,
              directives ++ schemaDirectives
            )
        case FragmentSpread(name, directives) if checkDirectives(directives, variableValues)                        =>
          fragments
            .get(name)
            .foreach { f =>
              val t =
                innerType.possibleTypes.flatMap(_.find(_.name.contains(f.typeCondition.name))).getOrElse(fieldType)
              fieldList ++= loop(f.selectionSet, t).fields.map(_.copy(condition = Some(f.typeCondition.name)))
            }
        case InlineFragment(typeCondition, directives, selectionSet) if checkDirectives(directives, variableValues) =>
          val t     = innerType.possibleTypes
            .flatMap(_.find(_.name.exists(typeCondition.map(_.name).contains)))
            .getOrElse(fieldType)
          val field = loop(selectionSet, t)
          typeCondition match {
            case None           => fieldList ++= field.fields
            case Some(typeName) => fieldList ++= field.fields.map(_.copy(condition = Some(typeName.name)))
          }
        case _                                                                                                      =>
      }
      ExecutionField("", fieldType, None, fields = fieldList.result())
    }

    loop(selectionSet, fieldType)
  }

  private def checkDirectives(directives: List[Directive], variableValues: Map[String, SymphonyQLInputValue]): Boolean =
    !checkDirective("skip", default = false, directives, variableValues) &&
      checkDirective("include", default = true, directives, variableValues)

  private def checkDirective(
    name: String,
    default: Boolean,
    directives: List[Directive],
    variableValues: Map[String, SymphonyQLInputValue]
  ): Boolean =
    directives
      .find(_.name == name)
      .flatMap(_.arguments.get("if")) match {
      case Some(SymphonyQLValue.BooleanValue(value))      => value
      case Some(SymphonyQLInputValue.VariableValue(name)) =>
        variableValues
          .get(name) match {
          case Some(SymphonyQLValue.BooleanValue(value)) => value
          case _                                         => default
        }
      case _                                              => default
    }
}
