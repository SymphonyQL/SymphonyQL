package symphony

import symphony.schema.Schema

final case class SymphonyQLResolver[Q, M, S](
  queryResolver: Option[(Q, Schema[Q])],
  mutationResolver: Option[(M, Schema[M])],
  subscriptionResolver: Option[(S, Schema[S])] = None
)

object SymphonyQLResolver {

  def apply[Q](queryResolver: (Q, Schema[Q])) =
    new SymphonyQLResolver(
      Some(queryResolver),
      Option.empty[(Unit, Schema[Unit])],
      Option.empty[(Unit, Schema[Unit])]
    )

  def apply[Q, M](
    queryResolver: (Q, Schema[Q]),
    mutationResolver: (M, Schema[M])
  ): SymphonyQLResolver[Q, M, Unit] =
    new SymphonyQLResolver(
      Some(queryResolver),
      Some(mutationResolver),
      Option.empty[(Unit, Schema[Unit])]
    )

  def apply[Q, M, S](
    queryResolver: (Q, Schema[Q]),
    mutationResolver: (M, Schema[M]),
    subscriptionResolver: (S, Schema[S])
  ): SymphonyQLResolver[Q, M, S] =
    new SymphonyQLResolver(
      Some(queryResolver),
      Some(mutationResolver),
      Some(subscriptionResolver)
    )
}
