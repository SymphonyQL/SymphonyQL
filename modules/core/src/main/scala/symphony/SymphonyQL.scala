package symphony

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.*

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*

import symphony.execution.Executor
import symphony.parser.*
import symphony.parser.SymphonyQLError.ExecutionError
import symphony.parser.adt.Definition.TypeSystemDefinition.*
import symphony.parser.adt.Document
import symphony.schema.*

final class SymphonyQL private (rootSchema: SymphonyQLSchema) {

  private lazy val _document: Document = Document(
    SchemaDefinition(
      Nil,
      rootSchema.query.flatMap(_.opType.name),
      rootSchema.mutation.flatMap(_.opType.name),
      rootSchema.subscription.flatMap(_.opType.name),
      None
    ) :: rootSchema.collectTypes.flatMap(_.toTypeDefinition),
    SourceMapper.empty
  )

  def document: Document = _document

  def render: String = DocumentRenderer.render(_document)

  def runWith(
    request: SymphonyQLRequest
  )(implicit actorSystem: ActorSystem): Future[SymphonyQLResponse[SymphonyQLError]] =
    compile(request)
      .map(SymphonyQLResponse(_, List.empty))
      .recoverWith {
        case se: SymphonyQLError => Source.failed(se)
        case ex: Exception       =>
          Source.single(
            SymphonyQLResponse(
              SymphonyQLValue.NullValue,
              List(
                SymphonyQLError.ExecutionError(
                  "Caught error during execution of source field",
                  innerThrowable = Some(ex)
                )
              )
            )
          )
      }
      .runWith[Future[SymphonyQLResponse[SymphonyQLError]]](Sink.head)

  private def compile(
    request: SymphonyQLRequest
  )(implicit actorSystem: ActorSystem): Source[SymphonyQLOutputValue, NotUsed] = {
    val document = SymphonyQLParser.parseQuery(request.query.getOrElse(""))
    document match
      case Left(ex)   => Source.failed(ex)
      case Right(doc) =>
        Executor.execute(
          doc,
          rootSchema,
          request.operationName,
          request.variables.getOrElse(Map.empty)
        )
  }
}

object SymphonyQL {
  def builder(): SymphonyQLBuilder = new SymphonyQLBuilder

  final class SymphonyQLBuilder {
    private var rootSchema = SymphonyQLSchema(None, None, None)

    def rootResolver[Q, M, S](
      rootResolver: SymphonyQLResolver[Q, M, S]
    ): this.type = {
      rootSchema = rootSchema ++ SymphonyQLSchema(
        rootResolver.queryResolver.map(r => Operation(r._2.toType(), r._2.analyze(r._1))),
        rootResolver.mutationResolver.map(r => Operation(r._2.toType(), r._2.analyze(r._1))),
        rootResolver.subscriptionResolver.map(r => Operation(r._2.toType(), r._2.analyze(r._1)))
      )
      this
    }

    def build() = new SymphonyQL(rootSchema)
  }

}
