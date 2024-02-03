package example.schema

import example.schema.ScalaUserMain.{graphql, querySource}

import scala.concurrent.*
import scala.concurrent.duration.Duration
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.*
import symphony.derivation.SchemaGen
import symphony.parser.*
import symphony.parser.SymphonyQLError
import symphony.schema.Schema

object UserMain extends App {

  val graphql: SymphonyQL = SymphonyQL
    .builder()
    .rootResolver(
      SymphonyQLResolver(
        UserQueryResolver(
          args => UserOutput("a1" + args.id, "b1" + args.id),
          args => Source.single(UserOutput("a1" + args.id, "b1" + args.id))
        ) -> querySchema
      )
    )
    .build()

  /**
   * *
   * {{{
   *   schema {
   *    query: UserQueryResolver
   *  }
   *
   *  type UserOutput {
   *    id: String!
   *    username: String
   *  }
   *
   *  type UserQueryResolver {
   *    getUsers(id: String!): UserOutput!
   *  }
   * }}}
   */
  println(graphql.render)

  val query =
    """{
      |  getUsers(id: "1000") {
      |    id
      |    username
      |  }
      |}""".stripMargin

  val querySource =
    """{
      |  batchGetUsers(id: "10001") {
      |    id
      |    username
      |  }
      |}""".stripMargin
    
  implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")

  val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(query)))
  val batchGetRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(querySource)))

  println(Await.result(getRes, Duration.Inf))
  println(Await.result(batchGetRes, Duration.Inf))

  actorSystem.terminate()

}
