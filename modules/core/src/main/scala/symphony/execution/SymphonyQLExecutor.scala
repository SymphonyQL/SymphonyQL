package symphony.execution

import scala.concurrent.Future

import symphony.SymphonyQLSchema
import symphony.parser.*
import symphony.parser.SymphonyQLError.ExecutionError
import symphony.parser.adt.Document

object SymphonyQLExecutor {

  def executeRequest(
    document: Document,
    schema: SymphonyQLSchema,
    operationName: Option[String] = None,
    variables: Map[String, SymphonyQLInputValue] = Map()
  ): Future[Either[ExecutionError, SymphonyQLOutputValue]] = ???

}
