package symphony.schema.derivation

import magnolia1.TypeInfo
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.annotations.scala.*

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

  def getName(info: TypeInfo): String =
    info.typeParams match {
      case Nil  => info.short
      case args => info.short + args.map(getName).mkString
    }

  def getName(annotations: Seq[Any], label: String): String =
    annotations.collectFirst { case GQLName(name) => name }.getOrElse(label)

  def getName(annotations: Seq[Any], info: TypeInfo): String =
    annotations.collectFirst { case GQLName(name) => name }.getOrElse {
      info.typeParams match {
        case Nil  => info.short
        case args => info.short + args.map(getName(Nil, _)).mkString
      }
    }

  def getDescription(annotations: Seq[Any]): Option[String] =
    annotations.collectFirst { case GQLDescription(desc) => desc }
}
