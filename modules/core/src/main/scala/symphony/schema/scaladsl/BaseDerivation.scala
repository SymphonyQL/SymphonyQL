package symphony.schema.scaladsl

import magnolia1.TypeInfo
import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.quoted.*

object BaseDerivation extends BaseDerivation {
  inline def implicitExists[T]: Boolean = ${ implicitExistsImpl[T] }
}

trait BaseDerivation {

  def implicitExistsImpl[T: Type](using q: Quotes): Expr[Boolean] = {
    import q.reflect.*
    Implicits.search(TypeRepr.of[T]) match {
      case _: ImplicitSearchSuccess => Expr(true)
      case _: ImplicitSearchFailure => Expr(false)
    }
  }

  def customInputTypeName(name: String): String = s"${name}Input"

  // see https://github.com/graphql/graphql-spec/issues/568
  def fixEmptyUnionObject(t: IntrospectionType): IntrospectionType =
    t.fields(DeprecatedArgs(Some(true))) match {
      case Some(Nil) =>
        t.copy(
          fields = (_: DeprecatedArgs) =>
            Some(
              List(
                IntrospectionField(
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

  def getName(info: TypeInfo): String =
    info.typeParams match {
      case Nil  => info.short
      case args => info.short + args.map(getName).mkString
    }
}
