package symphony.schema.scaladsl

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import symphony.*
import symphony.schema.*
import symphony.schema.scaladsl.*
import symphony.parser.adt.introspection.*

class SchemaSpec extends AnyFunSpec with Matchers {

  case class UserTest(name: String, age: Int)
  case class UserComplex(id: Int, user: UserTest)
  case class UserParams(nameLike: String, active: Boolean)

  case class QueryTest(allUsers: Int => List[UserTest])
  case class MutationTest(
    id: UserComplex => Boolean,
    fetch: UserParams => Boolean
  )

  val graphql =
    SymphonyQL
      .newSymphonyQL()
      .rootResolver(
        SymphonyQLResolver(
          QueryTest((a) => List())                -> Schema.derived[QueryTest],
          MutationTest(c => true, params => true) -> Schema.derived[MutationTest]
        )
      )
      .build()

  describe("Schema and Render") {
    it("render doc") {
      val document = graphql.render.trim
      println(document)
      document shouldEqual """schema {
                             |  query: QueryTest
                             |  mutation: MutationTest
                             |}
                             |
                             |input UserTestInput {
                             |  name: String!
                             |  age: Int!
                             |}
                             |
                             |type MutationTest {
                             |  id(id: Int!, user: UserTestInput!): Boolean!
                             |  fetch(nameLike: String!, active: Boolean!): Boolean!
                             |}
                             |
                             |type QueryTest {
                             |  allUsers(value: Int!): [UserTest!]!
                             |}
                             |
                             |type UserTest {
                             |  name: String!
                             |  age: Int!
                             |}""".stripMargin
    }

    it("nested input types") {
      case class Queries(a: A => Unit)
      case class A(b: B)
      case class B(c: C)
      case class C(d: Int)
      hasType(Schema[Queries].lazyType(), "BInput", __TypeKind.INPUT_OBJECT)
    }

    it("nested types") {
      case class Queries(a: Generic[Option[Double]], b: Generic[Option[Int]])
      case class Generic[T](value: T)
      hasType(Schema[Queries].lazyType(), "GenericOptionDouble", __TypeKind.OBJECT)
    }
  }

}
