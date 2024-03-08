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
