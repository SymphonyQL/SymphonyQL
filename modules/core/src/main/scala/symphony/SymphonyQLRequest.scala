package symphony

import symphony.parser.SymphonyQLInputValue
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import java.util.Optional

final case class SymphonyQLRequest(
  query: String,
  operationName: Option[String] = None,
  variables: Option[Map[String, SymphonyQLInputValue]] = None,
  extensions: Option[Map[String, SymphonyQLInputValue]] = None
)
object SymphonyQLRequest {

  def newRequest() = new Builder

  final class Builder {
    private var query: String                                         = _
    private var operationName: Option[String]                         = None
    private var variables: Option[Map[String, SymphonyQLInputValue]]  = None
    private var extensions: Option[Map[String, SymphonyQLInputValue]] = None
    def query(query: String): this.type                               =
      this.query = query
      this

    def operationName(operationName: Optional[String]): this.type =
      this.operationName = operationName.toScala
      this

    def variables(variables: Optional[java.util.Map[String, SymphonyQLInputValue]]): this.type =
      this.variables = variables.toScala.map(_.asScala.toMap)
      this

    def extensions(extensions: Optional[java.util.Map[String, SymphonyQLInputValue]]): this.type =
      this.extensions = extensions.toScala.map(_.asScala.toMap)
      this

    def build(): SymphonyQLRequest = new SymphonyQLRequest(query, operationName, variables, extensions)
  }
}
