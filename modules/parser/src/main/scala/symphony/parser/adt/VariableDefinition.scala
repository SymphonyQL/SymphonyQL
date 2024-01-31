package symphony
package parser
package adt

final case class VariableDefinition(
  name: String,
  variableType: Type,
  defaultValue: Option[SymphonyQLInputValue],
  directives: List[Directive]
)
