package symphony.schema.derivation

import scala.compiletime.*
import scala.quoted.*
import symphony.parser.adt.introspection.*
import symphony.schema.*

object Utils {

  inline def printTree[T](inline any: T): T                                    = ${ printDerived('any) }
  private def printDerived[T: Type](any: Expr[T])(using qctx: Quotes): Expr[T] = {
    import qctx.reflect.*
    println(Printer.TreeShortCode.show(any.asTerm))
    any
  }

  def customInputTypeName(name: String): String = s"${name}Input"

  // see https://github.com/graphql/graphql-spec/issues/568
  def fixEmptyUnionObject(t: __Type): __Type =
    t.fields(__DeprecatedArgs(Some(true))) match {
      case Some(Nil) =>
        t.copy(
          fields = (_: __DeprecatedArgs) =>
            Some(
              List(
                __Field(
                  "_",
                  Some(
                    "SymphonyQL does not support empty objects. Do not query, use __typename instead."
                  ),
                  _ => Nil,
                  () => Types.mkScalar("Boolean")
                )
              )
            )
        )
      case _         => t
    }
}
