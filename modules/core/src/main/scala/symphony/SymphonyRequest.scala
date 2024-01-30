package symphony

import symphony.parser.InputValue

final case class SymphonyRequest(
  query: Option[String] = None,
  operationName: Option[String] = None,
  variables: Option[Map[String, InputValue]] = None,
  extensions: Option[Map[String, InputValue]] = None
)
