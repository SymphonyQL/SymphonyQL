package example.schema

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.*
import symphony.parser.*

import scala.concurrent.*
import scala.concurrent.duration.Duration
import UserAPI.*

object JavaUserMain {

  val graphql: SymphonyQL = SymphonyQL
    .newSymphonyQL()
    .rootResolver(
      SymphonyQLResolver(
        Queries(args =>
          Source.single(
            Character("abc-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT))
          )
        ) -> queriesSchema
      )
    )
    .build()

  /**
   *  schema {
   *    query: Queries
   *  }
   *
   *  enum Origin {
   *    EARTH
   *    MARS
   *    BELT
   *  }
   *
   *  type Character {
   *    name: String!
   *    origin: Origin!
   *  }
   *
   *  type Queries {
   *    characters(name: Origin): [Character!]
   *   }
   *  }}}
   */
  println(graphql.render)

  val characters =
    """{
      |  characters(origin: "MARS") {
      |    name
      |    origin
      |  }
      |}""".stripMargin

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")

    val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(characters)))

    println(Await.result(getRes, Duration.Inf).toOutputValue)
    actorSystem.terminate()
  }

}
