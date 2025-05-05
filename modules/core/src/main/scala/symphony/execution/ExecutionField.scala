package symphony.execution

import symphony.parser.*
import symphony.parser.SymphonyQLValue
import symphony.parser.adt.introspection.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.*
import symphony.parser.adt.*
import symphony.schema.*

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

final case class ExecutionField(
  name: String,
  fieldType: __Type,
  parentType: Option[__Type],
  alias: Option[String] = None,
  fields: List[ExecutionField] = Nil,
  condition: Option[Set[String]] = None,
  arguments: Map[String, SymphonyQLInputValue] = Map(),
  directives: List[Directive] = List.empty
)

object ExecutionField {
  def apply(
    selectionSet: List[Selection],
    fragments: Map[String, FragmentDefinition],
    variableValues: Map[String, SymphonyQLInputValue],
    fieldType: __Type,
    rootType: RootType
  ): ExecutionField = {
    def loop(selectionSet: List[Selection], fieldType: __Type): ExecutionField = {
      val map        = mutable.Map.empty[String, Int]
      var fieldIndex = 0
      val fieldList  = ArrayBuffer.empty[ExecutionField]

      def addField(f: ExecutionField): Unit = {
        val name = f.alias.getOrElse(f.name)
        map.get(name) match {
          case None        =>
            // first time we see this field, add it to the array
            fieldList += f
            map.update(name, fieldIndex)
            fieldIndex = fieldIndex + 1
          case Some(index) =>
            // field already existed, merge it
            val existing = fieldList(index)
            fieldList(index) = existing.copy(
              fields = existing.fields ::: f.fields,
              condition = (existing.condition, f.condition) match {
                case (Some(v1), Some(v2)) => if (v1 == v2) existing.condition else Some(v1 ++ v2)
                case (Some(_), None)      => existing.condition
                case (None, Some(_))      => f.condition
                case (None, None)         => None
              }
            )
        }
      }

      val innerType = Types.innerType(fieldType)
      selectionSet.foreach {
        case Field(alias, name, arguments, directives, selectionSet) if checkDirectives(directives, variableValues) =>
          val selected = innerType
            .fields(__DeprecatedArgs(Some(true)))
            .flatMap(_.find(_.name == name))

          val schemaDirectives = selected.flatMap(_.directives).getOrElse(Nil)

          val t = selected.fold(Types.string)(_.`type`()) // default only case where it's not found is __typename

          val field = loop(selectionSet, t)
          addField(
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
          )
        case FragmentSpread(name, directives) if checkDirectives(directives, variableValues)                        =>
          fragments
            .get(name)
            .foreach { f =>
              val t =
                innerType.possibleTypes.flatMap(_.find(_.name.contains(f.typeCondition.name))).getOrElse(fieldType)
              loop(f.selectionSet, t).fields.map { field =>
                if (field.condition.isDefined) field
                else field.copy(condition = subtypeNames(f.typeCondition.name, rootType))
              }.foreach(addField)
            }
        case InlineFragment(typeCondition, directives, selectionSet) if checkDirectives(directives, variableValues) =>
          val t     = innerType.possibleTypes
            .flatMap(_.find(_.name.exists(typeCondition.map(_.name).contains)))
            .getOrElse(fieldType)
          val field = loop(selectionSet, t)
          typeCondition match {
            case None           => if (field.fields.nonEmpty) fieldList ++= field.fields
            case Some(typeName) =>
              field.fields
                .map(field =>
                  if (field.condition.isDefined) field
                  else field.copy(condition = subtypeNames(typeName.name, rootType))
                )
                .foreach(addField)
          }
        case _                                                                                                      =>
      }
      ExecutionField("", fieldType, None, fields = fieldList.toList)
    }

    loop(selectionSet, fieldType)
  }

  private def checkDirectives(directives: List[Directive], variableValues: Map[String, SymphonyQLInputValue]): Boolean =
    !checkDirective("skip", default = false, directives, variableValues) &&
      checkDirective("include", default = true, directives, variableValues)

  private def subtypeNames(typeName: String, rootType: RootType): Option[Set[String]] =
    rootType.types
      .get(typeName)
      .map(t =>
        t.possibleTypes
          .fold(Set.empty[String])(
            _.map(_.name.map(subtypeNames(_, rootType).getOrElse(Set.empty))).toSet.flatten.flatten
          ) + typeName
      )

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
