package symphony.parser

import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.OperationDefinition
import symphony.parser.adt.OperationType.Query
import symphony.parser.adt.Selection.Field
import symphony.parser.value.*

def simpleQuery(
  name: Option[String] = None,
  variableDefinitions: List[VariableDefinition] = Nil,
  directives: List[Directive] = Nil,
  selectionSet: List[Selection] = Nil,
  sourceMapper: SourceMapper = SourceMapper.empty
): Document =
  Document(List(OperationDefinition(Query, name, variableDefinitions, directives, selectionSet)), sourceMapper)

def simpleField(
  name: String,
  alias: Option[String] = None,
  arguments: Map[String, InputValue] = Map(),
  directives: List[Directive] = Nil,
  selectionSet: List[Selection] = Nil
): Field = Field(alias, name, arguments, directives, selectionSet)
