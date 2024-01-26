package symphony

import scala.concurrent.Future

import symphony.SymphonyQL.RootSchema
import symphony.parser.SymphonyError.ExecutionError
import symphony.parser.adt.Document
import symphony.parser.value.*

object Executor {

  def executeRequest(
    document: Document,
    schema: RootSchema,
    operationName: Option[String] = None,
    variables: Map[String, InputValue] = Map()
  ): Future[Either[ExecutionError, OutputValue]] = ???

}
