package symphony.parser.adt.introspection

final case class __Introspection(
  __schema: __Schema,
  __type: TypeArgs => Option[__Type]
)
