package symphony.example

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.*
import org.apache.pekko.stream.scaladsl.*
import symphony.parser.*
import symphony.*
import symphony.example.schema.Users.*
import symphony.example.schema.queriesSchema
import scala.concurrent.*
import scala.concurrent.duration.Duration

object ScalaAPIMain {

  val graphql: SymphonyQL = SymphonyQL
    .newSymphonyQL()
    .addQuery(
      Queries(args =>
        Source.single(
          Character("hello-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT))
        )
      ),
      queriesSchema
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

    val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(characters))

    println(Await.result(getRes, Duration.Inf).toOutputValue)
    actorSystem.terminate()
  }

}
