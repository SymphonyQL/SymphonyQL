package symphony.parser

import symphony.parser.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.{ FragmentDefinition, OperationDefinition }
import symphony.parser.adt.OperationType.Query
import symphony.parser.adt.Selection.Field

def simpleQuery(
  name: Option[String] = None,
  variableDefinitions: List[VariableDefinition] = List.empty,
  directives: List[Directive] = List.empty,
  selectionSet: List[Selection] = List.empty,
  sourceMapper: SourceMapper = SourceMapper.empty
): Document =
  Document(List(OperationDefinition(Query, name, variableDefinitions, directives, selectionSet)), sourceMapper)

def simpleQueryWithFragment(
  name: Option[String] = None,
  variableDefinitions: List[VariableDefinition] = List.empty,
  directives: List[Directive] = List.empty,
  selectionSet: List[Selection] = List.empty,
  fragment: FragmentDefinition,
  sourceMapper: SourceMapper = SourceMapper.empty
): Document =
  Document(
    List(OperationDefinition(Query, name, variableDefinitions, directives, selectionSet), fragment),
    sourceMapper
  )

def mkSimpleField(
  name: String,
  alias: Option[String] = None,
  arguments: Map[String, SymphonyQLInputValue] = Map(),
  directives: List[Directive] = List.empty,
  selectionSet: List[Selection] = List.empty
): Field = Field(alias, name, arguments, directives, selectionSet)
