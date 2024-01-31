package symphony

import symphony.parser.SymphonyQLInputValue

final case class SymphonyQLRequest(
  query: Option[String] = None,
  operationName: Option[String] = None,
  variables: Option[Map[String, SymphonyQLInputValue]] = None,
  extensions: Option[Map[String, SymphonyQLInputValue]] = None
)
