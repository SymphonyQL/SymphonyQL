package symphony

import symphony.parser.introspection.*
import symphony.schema.*

final case class SymphonyQLSchema(
  query: Option[Operation],
  mutation: Option[Operation],
  subscription: Option[Operation],
  additionalTypes: List[__Type] = Nil
) {

  def ++(that: SymphonyQLSchema): SymphonyQLSchema =
    SymphonyQLSchema(
      (query ++ that.query).reduceOption(_ ++ _),
      (mutation ++ that.mutation).reduceOption(_ ++ _),
      (subscription ++ that.subscription).reduceOption(_ ++ _)
    )

  def types: List[__Type] = {
    val empty = additionalTypes
    (query.map(_.opType).fold(empty)(Types.collectTypes(_)) ++
      mutation.map(_.opType).fold(empty)(Types.collectTypes(_)) ++
      subscription.map(_.opType).fold(empty)(Types.collectTypes(_)))
      .groupBy(t => (t.name, t.kind, t.origin))
      .flatMap(_._2.headOption)
      .toList
  }
}
