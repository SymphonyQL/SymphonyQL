package example.schema

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
          args => Source.apply(args.map(a => UserOutput("a1" + a.id, "b1" + a.id)))
        ) -> querySchema
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

  val batchQuery =
    """{
      |  batchGetUsers(value: [{id:"1001"},{id:"1002"}]) {
      |    id
      |    username
      |  }
      |}""".stripMargin

  implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")

  val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(query)))

  val batchGetRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(batchQuery)))

  println(Await.result(getRes, Duration.Inf))
  println(Await.result(batchGetRes, Duration.Inf))

  actorSystem.terminate()

}
