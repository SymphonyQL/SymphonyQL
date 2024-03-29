package symphony.schema

import symphony.parser.*
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLInputValue
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.schema.derivation.ArgExtractorDerivation

import java.util.Optional
import scala.annotation.unused
import scala.jdk.javaapi.CollectionConverters
import scala.jdk.javaapi.OptionConverters

trait ArgumentExtractor[T] { self =>

  /**
   * Java API
   * This method is only available for object types.
   */
  def defaultValue(default: Optional[String]): Either[ArgumentError, T] =
    defaultValue(OptionConverters.toScala(default))

  def defaultValue(default: Option[String] = None): Either[ArgumentError, T] =
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

object ArgumentExtractor extends GenericArgExtractor with ArgExtractorFactory {
  def apply[T](implicit ae: ArgumentExtractor[T]): ArgumentExtractor[T] = ae

}
trait ArgExtractorFactory { self: GenericArgExtractor =>

  /**
   * Java API
   */
  @unused
  def createOptional[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.Optional[A]] =
    (input: SymphonyQLInputValue) => ae.extract(input).map(java.util.Optional.of)

  /**
   * Java API
   */
  @unused
  def createList[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.List[A]] =
    (input: SymphonyQLInputValue) => mkList(ae).extract(input).map(a => CollectionConverters.asJava(a))

  /**
   * Java API
   */
  @unused
  def createSet[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.Set[A]] =
    (input: SymphonyQLInputValue) => mkSet(ae).extract(input).map(a => CollectionConverters.asJava(a))

  /**
   * Java API
   */
  @unused
  def createVector[A](ae: ArgumentExtractor[A]): ArgumentExtractor[java.util.Vector[A]] =
    (input: SymphonyQLInputValue) =>
      mkVector(ae).extract(input).map(a => new java.util.Vector[A](CollectionConverters.asJava(a)))

  /**
   * Using in APT
   */
  @unused
  def getArgument(arg: SymphonyQLInputValue): Any =
    arg match
      case SymphonyQLValue.NullValue => null
      case value: IntValue           =>
        value match
          case IntValue.IntNumber(value)    => value
          case IntValue.LongNumber(value)   => value
          case IntValue.BigIntNumber(value) => value
      case value: FloatValue         =>
        value match
          case FloatValue.FloatNumber(value)      => value
          case FloatValue.DoubleNumber(value)     => value
          case FloatValue.BigDecimalNumber(value) => value
      case StringValue(value)        => value
      case BooleanValue(value)       => value
      case EnumValue(value)          => value
      case _                         => throw new IllegalArgumentException(s"Method 'ArgumentExtractor.getArgument' is not support for $arg")

  /**
   * Using in APT
   */
  @unused
  def getArgumentExtractor(typeName: String): ArgumentExtractor[?] =
    typeName match
      case "java.lang.Boolean"    => ArgumentExtractor.BooleanArg
      case "java.lang.String"     => ArgumentExtractor.StringArg
      case "java.lang.Integer"    => ArgumentExtractor.IntArg
      case "java.lang.Long"       => ArgumentExtractor.LongArg
      case "java.lang.Double"     => ArgumentExtractor.DoubleArg
      case "java.lang.Float"      => ArgumentExtractor.FloatArg
      case "java.lang.Short"      => ArgumentExtractor.ShortArg
      case "java.math.BigInteger" => ArgumentExtractor.BigIntegerArg
      case "java.math.BigDecimal" => ArgumentExtractor.BigDecimalArg
      case "java.lang.Void"       => ArgumentExtractor.UnitArg
      case "boolean"              => ArgumentExtractor.BooleanArg
      case "int"                  => ArgumentExtractor.IntArg
      case "long"                 => ArgumentExtractor.LongArg
      case "double"               => ArgumentExtractor.DoubleArg
      case "float"                => ArgumentExtractor.FloatArg
      case "short"                => ArgumentExtractor.ShortArg
      case "void"                 => ArgumentExtractor.UnitArg
      case _                      =>
        throw new IllegalArgumentException(
          s"Method 'ArgumentExtractor.getArgumentExtractor' is not support for $typeName"
        )
}
trait GenericArgExtractor extends ArgExtractorDerivation {

  implicit lazy val UnitArg: ArgumentExtractor[Unit] = (_: SymphonyQLInputValue) => Right(())

  implicit lazy val IntArg: ArgumentExtractor[Int] = (input: SymphonyQLInputValue) =>
    LongArg.extract(input).map(_.toInt)

  implicit lazy val ShortArg: ArgumentExtractor[Short] = (input: SymphonyQLInputValue) =>
    LongArg.extract(input).map(_.toShort)

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

  implicit lazy val BigIntArg: ArgumentExtractor[BigInt] = {
    case value: IntValue => Right(value.toBigInt)
    case other           => Left(ArgumentError(s"Cannot build a BigInt from input $other"))
  }

  implicit lazy val BigIntegerArg: ArgumentExtractor[java.math.BigInteger] = BigIntArg.map(_.underlying())

  implicit lazy val BigDecimalArg: ArgumentExtractor[BigDecimal] = {
    case value: IntValue   => Right(scala.math.BigDecimal(value.toBigInt))
    case value: FloatValue => Right(value.toBigDecimal)
    case other             => Left(ArgumentError(s"Cannot build a BigDecimal from input $other"))
  }

  implicit lazy val JavaBigDecimalArg: ArgumentExtractor[java.math.BigDecimal] = BigDecimalArg.map(_.underlying())

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
