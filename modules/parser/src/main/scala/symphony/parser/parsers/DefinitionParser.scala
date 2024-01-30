package symphony.parser.parsers

import org.parboiled2.*
import org.parboiled2.support.hlist
import org.parboiled2.support.hlist.HNil

import symphony.parser.*
import symphony.parser.InputValue.*
import symphony.parser.SymphonyError.ParsingError
import symphony.parser.Value.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Definition.ExecutableDefinition.OperationDefinition
import symphony.parser.adt.Selection.*
import symphony.parser.adt.Type.*

open class DefinitionParser(val input: ParserInput) extends SelectionParser { self =>

  // ========================================Executable Definitions===================================================================
  def variableDefinitions: Rule1[List[VariableDefinition]] = rule {
    "(" ~!~ ignored ~ variableDefinition.*.separatedBy(ignored) ~ ignored ~ ")" ~> (_.toList)
  }

  def variableDefinition: Rule1[VariableDefinition] = rule {
    variableValue ~ ":" ~ ignored ~!~ type_ ~ ignored ~ defaultValue.? ~ ignored ~ directives.? ~> {
      (v, t, default, dirs) =>
        VariableDefinition(v.name, t, default, dirs.toList.flatten)
    }
  }

  def operationDefinition: Rule1[OperationDefinition] = rule {
    operationType ~!~ ignored ~ name.? ~ ignored ~ variableDefinitions.? ~ ignored ~ directives ~ ignored ~ selectionSet ~> {
      (operationType, name, variableDefinitions, directives, selection) =>
        {
          OperationDefinition(operationType, name, variableDefinitions.getOrElse(Nil), directives, selection)
        }
    } | ignored ~ selectionSet ~> { sel =>
      OperationDefinition(OperationType.Query, None, Nil, Nil, sel)
    }
  }

  def fragmentDefinition: Rule1[FragmentDefinition] = rule {
    "fragment" ~!~ ignored ~ fragmentName ~ ignored ~ typeCondition ~ ignored ~ directives ~ ignored ~ selectionSet ~> {
      (name, typeCondition, dirs, sel) =>
        FragmentDefinition(name, typeCondition, dirs, sel)
    }
  }

  def executableDefinition: Rule1[ExecutableDefinition] = rule {
    operationDefinition | fragmentDefinition
  }

  def definition: Rule1[Definition] = executableDefinition

  def document: Rule1[ParsedDocument] = rule {
    ignored ~ definition.*.separatedBy(ignored) ~ ignored ~ EOI ~> { seq =>
      ParsedDocument(seq.toList)
    }
  }

}
