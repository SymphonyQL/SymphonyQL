package symphony
package parser
package adt

import value.InputValue

final case class VariableDefinition(
  name: String,
  variableType: Type,
  defaultValue: Option[InputValue],
  directives: List[Directive]
)
