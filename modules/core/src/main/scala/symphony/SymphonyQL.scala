package symphony

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.*

import symphony.SymphonyQL.RootSchema
import symphony.parser.*
import symphony.parser.SymphonyError.ExecutionError
import symphony.parser.adt.Definition.TypeSystemDefinition.*
import symphony.parser.adt.Document
import symphony.schema.*
import symphony.schema.ExecutionPlan.ObjectDataPlan

final class SymphonyQL private (rootSchema: RootSchema) {

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

  def execute(request: SymphonyRequest): Future[SymphonyResponse[SymphonyError]] = {
    val document = Future(Parser.parseQuery(request.query.getOrElse("")))
    document.flatMap {
      case Left(value) => Future.successful(Left(value))
      case Right(value) =>
        Executor.executeRequest(value, rootSchema, request.operationName, request.variables.getOrElse(Map.empty))
    }.map {
      case Left(value)  => SymphonyResponse(Value.NullValue, List(value))
      case Right(value) => SymphonyResponse(value, List.empty)
    }
  }

}

object SymphonyQL {

  final case class RootSchema(
    query: Option[Operation] = None,
    mutation: Option[Operation] = None
  )

  def builder(): SymphonyQLBuilder = new SymphonyQLBuilder

  final class SymphonyQLBuilder {
    private var queryOperation: Option[Operation]    = None
    private var mutationOperation: Option[Operation] = None

    private def mergePlans(plan1: ExecutionPlan, plan2: ExecutionPlan): ExecutionPlan =
      (plan1, plan2) match {
        case (ObjectDataPlan(name, fields1), ObjectDataPlan(_, fields2)) =>
          val r = fields1 ++ fields2
          ObjectDataPlan(name, r)
        case (ObjectDataPlan(_, _), _) => plan1
        case _                         => plan2
      }

    def addQuery[T](query: Schema[T], _type: T): this.type = {
      this.queryOperation = Some(
        Operation(
          this.queryOperation.fold(query.toType)(op => query.toType ++ op.opType),
          this.queryOperation.fold(query.resolve(_type))(op => mergePlans(op.executionPlan, query.resolve(_type)))
        )
      )
      this
    }

    def addMutation[T](mutation: Schema[T], _type: T): this.type = {
      this.mutationOperation = Some(
        Operation(
          this.mutationOperation.fold(mutation.toType)(op => mutation.toType ++ op.opType),
          this.mutationOperation.fold(mutation.resolve(_type))(op =>
            mergePlans(op.executionPlan, mutation.resolve(_type))
          )
        )
      )
      this
    }

    def build() = new SymphonyQL(RootSchema(queryOperation, mutationOperation))
  }

}
