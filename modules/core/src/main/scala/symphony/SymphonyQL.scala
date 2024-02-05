package symphony

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.*
import symphony.execution.*
import symphony.parser.*
import symphony.parser.adt.Definition.TypeSystemDefinition.*
import symphony.parser.adt.Document
import symphony.schema.*

import scala.jdk.FutureConverters.*
import java.util.concurrent.CompletionStage
import scala.concurrent.*
import scala.util.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.OperationType

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

  def run(request: SymphonyQLRequest, actorSystem: ActorSystem): CompletionStage[SymphonyQLResponse[SymphonyQLError]] =
    runWith(request)(actorSystem).asJava

  def runWith(
    request: SymphonyQLRequest
  )(implicit
    actorSystem: ActorSystem,
  ): Future[SymphonyQLResponse[SymphonyQLError]] =
    import actorSystem.dispatcher
    for {
      doc  <- Future(SymphonyQLParser.parseQuery(request.query.getOrElse("")))
      resp <- doc match
                case Left(ex)        => Future.failed(ex)
                case Right(document) =>
                  compileRequest(document, request)
                    .map(SymphonyQLResponse(_, List.empty))
                    .runWith[Future[SymphonyQLResponse[SymphonyQLError]]](Sink.head)
    } yield resp

  private def getOperation(
    operationName: Option[String] = None,
    document: Document
  ): Either[SymphonyQLError, (OperationDefinition, Operation)] = {
    val op = operationName match {
      case Some(name) =>
        document.definitions.collectFirst { case op: OperationDefinition if op.name.contains(name) => op }
          .toRight(SymphonyQLError.ArgumentError(s"Unknown operation $name."))
      case None       =>
        document.definitions.collect { case op: OperationDefinition => op } match {
          case head :: Nil => Right(head)
          case _           => Left(SymphonyQLError.ArgumentError("Operation name is required."))
        }
    }

    val operation = op match {
      case Left(error)                => Left(error)
      case Right(operationDefinition) =>
        operationDefinition.operationType match {
          case OperationType.Query        =>
            rootSchema.query.toRight(SymphonyQLError.ExecutionError("Queries are not supported on this schema"))
          case OperationType.Mutation     =>
            rootSchema.mutation match {
              case Some(m) => Right(m)
              case None    => Left(SymphonyQLError.ExecutionError("Mutations are not supported on this schema"))
            }
          case OperationType.Subscription =>
            rootSchema.subscription match {
              case Some(m) => Right(m)
              case None    => Left(SymphonyQLError.ExecutionError("Subscriptions are not supported on this schema"))
            }
        }
    }

    op.flatMap(d => operation.map(o => d -> o))
  }

  private def compileRequest(doc: Document, request: SymphonyQLRequest)(implicit
    actorSystem: ActorSystem,
    ec: ExecutionContext
  ): Source[SymphonyQLOutputValue, NotUsed] = {
    val fragments = doc.definitions.collect { case fragment: FragmentDefinition =>
      fragment.name -> fragment
    }.toMap
    getOperation(request.operationName, doc) match
      case Left(ex)            => Source.failed(ex)
      case Right((define, op)) =>
        Executor.executeRequest(
          ExecutionRequest(
            op.stage,
            define.selectionSet,
            fragments,
            define.variableDefinitions,
            request.variables.getOrElse(Map.empty),
            define.operationType
          )
        )
  }
}

object SymphonyQL {

  def newSymphonyQL(): SymphonyQLBuilder = new SymphonyQLBuilder

  private def mergeOperation[R](op: Option[Operation], resolver: R, schema: Schema[R]): Option[Operation] =
    Some(
      op.fold(Operation(schema.lazyTpe(), schema.analyze(resolver)))(s =>
        s ++ Operation(schema.lazyTpe(), schema.analyze(resolver))
      )
    )

  final class SymphonyQLBuilder {
    private var rootSchema                      = SymphonyQLSchema(None, None, None)
    private var query: Option[Operation]        = None
    private var mutation: Option[Operation]     = None
    private var subscription: Option[Operation] = None

    def addQuery[Q](query: Q, schema: Schema[Q]): this.type = {
      this.query = mergeOperation(this.query, query, schema)
      this
    }

    def addMutation[M](mutation: M, schema: Schema[M]): this.type = {
      this.mutation = mergeOperation(this.mutation, mutation, schema)
      this
    }

    def addSubscription[S](subscription: S, schema: Schema[S]): this.type = {
      this.subscription = mergeOperation(this.subscription, subscription, schema)
      this
    }

    def rootResolver[Q, M, S](
      rootResolver: SymphonyQLResolver[Q, M, S]
    ): this.type = {
      rootSchema = rootSchema ++ SymphonyQLSchema(
        rootResolver.queryResolver.map(r => Operation(r._2.lazyTpe(), r._2.analyze(r._1))),
        rootResolver.mutationResolver.map(r => Operation(r._2.lazyTpe(), r._2.analyze(r._1))),
        rootResolver.subscriptionResolver.map(r => Operation(r._2.lazyTpe(), r._2.analyze(r._1)))
      )
      this
    }

    def build(): SymphonyQL = {
      val allSchemas = SymphonyQLSchema(query, mutation, subscription) ++ rootSchema
      new SymphonyQL(allSchemas)
    }
  }

}
