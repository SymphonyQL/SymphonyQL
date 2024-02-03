package example.schema

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.*
import symphony.derivation.SchemaDerivation.*
import symphony.derivation.SchemaDerivation.given
import symphony.derivation.ArgumentExtractorDerivation.given
import symphony.derivation.SchemaDerivation
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
        ) -> SchemaDerivation.derived[UserQueryResolver]
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
      |  batchGetUsers(id: "10001") {
      |    id
      |    username
      |  }
      |}""".stripMargin

  implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")

  val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(query)))

  val batchGetRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(querySource)))

  // 有问题，会block 
  println(Await.result(batchGetRes, Duration.Inf))

  actorSystem.terminate()

}
