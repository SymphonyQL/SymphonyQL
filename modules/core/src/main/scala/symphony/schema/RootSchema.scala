package symphony.schema

import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.annotation.targetName

final case class RootSchema(
  query: Option[Operation],
  mutation: Option[Operation],
  subscription: Option[Operation],
  additionalTypes: List[__Type] = Nil
) {

  def collectTypes: List[__Type] =
    (
      query.map(op => Types.collectTypes(op.opType)).toList.flatten ++
        mutation
          .map(op => Types.collectTypes(op.opType))
          .toList
          .flatten ++
        subscription
          .map(op => Types.collectTypes(op.opType))
          .toList
          .flatten
    ).groupBy(t => (t.name, t.kind, t.origin)).flatMap(_._2.headOption).toList

  @targetName("add")
  def ++(that: RootSchema): RootSchema =
    RootSchema(
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
