package symphony.example

import org.apache.pekko.actor
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.*
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.stream.SystemMaterializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import symphony.SymphonyQL
import symphony.example.schema.Users.*
import symphony.server.DefaultRoute
import org.apache.pekko.actor.ActorSystem as ClassicActorSystem

import scala.concurrent.ExecutionContextExecutor

object SimpleHttpServer
    extends DefaultRoute(
      SymphonyQL
        .newSymphonyQL()
        .query(
          Queries(args =>
            Source.failed(
              Character("hello-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT))
            )
          )
        )
        .build()
    ) {

  override implicit val actorSystem: ClassicActorSystem =
    ActorSystem[Nothing](serverBehavior, "SimpleHttpServer").classicSystem

  def serverBehavior: Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    implicit val system                                     = context.system
    implicit val executionContext: ExecutionContextExecutor = context.executionContext

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)
    context.log.info("Server online at http://localhost:8080/")

    Behaviors.receiveMessage { _ =>
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
      Behaviors.stopped
    }
  }

  def main(args: Array[String]): Unit =
    serverBehavior
}
