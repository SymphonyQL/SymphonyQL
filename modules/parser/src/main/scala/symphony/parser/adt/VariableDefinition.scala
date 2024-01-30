package symphony
package parser
package adt

final case class VariableDefinition(
  name: String,
  variableType: Type,
  defaultValue: Option[InputValue],
  directives: List[Directive]
)
