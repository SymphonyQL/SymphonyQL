package symphony.schema.scaladsl

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.*
import symphony.parser.*
import symphony.parser.adt.introspection.__TypeKind
import symphony.schema.*

import scala.concurrent.Future

class AutoGenSpec extends AnyFunSpec with Matchers {

  // test classes
  final case class UserQueryInput(id: String)

  final case class UserOutput(id: String, username: String)

  final case class SourceQueryResolver(
    getUsers: UserQueryInput => Source[UserOutput, NotUsed]
  )

  final case class SimpleUserQueryResolver(
    user: UserOutput,
    userFuture: Future[UserOutput]
  )

  describe("Simple Derivation") {
    it("derives simple input schema") {
      val inputSchema = Schema.derived[UserQueryInput]
      hasType(inputSchema.lazyType(true), "UserQueryInputInput", __TypeKind.INPUT_OBJECT)
    }

    it("derives simple output schema") {
      val outputSchema = Schema.derived[UserOutput]
      hasType(outputSchema.lazyType(), "UserOutput", __TypeKind.OBJECT)
    }

    it("derives simple func schema") {
      val funSchema = Schema.mkFunction(
        ArgumentExtractor[UserQueryInput],
        Schema[UserQueryInput],
        Schema[UserOutput]
      )
      hasType(funSchema.lazyType(), "UserOutput", __TypeKind.OBJECT)
    }

    it("derives simple root schema with simple resolvers") {
      val objectSchema   = Schema.derived[SimpleUserQueryResolver]
      val pekkoSchemaDoc = DocumentRenderer.renderType(objectSchema.lazyType())
      val resolver       = SimpleUserQueryResolver(
        UserOutput("id", "symphony obj"),
        Future.successful(UserOutput("id", "symphony future"))
      )
      assert(pekkoSchemaDoc == "SimpleUserQueryResolver")
      resolver.user.username shouldEqual "symphony obj"
    }

    it("use simple schema") {
      val resolver            = SourceQueryResolver(args => Source.single(UserOutput("id", "symphony")))
      val graphql: SymphonyQL = SymphonyQL
        .newSymphonyQL()
        .rootResolver(SymphonyQLResolver(resolver -> Schema.derived[SourceQueryResolver]))
        .build()

      graphql.render shouldEqual
        """schema {
          |  query: SourceQueryResolver
          |}
          |
          |type SourceQueryResolver {
          |  getUsers(id: String!): [UserOutput!]
          |}
          |
          |type UserOutput {
          |  id: String!
          |  username: String!
          |}""".stripMargin
    }

    it("pekko Source stage") {
      val resolver = SourceQueryResolver(args => Source.single(UserOutput("id", "symphony")))

      val stage = Schema.derived[SourceQueryResolver].analyze(resolver)

      val getUsersStage = stage
        .asInstanceOf[Stage.ObjectStage]
        .fields
        .get("getUsers")
        .head
        .asInstanceOf[Stage.FunctionStage]
        .stage
        .apply(Map("id" -> SymphonyQLValue.StringValue("")))
      println(getUsersStage)
      getUsersStage.toString.contains("ScalaSourceStage(Source(SourceShape(") shouldEqual true
    }
  }
}
