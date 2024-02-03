package symphony.derivation

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.*
import symphony.parser.*
import symphony.parser.introspection.__TypeKind
import symphony.schema.*
import symphony.schema.ArgumentExtractor.*

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

  import symphony.derivation.SchemaGen.auto

  given ArgumentExtractor[UserQueryInput] = ArgumentExtractorGen.gen[UserQueryInput]

  describe("Simple Derivation") {
    it("derives simple input schema") {
      val inputSchema = SchemaGen.gen[UserQueryInput]
      hasType(inputSchema.toType(true), "UserQueryInputInput", __TypeKind.INPUT_OBJECT)
    }

    it("derives simple output schema") {
      val outputSchema = SchemaGen.gen[UserOutput]
      hasType(outputSchema.toType(), "UserOutput", __TypeKind.OBJECT)
    }

    it("derives simple func schema") {
      val funSchema = Schema.mkFuncSchema(
        summon[ArgumentExtractor[UserQueryInput]],
        summon[Schema[UserQueryInput]],
        summon[Schema[SourceQueryResolver]]
      )
      hasType(funSchema.toType(), "String", __TypeKind.SCALAR)
    }

    it("derives simple root schema with simple resolvers") {
      val objectSchema   = SchemaGen.gen[SimpleUserQueryResolver]
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
        .rootResolver(SymphonyQLResolver(resolver -> SchemaGen.gen[SourceQueryResolver]))
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
