package symphony.parser.introspection

final case class __Directive(
  name: String,
  description: Option[String],
  locations: Set[__DirectiveLocation],
  args: __DeprecatedArgs => List[__InputValue],
  isRepeatable: Boolean
)
