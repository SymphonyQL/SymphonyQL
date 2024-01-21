package symphony

final case class SymphonyContext[A](param: A, env: String = "dev")
