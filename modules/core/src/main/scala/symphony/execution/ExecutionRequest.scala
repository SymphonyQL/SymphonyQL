package symphony.execution

import symphony.parser.SymphonyQLInputValue
import symphony.parser.adt.Definition.ExecutableDefinition.FragmentDefinition
import symphony.parser.adt.*
import symphony.schema.Stage

final case class ExecutionRequest(
  stage: Stage,
  currentField: ExecutionField,
  variableDefinitions: List[VariableDefinition],
  variableValues: Map[String, SymphonyQLInputValue],
  operationType: OperationType
)
