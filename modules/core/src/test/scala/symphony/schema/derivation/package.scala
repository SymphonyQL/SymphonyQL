package symphony.schema.derivation

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers.shouldEqual
import symphony.parser.adt.introspection.*
import symphony.annotations.scala.*
import symphony.parser.SourceMapper
import symphony.parser.adt.Document
import symphony.schema.*

import scala.concurrent.Future

def hasType(tpe: __Type, name: String, kind: __TypeKind): Assertion = {
  val allTypes                           = Types.collectTypes(tpe)
  val nameKinds: Map[String, __TypeKind] = allTypes.map(t => t.name.getOrElse("") -> t.kind).toMap
  val has                                = nameKinds.get(name).contains(kind)
  if (!has) {
    println(s"Actual Types: ${nameKinds.mkString("[", ",", "]")}")
  }
  has shouldEqual true
}

case class FutureFieldSchema(q: Future[Int])

@GQLInterface
sealed trait MyInterface
object MyInterface {
  case class A(common: Int, different: String)  extends MyInterface
  case class B(common: Int, different: Boolean) extends MyInterface
}

@GQLUnion
sealed trait EnumLikeUnion
object EnumLikeUnion {
  case object A extends EnumLikeUnion
  case object B extends EnumLikeUnion
}

@GQLInterface
sealed trait EnumLikeInterface
object EnumLikeInterface {
  case object A extends EnumLikeInterface
  case object B extends EnumLikeInterface
}

@GQLInterface
sealed trait NestedInterface

object NestedInterface {

  @GQLInterface
  sealed trait Mid1 extends NestedInterface

  @GQLInterface
  sealed trait Mid2 extends NestedInterface

  case class FooA(a: String, b: String, c: String) extends Mid1

  case class FooB(b: String, c: String, d: String) extends Mid1 with Mid2

  case class FooC(b: String, d: String, e: String) extends Mid2
}

def getDocument[A](schema: Schema[A], isInput: Boolean = false): Document =
  Document(
    Types.collectTypes(schema.lazyType(isInput)).flatMap(_.toTypeDefinition.toList),
    SourceMapper.empty
  )
