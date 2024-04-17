package symphony.execution

import symphony.parser.SymphonyQLPathValue
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.*
import symphony.parser.adt.*

final case class ExecutionFieldInfo(
  name: String,
  path: List[SymphonyQLPathValue],
  details: ExecutionField,
  directives: List[Directive] = Nil
)
