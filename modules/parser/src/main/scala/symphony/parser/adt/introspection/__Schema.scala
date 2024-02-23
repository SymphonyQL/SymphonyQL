package symphony.parser.adt.introspection

final case class __Schema(
  description: Option[String],
  types: List[__Type],
  queryType: __Type,
  mutationType: Option[__Type],
  subscriptionType: Option[__Type],
  directives: List[__Directive]
)
