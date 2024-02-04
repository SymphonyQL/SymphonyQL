package symphony.parser.adt.introspection

final case class IntrospectionDirective(
  name: String,
  description: Option[String],
  locations: Set[IntrospectionDirectiveLocation],
  args: DeprecatedArgs => List[IntrospectionInputValue],
  isRepeatable: Boolean
)
