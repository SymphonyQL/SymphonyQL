package symphony.server

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.marshalling.*
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.unmarshalling.*
import org.apache.pekko.http.scaladsl.server.*
import spray.json.*
import symphony.*
import symphony.parser.*

trait DefaultRoute(symphonyQL: SymphonyQL) extends JsonFormats with SprayJsonSupport {

  implicit val actorSystem: ActorSystem

  def defaultExceptionHandler: ExceptionHandler =
    ExceptionHandler { case e: SymphonyQLError =>
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = HttpEntity(ContentTypes.`application/json`, e.toJson.toString)
        )
      )
    }

  final implicit val symphonyQLRequestUnMarshaller: Unmarshaller[HttpEntity, SymphonyQLRequest] =
    sprayJsValueUnmarshaller.map(json => symphonyQLRequestJsonFormat.read(json))

  final implicit val symphonyQLResponseMarshaller: ToResponseMarshaller[SymphonyQLResponse[SymphonyQLError]] =
    Marshaller.withFixedContentType[SymphonyQLResponse[SymphonyQLError], HttpResponse](ContentTypes.`application/json`)(
      a =>
        HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString))
    )

  val routes: Route = path("api" / "graphql") {
    handleExceptions(defaultExceptionHandler) {
      post {
        entity(as[SymphonyQLRequest]) { symphonyQLRequest =>
          complete {
            symphonyQL.runWith(symphonyQLRequest)
          }
        }
      }
    }
  }
}
