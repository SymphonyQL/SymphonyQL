package symphony.schema

import symphony.parser.*
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLInputValue
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*

trait ArgumentExtractor[T] { self =>

  def extract(input: SymphonyQLInputValue): Either[ArgumentError, T]

  def map[A](f: T => A): ArgumentExtractor[A] = (input: SymphonyQLInputValue) => self.extract(input).map(f)

  def flatMap[A](f: T => Either[ArgumentError, A]): ArgumentExtractor[A] = (input: SymphonyQLInputValue) =>
    self.extract(input).flatMap(f)

}

object ArgumentExtractor {

  case object UnitArgumentExtractor extends ArgumentExtractor[Unit] {
    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Unit] = Right(())
  }

  case object IntArgumentExtractor extends ArgumentExtractor[Int] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Int] =
      LongArgumentExtractor.extract(input).map(_.toInt)
  }

  case object LongArgumentExtractor extends ArgumentExtractor[Long] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Long] =
      input match
        case value: IntValue => Right(value.toLong)
        case other           => Left(ArgumentError(s"Can't build an Long from input $other"))
  }

  case object DoubleArgumentExtractor extends ArgumentExtractor[Double] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Double] =
      input match
        case value: IntValue   => Right(value.toLong.toDouble)
        case value: FloatValue => Right(value.toDouble)
        case other             => Left(ArgumentError(s"Can't build a Double from input $other"))
  }

  case object FloatArgumentExtractor extends ArgumentExtractor[Float] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Float] =
      DoubleArgumentExtractor.extract(input).map(_.toFloat)
  }

  case object StringArgumentExtractor extends ArgumentExtractor[String] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, String] =
      input match
        case StringValue(value) => Right(value)
        case other              => Left(ArgumentError(s"Can't build a String from input $other"))
  }

  case object BooleanArgumentExtractor extends ArgumentExtractor[Boolean] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Boolean] =
      input match
        case BooleanValue(value) => Right(value)
        case other               => Left(ArgumentError(s"Can't build a Boolean from input $other"))
  }

  final case class OptionArgumentExtractor[A](ev: ArgumentExtractor[A]) extends ArgumentExtractor[Option[A]] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Option[A]] =
      input match
        case SymphonyQLValue.NullValue => Right(None)
        case value                     => ev.extract(value).map(Some(_))
  }

  final case class ListArgumentExtractor[A](ev: ArgumentExtractor[A]) extends ArgumentExtractor[List[A]] {

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, List[A]] = {
      input match
        case SymphonyQLInputValue.ListValue(items) =>
          items
            .foldLeft[Either[ArgumentError, List[A]]](Right(Nil)) {
              case (res @ Left(_), _) => res
              case (Right(res), value) =>
                ev.extract(value) match {
                  case Left(error)  => Left(error)
                  case Right(value) => Right(value :: res)
                }
            }
            .map(_.reverse)
        case other => ev.extract(other).map(List(_))
    }
  }

  final case class SeqArgumentExtractor[A](ev: ArgumentExtractor[A]) extends ArgumentExtractor[Seq[A]] {
    private lazy val list = ListArgumentExtractor(ev)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Seq[A]] = {
      list.extract(input).map(_.toSeq)
    }
  }

  final case class SetArgumentExtractor[A](ev: ArgumentExtractor[A]) extends ArgumentExtractor[Set[A]] {
    private lazy val list = ListArgumentExtractor(ev)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Set[A]] = {
      list.extract(input).map(_.toSet)
    }
  }

  final case class VectorArgumentExtractor[A](ev: ArgumentExtractor[A]) extends ArgumentExtractor[Vector[A]] {
    private lazy val list = ListArgumentExtractor(ev)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Vector[A]] = {
      list.extract(input).map(_.toVector)
    }
  }
}
