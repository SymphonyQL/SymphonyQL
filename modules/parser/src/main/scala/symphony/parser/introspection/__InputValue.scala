package symphony.parser.introspection

import symphony.parser.{ InputValue, Parser }
import symphony.parser.Value.StringValue
import symphony.parser.adt.Definition.TypeSystemDefinition.TypeDefinition.InputValueDefinition
import symphony.parser.adt.Directive

final case class __InputValue(
  name: String,
  description: Option[String],
  `type`: () => __Type,
  defaultValue: Option[String],
  isDeprecated: Boolean = false,
  deprecationReason: Option[String] = None,
  directives: Option[List[Directive]] = None
) {

  def toInputValueDefinition: InputValueDefinition = {
    val default = defaultValue.flatMap(v => Parser.parseInputValue(v).toOption)
    val allDirectives = (if (isDeprecated)
                           List(
                             Directive(
                               "deprecated",
                               List(deprecationReason.map(reason => "reason" -> StringValue(reason))).flatten.toMap
                             )
                           )
                         else Nil) ++ directives.getOrElse(Nil)
    InputValueDefinition(description, name, _type.toType(), default, allDirectives)
  }

  private[symphony] lazy val _type: __Type = `type`()
}
