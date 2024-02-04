package symphony.parser.adt.introspection

import symphony.parser.SymphonyQLValue.StringValue
import symphony.parser.adt.Definition.TypeSystemDefinition.TypeDefinition.EnumValueDefinition
import symphony.parser.adt.Directive

final case class IntrospectionEnumValue(
  name: String,
  description: Option[String],
  isDeprecated: Boolean,
  deprecationReason: Option[String],
  directives: Option[List[Directive]]
) {

  def toEnumValueDefinition: EnumValueDefinition =
    EnumValueDefinition(
      description,
      name,
      (if (isDeprecated)
         List(
           Directive(
             "deprecated",
             List(deprecationReason.map(reason => "reason" -> StringValue(reason))).flatten.toMap
           )
         )
       else Nil) ++ directives.getOrElse(Nil)
    )
}
