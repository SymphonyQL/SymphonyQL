package symphony.schema

import org.apache.pekko.stream.scaladsl.*

import symphony.SymphonyContext

sealed trait Resolver {}

trait QueryResolver[M, In, Out] extends Resolver {
  def resolve(ctx: SymphonyContext[In]): Source[Out, M]
}
