package symphony.derivation

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.*
import symphony.parser.*
import symphony.parser.introspection.__TypeKind
import symphony.schema.*

import symphony.derivation.SchemaGen.gen
import symphony.derivation.SchemaGen.*
import symphony.derivation.ArgumentExtractorGen.gen
import symphony.derivation.ArgumentExtractorGen.*

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
      val inputSchema = SchemaGen.derived[UserQueryInput]
      hasType(inputSchema.toType(true), "UserQueryInputInput", __TypeKind.INPUT_OBJECT)
    }

    it("derives simple output schema") {
      val outputSchema = SchemaGen.derived[UserOutput]
      hasType(outputSchema.toType(), "UserOutput", __TypeKind.OBJECT)
    }

    it("derives simple func schema") {
      val funSchema = SchemaGen.mkFuncSchema(
        summon[ArgumentExtractor[UserQueryInput]],
        SchemaGen.derived[UserQueryInput],
        SchemaGen.derived[UserOutput]
      )
      hasType(funSchema.toType(), "UserOutput", __TypeKind.OBJECT)
    }

    it("derives simple root schema with simple resolvers") {
      val objectSchema   = SchemaGen.derived[SimpleUserQueryResolver]
      val pekkoSchemaDoc = DocumentRenderer.renderType(objectSchema.toType())
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
        .builder()
        .rootResolver(SymphonyQLResolver(resolver -> SchemaGen.derived[SourceQueryResolver]))
        .build()

      graphql.render shouldEqual """schema {
                                   |  query: SourceQueryResolver
                                   |}
                                   |
                                   |type SourceQueryResolver {
                                   |  getUsers(id: String!): UserOutput
                                   |}
                                   |
                                   |type UserOutput {
                                   |  id: String!
                                   |  username: String!
                                   |}""".stripMargin
    }
  }
}
