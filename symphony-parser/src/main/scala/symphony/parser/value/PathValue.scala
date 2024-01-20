package symphony
package parser
package value

import Value.StringValue

private[symphony] trait PathValue extends Value

object PathValue {
  def fromEither(either: Either[String, Int]): PathValue = either.fold(Key.apply, Index.apply)

  object Key {
    def apply(value: String): PathValue = Value.StringValue(value)

    def unapply(value: PathValue): Option[String] = value match {
      case Value.StringValue(s) => Some(s)
      case _                    => None
    }
  }

  object Index {
    def apply(value: Int): PathValue = Value.IntValue.IntNumber(value)

    def unapply(value: PathValue): Option[Int] = value match {
      case Value.IntValue.IntNumber(i) => Some(i)
      case _                           => None
    }
  }
}
