package symphony.schema.scaladsl

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers.shouldEqual
import symphony.parser.adt.introspection.*
import symphony.schema.Types

def hasType(tpe: __Type, name: String, kind: TypeKind): Assertion = {
  val allTypes                         = Types.collectTypes(tpe)
  val nameKinds: Map[String, TypeKind] = allTypes.map(t => t.name.getOrElse("") -> t.kind).toMap
  val has                              = nameKinds.get(name).contains(kind)
  if (!has) {
    println(s"Actual Types: ${nameKinds.mkString("[", ",", "]")}")
  }
  has shouldEqual true
}
