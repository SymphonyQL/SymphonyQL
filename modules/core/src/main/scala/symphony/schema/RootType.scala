package symphony
package schema
import parser.adt.introspection.*

final case class RootType(
  queryType: __Type,
  mutationType: Option[__Type],
  subscriptionType: Option[__Type],
  additionalDirectives: List[__Directive] = List.empty,
  description: Option[String] = None
) {
  val empty                      = List.empty[__Type]
  val types: Map[String, __Type] =
    (mutationType.toList ++ subscriptionType.toList)
      .foldLeft(Types.collectTypes(queryType)) { case (existingTypes, tpe) => Types.collectTypes(tpe, existingTypes) }
      .map(t => t.name.getOrElse("") -> t)
      .toMap
}
