package symphony.parser.adt.introspection

final case class __Introspection(
  __schema: __Schema,
  __type: __TypeArgs => Option[__Type]
)
