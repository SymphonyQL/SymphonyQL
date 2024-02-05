package symphony.execution

import symphony.parser.SymphonyQLInputValue
import symphony.parser.adt.Definition.ExecutableDefinition.FragmentDefinition
import symphony.parser.adt.*
import symphony.schema.Stage

final case class ExecutionRequest(
  stage: Stage,
  selectionSet: List[Selection],
  fragments: Map[String, FragmentDefinition],
  variableDefinitions: List[VariableDefinition],
  variableValues: Map[String, SymphonyQLInputValue],
  operationType: OperationType
)
