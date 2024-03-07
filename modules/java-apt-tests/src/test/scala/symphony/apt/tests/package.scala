package symphony.apt.tests

import symphony.parser.SourceMapper
import symphony.parser.adt.Definition.TypeSystemDefinition.SchemaDefinition
import symphony.parser.adt.Document
import symphony.parser.adt.introspection.__Type
import symphony.schema.*

def getDocument[A](schema: Schema[A], isInput: Boolean = false): Document =
  Document(
    Types.collectTypes(schema.lazyType(isInput)).flatMap(_.toTypeDefinition.toList),
    SourceMapper.empty
  )
