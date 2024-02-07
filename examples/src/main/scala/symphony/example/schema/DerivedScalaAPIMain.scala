package symphony.example.schema

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.*
import symphony.parser.*
import symphony.schema.*
import Users.*

import scala.concurrent.*
import scala.concurrent.duration.Duration

object DerivedScalaAPIMain {

  val graphql: SymphonyQL = SymphonyQL
    .newSymphonyQL()
    .addQuery(
      Queries(args =>
        Source.single(
          Character("abc-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT))
        )
      ),
      Schema.derived[Queries]
    )
    .build()

  println(graphql.render)

  val characters =
    """{
      |  characters(origin: "MARS") {
      |    name
      |    origin
      |  }
      |}""".stripMargin

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem                   = ActorSystem("symphonyActorSystem")
    val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(characters)))
    println(Await.result(getRes, Duration.Inf).toOutputValue)
    actorSystem.terminate()
  }

}
