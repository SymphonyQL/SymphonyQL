package example.schema

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.*
import symphony.derivation.SchemaGen.*
import symphony.derivation.SchemaGen.given
import symphony.derivation.ArgumentExtractorGen.given
import symphony.derivation.SchemaGen
import symphony.parser.*
import symphony.schema.Schema

import scala.concurrent.*
import scala.concurrent.duration.Duration

object ScalaUserMain extends App {

  val graphql: SymphonyQL = SymphonyQL
    .builder()
    .rootResolver(
      SymphonyQLResolver(
        UserQueryResolver(
          args => UserOutput("a1" + args.id, "b1" + args.id),
          args => Source.single(UserOutput("a1" + args.id, "b1" + args.id))
        ) -> SchemaGen.derived[UserQueryResolver]
      )
    )
    .build()

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
      |  batchGetUsers(ids: "10001") {
      |    id
      |    username
      |  }
      |}""".stripMargin

  implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")

  val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(query)))

  val batchGetRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(querySource)))

  println(Await.result(batchGetRes, Duration.Inf))

  actorSystem.terminate()

}
