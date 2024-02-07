package symphony.schema

import symphony.parser.*
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLInputValue
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.schema.scaladsl.ArgExtractorDerivation

import java.util
import scala.jdk.javaapi.CollectionConverters
import java.util.Optional

trait ArgumentExtractor[T] { self =>

  def default(default: Option[String] = None): Either[ArgumentError, T] =
    default
      .map(
        SymphonyQLParser.parseInputValue(_).flatMap(extract).left.map(e => ArgumentError(e.getMessage()))
      )
      .getOrElse(extract(NullValue))

  def extract(input: SymphonyQLInputValue): Either[ArgumentError, T]

  def map[A](f: T => A): ArgumentExtractor[A] = (input: SymphonyQLInputValue) => self.extract(input).map(f)

  def flatMap[A](f: T => Either[ArgumentError, A]): ArgumentExtractor[A] = (input: SymphonyQLInputValue) =>
    self.extract(input).flatMap(f)

}

object ArgumentExtractor extends GenericArgExtractor with ArgExtractorJavaAPI {
  def apply[T](implicit ae: ArgumentExtractor[T]): ArgumentExtractor[T] = ae

}
trait ArgExtractorJavaAPI { self: GenericArgExtractor =>

  /**
   * Java API
   */
  def createOption[A](ae: ArgumentExtractor[A]): ArgumentExtractor[Optional[A]] =
    (input: SymphonyQLInputValue) => ae.extract(input).map(Optional.of)

  /**
   * Java API
   */
  def createList[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.List[A]] =
    (input: SymphonyQLInputValue) => mkList(ae).extract(input).map(a => CollectionConverters.asJava(a))

  /**
   * Java API
   */
  def createSet[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.Set[A]] =
    (input: SymphonyQLInputValue) => mkSet(ae).extract(input).map(a => CollectionConverters.asJava(a))

  /**
   * Java API
   */
  def createVector[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.Vector[A]] =
    (input: SymphonyQLInputValue) =>
      mkVector(ae).extract(input).map(a => new util.Vector[A](CollectionConverters.asJava(a)))
}
trait GenericArgExtractor extends ArgExtractorDerivation {

  implicit lazy val UnitArg: ArgumentExtractor[Unit] = (_: SymphonyQLInputValue) => Right(())

  implicit lazy val IntArg: ArgumentExtractor[Int] = (input: SymphonyQLInputValue) =>
    LongArg.extract(input).map(_.toInt)

  implicit lazy val LongArg: ArgumentExtractor[Long] = {
    case value: IntValue => Right(value.toLong)
    case other           => Left(ArgumentError(s"Cannot build an Long from input $other"))
  }

  implicit lazy val DoubleArg: ArgumentExtractor[Double] = {
    case value: IntValue   => Right(value.toLong.toDouble)
    case value: FloatValue => Right(value.toDouble)
    case other             => Left(ArgumentError(s"Cannot build a Double from input $other"))
  }

  implicit lazy val FloatArg: ArgumentExtractor[Float] = (input: SymphonyQLInputValue) =>
    DoubleArg.extract(input).map(_.toFloat)

  implicit lazy val StringArg: ArgumentExtractor[String] = {
    case StringValue(value) => Right(value)
    case other              => Left(ArgumentError(s"Cannot build a String from input $other"))
  }

  implicit lazy val BooleanArg: ArgumentExtractor[Boolean] = {
    case BooleanValue(value) => Right(value)
    case other               => Left(ArgumentError(s"Cannot build a Boolean from input $other"))
  }

  implicit lazy val BigInt: ArgumentExtractor[BigInt] = {
    case value: IntValue => Right(value.toBigInt)
    case other           => Left(ArgumentError(s"Cannot build a BigInt from input $other"))
  }

  implicit lazy val BigDecimal: ArgumentExtractor[BigDecimal] = {
    case value: IntValue   => Right(scala.math.BigDecimal(value.toBigInt))
    case value: FloatValue => Right(value.toBigDecimal)
    case other             => Left(ArgumentError(s"Cannot build a BigDecimal from input $other"))
  }

  implicit def mkOption[A](implicit ae: ArgumentExtractor[A]): ArgumentExtractor[Option[A]] = {
    case SymphonyQLValue.NullValue => Right(None)
    case value                     => ae.extract(value).map(Some(_))
  }

  implicit def mkList[A](implicit ae: ArgumentExtractor[A]): ArgumentExtractor[List[A]] = {
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

  implicit def mkSeq[A](implicit ae: ArgumentExtractor[A]): ArgumentExtractor[Seq[A]] = new ArgumentExtractor[Seq[A]] {
    private lazy val _list = mkList(ae)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Seq[A]] =
      _list.extract(input).map(_.toSeq)
  }

  implicit def mkSet[A](implicit ae: ArgumentExtractor[A]): ArgumentExtractor[Set[A]] = new ArgumentExtractor[Set[A]] {
    private lazy val _list = mkList(ae)

    override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Set[A]] =
      _list.extract(input).map(_.toSet)
  }

  implicit def mkVector[A](implicit ae: ArgumentExtractor[A]): ArgumentExtractor[Vector[A]] =
    new ArgumentExtractor[Vector[A]] {
      private lazy val _list = mkList(ae)

      override def extract(input: SymphonyQLInputValue): Either[ArgumentError, Vector[A]] =
        _list.extract(input).map(_.toVector)
    }
}
