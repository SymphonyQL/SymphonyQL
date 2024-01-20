package symphony
package parser
package adt
package introspection

import Definition.TypeSystemDefinition.TypeDefinition.EnumValueDefinition
import adt.Directive
import value.Value.StringValue

case class __EnumValue(
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
