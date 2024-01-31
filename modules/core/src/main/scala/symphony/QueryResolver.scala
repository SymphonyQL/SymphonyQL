package symphony

import org.apache.pekko.stream.scaladsl.Source

import symphony.parser.InputValue

trait QueryResolver[-A, +T, +M] {
  def resolve(ctx: SymphonyContext, params: A): Source[T, M]
}
