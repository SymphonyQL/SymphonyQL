package symphony.schema.derivation

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import symphony.*
import symphony.schema.*
import symphony.schema.derivation.*
import symphony.annotations.scala.*
import symphony.parser.DocumentRenderer
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

    it("it should render descriptions") {
      case class UserTest(name: String, @GQLDescription("field-description") age: Int)
      case class UserComplex(id: Int, user: UserTest)
      case class UserParams(nameLike: String, @GQLDescription("is user active currently") active: Boolean)
      case class QueryTest(allUsers: () => List[UserTest])
      case class MutationTest(id: UserComplex => Boolean, fetch: UserParams => Boolean)
      val graphql   =
        SymphonyQL
          .newSymphonyQL()
          .rootResolver(
            SymphonyQLResolver(
              QueryTest(() => List())                 -> Schema.derived[QueryTest],
              MutationTest(c => true, params => true) -> Schema.derived[MutationTest]
            )
          )
          .build()
      val generated = graphql.render.trim
      assertResult("""|schema {
                      |  query: QueryTest
                      |  mutation: MutationTest
                      |}
                      |
                      |input UserTestInput {
                      |  name: String!
                      |  "field-description"
                      |  age: Int!
                      |}
                      |
                      |type MutationTest {
                      |  id(id: Int!, user: UserTestInput!): Boolean!
                      |  fetch(nameLike: String!, "is user active currently" active: Boolean!): Boolean!
                      |}
                      |
                      |type QueryTest {
                      |  allUsers: [UserTest!]!
                      |}
                      |
                      |type UserTest {
                      |  name: String!
                      |  "field-description"
                      |  age: Int!
                      |}""".stripMargin.trim)(generated)
    }

    it("nested types") {
      case class Queries(a: Generic[Option[Double]], b: Generic[Option[Int]])
      case class Generic[T](value: T)
      hasType(Schema[Queries].lazyType(), "GenericOptionDouble", __TypeKind.OBJECT)
    }

    it("GQLExcluded") {
      case class QueryType(a: String, @GQLExcluded b: String)
      case class Query(query: QueryType)
      val graphql  =
        SymphonyQL
          .newSymphonyQL()
          .rootResolver(
            SymphonyQLResolver(
              Query(QueryType("a", "b")) -> Schema.derived[Query]
            )
          )
          .build()
      val expected =
        """schema {
          |  query: Query
          |}

          |type Query {
          |  query: QueryType!
          |}

          |type QueryType {
          |  a: String!
          |}""".stripMargin
      assertResult(expected)(graphql.render)
    }

    it("enum-like sealed traits annotated with GQLUnion") {
      val doc = getDocument(Schema[EnumLikeUnion])
      DocumentRenderer.render(doc).trim shouldEqual
        // https://spec.graphql.org/October2021/#sec-Objects
        // An Object type must define one or more fields.
        """union EnumLikeUnion = A | B
          |
          |type A {
          |  "SymphonyQL does not support empty objects. Do not query, use __typename instead."
          |  _: Boolean
          |}
          |
          |type B {
          |  "SymphonyQL does not support empty objects. Do not query, use __typename instead."
          |  _: Boolean
          |}""".stripMargin

    }

    it("enum-like sealed traits annotated with GQLInterface") {
      val doc = getDocument(Schema[EnumLikeInterface])
      // https://spec.graphql.org/October2021/#sec-Interfaces
      // An Interface type must define one or more fields.
      DocumentRenderer.render(doc).trim shouldEqual
        """interface EnumLikeInterface
          |
          |type A implements EnumLikeInterface
          |
          |type B implements EnumLikeInterface""".stripMargin
    }

    it("nested interfaces") {
      val doc = getDocument(Schema[NestedInterface])
      DocumentRenderer.render(doc).trim shouldEqual
        """interface Mid1 implements NestedInterface {
          |  b: String!
          |  c: String!
          |}
          |
          |interface Mid2 implements NestedInterface {
          |  b: String!
          |  d: String!
          |}
          |
          |interface NestedInterface {
          |  b: String!
          |}
          |
          |type FooA implements Mid1 {
          |  a: String!
          |  b: String!
          |  c: String!
          |}
          |
          |type FooB implements Mid1 & Mid2 {
          |  b: String!
          |  c: String!
          |  d: String!
          |}
          |
          |type FooC implements Mid2 {
          |  b: String!
          |  d: String!
          |  e: String!
          |}""".stripMargin
    }
  }

}
