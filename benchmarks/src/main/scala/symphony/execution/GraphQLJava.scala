package symphony.execution

import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.*
import graphql.schema.idl.{
  EnumValuesProvider,
  NaturalEnumValuesProvider,
  RuntimeWiring,
  SchemaGenerator,
  SchemaParser,
  TypeDefinitionRegistry,
  TypeRuntimeWiring
}
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring

import scala.jdk.CollectionConverters.*
import java.util

object GraphQLJava {
  val schemaParser           = new SchemaParser
  val typeDefinitionRegistry = schemaParser.parse("""schema {
                                                    |  query: Queries
                                                    |}
                                                    |
                                                    |enum Origin {
                                                    |  EARTH, MARS, BELT
                                                    |}
                                                    |
                                                    |type Character {
                                                    |  name: String!
                                                    |  origin: Origin!
                                                    |}
                                                    |
                                                    |type Queries {
                                                    |    characters(origin: Origin): [Character!]
                                                    |}""".stripMargin)

  private val runtimeWiring =
    newRuntimeWiring
      .`type`(
        "Queries",
        (builder: TypeRuntimeWiring.Builder) =>
          builder
            .dataFetcher(
              "characters",
              new DataFetcher[java.util.List[Data.Character]]() {
                override def get(environment: DataFetchingEnvironment): java.util.List[Data.Character] =
                  Data.characters.asJava
              }
            )
            .enumValues((name: String) => Data.Origin.valueOf(name))
      )
      .build

  private val schemaGenerator = new SchemaGenerator
  private val graphQLSchema   = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
  val build: GraphQL          = GraphQL.newGraphQL(graphQLSchema).build
//
//  val simpleQuery: String =
//    """{
//          characters {
//            name
//            origin
//          }
//       }""".stripMargin
//  val executionResult     = GraphQLJava.build.execute(simpleQuery)
//  println(executionResult.getErrors)
//  println(executionResult.getData.toString)

}
