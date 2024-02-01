package symphony.derivation

import scala.compiletime.*
import scala.deriving.*

import magnolia1.Macro
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLInputValue
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.schema.*

object ArgumentExtractorGen extends ArgumentExtractorGen {
  inline given auto[A]: ArgumentExtractor[A] = gen[A]
}

trait ArgumentExtractorGen extends BaseDerivation {
  import BaseDerivation.*

  inline def gen[A]: ArgumentExtractor[A] =
    inline summonInline[Mirror.Of[A]] match {
      case m: Mirror.SumOf[A]     =>
        makeSum[A](
          constValue[m.MirroredLabel],
          recurseSum[A, m.MirroredElemLabels, m.MirroredElemTypes](Nil)
        )
      case m: Mirror.ProductOf[A] =>
        makeProduct(recurseProduct[A, m.MirroredElemLabels, m.MirroredElemTypes](Nil))(m.fromProduct)
    }

  private inline def recurseSum[P, Label, A <: Tuple](
    inline values: List[(String, ArgumentExtractor[Any])]
  ): List[(String, ArgumentExtractor[Any])] =
    inline erasedValue[(Label, A)] match {
      case (_: EmptyTuple, _)                => values.reverse
      case (_: (head *: tail), _: (t *: ts)) =>
        recurseSum[P, tail, ts] {
          inline summonInline[Mirror.Of[t]] match {
            case m: Mirror.SumOf[t] =>
              recurseSum[t, m.MirroredElemLabels, m.MirroredElemTypes](values)
            case _                  =>
              val extractor =
                if (Macro.isEnum[t])
                  if (!implicitExists[ArgumentExtractor[t]]) gen[t]
                  else summonInline[ArgumentExtractor[t]]
                else summonInline[ArgumentExtractor[t]]
              (constValue[head].toString, extractor.asInstanceOf[ArgumentExtractor[Any]]) :: values
          }
        }
    }

  private inline def recurseProduct[P, Label, A <: Tuple](
    values: List[(String, ArgumentExtractor[Any])]
  ): List[(String, ArgumentExtractor[Any])] =
    inline erasedValue[(Label, A)] match {
      case (_: EmptyTuple, _)                 => values.reverse
      case (_: (name *: names), _: (t *: ts)) =>
        recurseProduct[P, names, ts](
          (constValue[name].toString, summonInline[ArgumentExtractor[t]].asInstanceOf[ArgumentExtractor[Any]]) :: values
        )
    }

  private def makeSum[A](
    traitLabel: String,
    subTypes: => List[(String, ArgumentExtractor[Any])]
  ) = new ArgumentExtractor[A] {

    def extract(input: SymphonyQLInputValue): Either[ArgumentError, A] =
      input.match {
        case EnumValue(value)   => Right(value)
        case StringValue(value) => Right(value)
        case _                  => Left(ArgumentError(s"Cannot build a trait from input $input"))
      }.flatMap { value =>
        subTypes.collectFirst {
          case (label, builder: ArgumentExtractor[A @unchecked]) if label == value => builder
        }
          .toRight(ArgumentError(s"Invalid SymphonyQL value $value for trait $traitLabel"))
          .flatMap(_.extract(SymphonyQLInputValue.ObjectValue(Map.empty)))
      }
  }

  private def makeProduct[A](
    _fields: => List[(String, ArgumentExtractor[Any])]
  )(fromProduct: Product => A) = new ArgumentExtractor[A] {

    def extract(input: SymphonyQLInputValue): Either[ArgumentError, A] =
      _fields.map { (label, builder) =>
        input match {
          case SymphonyQLInputValue.ObjectValue(fields) =>
            fields
              .get(label)
              .map(builder.extract)
              .getOrElse(Left(ArgumentError(s"Cannot build a case class from input $input")))
          case value                                    => builder.extract(value)
        }
      }.foldLeft[Either[ArgumentError, Tuple]](Right(EmptyTuple)) { (acc, item) =>
        item match {
          case Right(value) => acc.map(_ :* value)
          case Left(e)      => Left(e)
        }
      }.map(fromProduct)
  }
}
