package symphony.parser.adt.introspection

final case class Introspection(
  schema: IntrospectionSchema,
  tpe: TypeArgs => Option[IntrospectionType]
)
