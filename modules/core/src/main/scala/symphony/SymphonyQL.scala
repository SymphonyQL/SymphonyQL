package symphony

import scala.concurrent.Future

import symphony.parser.*

final class SymphonyQL private {

  // TODO add RootSchema and RootSchemaBuilder to create graphql schema

  def render: String = ???

  def check(query: String): Future[Either[SymphonyError, Unit]] = ???

  def execute(request: SymphonyRequest): Future[SymphonyResponse[SymphonyError]] = ???
}

object SymphonyQL {

  def builder(): SymphonyQLBuilder = new SymphonyQLBuilder

  final class SymphonyQLBuilder {
    def build() = new SymphonyQL()
  }

}
