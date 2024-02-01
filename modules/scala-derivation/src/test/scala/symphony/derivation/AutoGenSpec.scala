package symphony.derivation

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*

import symphony.*
import symphony.parser.*
import symphony.schema.*
import symphony.schema.ArgumentExtractor.*

class AutoGenSpec extends AnyFunSpec with Matchers {

  // test classes
  final case class UserQueryInput(id: String)

  final case class UserOutput(id: String, username: String)

  final case class UserQueryResolver(
    getUsers: UserQueryInput => Source[UserOutput, NotUsed]
  )

  import symphony.derivation.SchemaGen.auto

  given ArgumentExtractor[UserQueryInput] = ArgumentExtractorGen.gen[UserQueryInput]

  describe("Simple Derivation") {
    it("derives simple input schema") {
      val inputSchema = SchemaGen.gen[UserQueryInput]
      val inputDoc    = DocumentRenderer.renderType(inputSchema.toType(true))
      inputDoc shouldEqual "UserQueryInputInput"
    }

    it("derives simple output schema") {
      val outputSchema = SchemaGen.gen[UserOutput]
      val outputDoc    = DocumentRenderer.renderType(outputSchema.toType())
      outputDoc shouldEqual "UserOutput"
    }

    it("derives simple func schema") {
      val funSchema = Schema.mkFuncSchema(
        summon[ArgumentExtractor[UserQueryInput]],
        summon[Schema[UserQueryInput]],
        summon[Schema[UserQueryResolver]]
      )
      funSchema.arguments.size shouldEqual 1
    }

    it("derives simple root schema") {
      val pekkoSchema    = SchemaGen.gen[UserQueryResolver]
      val pekkoSchemaDoc = DocumentRenderer.renderType(pekkoSchema.toType())
      val resolver       = UserQueryResolver(_ => Source.single(UserOutput("id", "symphony")))

      val stage = pekkoSchema.analyze(resolver)

      println(
        stage
          .asInstanceOf[Stage.ObjectStage]
          .fields("getUsers")
          .asInstanceOf[Stage.FunctionStage]
          .stage
          .apply(Map.empty)
      )

      pekkoSchemaDoc shouldEqual "UserQueryResolver"
    }

    it("use simple schema") {
      val resolver = UserQueryResolver(_ => Source.single(UserOutput("id", "symphony")))

      val graphql: SymphonyQL = SymphonyQL
        .builder()
        .rootResolver(SymphonyQLResolver(resolver -> SchemaGen.gen[UserQueryResolver]))
        .build()

      graphql.render shouldEqual """schema {
                                   |  query: UserQueryResolver
                                   |}
                                   |
                                   |type UserOutput {
                                   |  id: String!
                                   |  username: String!
                                   |}
                                   |
                                   |type UserQueryResolver {
                                   |  getUsers(id: String!): UserOutput!
                                   |}""".stripMargin
    }
  }
}
