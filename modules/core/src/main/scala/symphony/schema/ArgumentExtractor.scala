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

  lazy val unit: ArgumentExtractor[Unit] = (_: SymphonyQLInputValue) => Right(())

  lazy val int: ArgumentExtractor[Int] = (input: SymphonyQLInputValue) => long.extract(input).map(_.toInt)

  lazy val long: ArgumentExtractor[Long] = {
    case value: IntValue => Right(value.toLong)
    case other           => Left(ArgumentError(s"Cannot build an Long from input $other"))
  }

  lazy val double: ArgumentExtractor[Double] = {
    case value: IntValue   => Right(value.toLong.toDouble)
    case value: FloatValue => Right(value.toDouble)
    case other             => Left(ArgumentError(s"Cannot build a Double from input $other"))
  }

  lazy val float: ArgumentExtractor[Float] = (input: SymphonyQLInputValue) => double.extract(input).map(_.toFloat)

  lazy val string: ArgumentExtractor[String] = {
    case StringValue(value) => Right(value)
    case other              => Left(ArgumentError(s"Cannot build a String from input $other"))
  }

  lazy val boolean: ArgumentExtractor[Boolean] = {
    case BooleanValue(value) => Right(value)
    case other               => Left(ArgumentError(s"Cannot build a Boolean from input $other"))
  }

  def option[A](ae: ArgumentExtractor[A]): ArgumentExtractor[Option[A]] = {
    case SymphonyQLValue.NullValue => Right(None)
    case value                     => ae.extract(value).map(Some(_))
  }

  def list[A](ae: ArgumentExtractor[A]): ArgumentExtractor[List[A]] = {
    case SymphonyQLInputValue.ListValue(values) =>
      values
        .foldLeft[Either[ArgumentError, List[A]]](Right(Nil)) {
          case (res @ Left(_), _)  => res
          case (Right(res), value) =>
            ae.extract(value) match {
              case Left(error)  => Left(error)
              case Right(value) => Right(value :: res)
            }
        }
        .map(_.reverse)
    case other                                  => ae.extract(other).map(List(_))
  }

  def seq[A](ae: ArgumentExtractor[A]): ArgumentExtractor[Seq[A]] = new ArgumentExtractor[Seq[A]] {
    private lazy val _list = list(ae)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Seq[A]] =
      _list.extract(input).map(_.toSeq)
  }

  def set[A](ae: ArgumentExtractor[A]): ArgumentExtractor[Set[A]] = new ArgumentExtractor[Set[A]] {
    private lazy val _list = list(ae)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Set[A]] =
      _list.extract(input).map(_.toSet)
  }

  def vector[A](ae: ArgumentExtractor[A]): ArgumentExtractor[Vector[A]] =
    new ArgumentExtractor[Vector[A]] {
      private lazy val _list = list(ae)

      override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Vector[A]] =
        _list.extract(input).map(_.toVector)
    }
}
