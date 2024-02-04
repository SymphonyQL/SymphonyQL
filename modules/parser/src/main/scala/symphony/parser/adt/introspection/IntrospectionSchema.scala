package symphony.parser.adt.introspection

final case class IntrospectionSchema(
  description: Option[String],
  queryType: IntrospectionType,
  mutationType: Option[IntrospectionType],
  subscriptionType: Option[IntrospectionType],
  types: List[IntrospectionType],
  directives: List[IntrospectionDirective]
)
