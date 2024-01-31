package symphony

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.*

import symphony.execution.SymphonyQLExecutor
import symphony.parser.*
import symphony.parser.SymphonyQLError.ExecutionError
import symphony.parser.adt.Definition.TypeSystemDefinition.*
import symphony.parser.adt.Document
import symphony.schema.*

final class SymphonyQL private (rootSchema: SymphonyQLSchema) {

  private val allTypes = (
    rootSchema.query.map(op => Types.collectTypes(op.opType)).toList.flatten ++ rootSchema.mutation
      .map(op => Types.collectTypes(op.opType))
      .toList
      .flatten
  )
    .groupBy(t => (t.name, t.kind, t.origin))
    .flatMap(_._2.headOption)
    .toList

  val document: Document = Document(
    SchemaDefinition(
      Nil,
      rootSchema.query.flatMap(_.opType.name),
      rootSchema.mutation.flatMap(_.opType.name),
      None,
      None
    ) :: allTypes.flatMap(_.toTypeDefinition),
    SourceMapper.empty
  )

  def render: String = DocumentRenderer.render(document)

  def execute(request: SymphonyQLRequest): Future[SymphonyQLResponse[SymphonyQLError]] = {
    val document = Future(SymphonyQLParser.parseQuery(request.query.getOrElse("")))
    document.flatMap {
      case Left(value) => Future.successful(Left(value))
      case Right(value) =>
        SymphonyQLExecutor.executeRequest(
          value,
          rootSchema,
          request.operationName,
          request.variables.getOrElse(Map.empty)
        )
    }.map {
      case Left(value)  => SymphonyQLResponse(SymphonyQLValue.NullValue, List(value))
      case Right(value) => SymphonyQLResponse(value, List.empty)
    }
  }

}

object SymphonyQL {
  def builder(): SymphonyQLBuilder = new SymphonyQLBuilder

  final class SymphonyQLBuilder {
    private var rootSchema: Option[SymphonyQLSchema] = None

    def rootResolver[Q, M, S](
      rootResolver: SymphonyQLResolver[Q, M, S]
    ): this.type = {
      rootSchema = rootSchema.map(
        _ ++ SymphonyQLSchema(
          rootResolver.queryResolver.map(r => Operation(r._2.toType, r._2.resolve(r._1))),
          rootResolver.mutationResolver.map(r => Operation(r._2.toType, r._2.resolve(r._1))),
          rootResolver.subscriptionResolver.map(r => Operation(r._2.toType, r._2.resolve(r._1)))
        )
      )
      this
    }

    def build() = new SymphonyQL(rootSchema.getOrElse(SymphonyQLSchema(None, None, None)))
  }

}
