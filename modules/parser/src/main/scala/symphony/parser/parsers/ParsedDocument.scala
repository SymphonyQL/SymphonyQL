package symphony.parser.parsers

import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*

final case class ParsedDocument(definitions: List[Definition])
