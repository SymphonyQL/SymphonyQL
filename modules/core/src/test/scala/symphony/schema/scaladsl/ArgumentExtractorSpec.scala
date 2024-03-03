package symphony.schema.scaladsl

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import symphony.*
import symphony.parser.*
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.schema.scaladsl.*

class ArgumentExtractorSpec extends AnyFunSpec with Matchers {

  describe("ArgumentExtractor") {

    it("Long from string") {
      assertResult(Right(123))(
        ArgumentExtractor.LongArg.extract(SymphonyQLValue.IntValue.stringToIntValue("123"))
      )
    }

    it("works with derived case class ArgumentExtractor") {
      sealed abstract class Nullable[+T]
      case class SomeNullable[+T](t: T) extends Nullable[T]
      case object NullNullable          extends Nullable[Nothing]
      case object MissingNullable       extends Nullable[Nothing]

      implicit def nullableArgumentExtractor[A](implicit ev: ArgumentExtractor[A]): ArgumentExtractor[Nullable[A]] =
        new ArgumentExtractor[Nullable[A]] {
          def extract(input: SymphonyQLInputValue): Either[SymphonyQLError.ArgumentError, Nullable[A]]      = input match {
            case SymphonyQLValue.NullValue => Right(NullNullable)
            case _                         => ev.extract(input).map(SomeNullable(_))
          }
          override def default(default: Option[String]): Either[SymphonyQLError.ArgumentError, Nullable[A]] =
            Right(MissingNullable)
        }

      case class Wrapper(a: Nullable[String])

      val derivedAB = implicitly[ArgumentExtractor[Wrapper]]

      assert(derivedAB.extract(SymphonyQLInputValue.ObjectValue(Map.empty)) == Right(Wrapper(MissingNullable)))

      assert(
        derivedAB.extract(SymphonyQLInputValue.ObjectValue(Map("a" -> SymphonyQLValue.NullValue))) == Right(
          Wrapper(NullNullable)
        )
      )

      assert(
        derivedAB.extract(SymphonyQLInputValue.ObjectValue(Map("a" -> SymphonyQLValue.StringValue("x")))) == Right(
          Wrapper(SomeNullable("x"))
        )
      )
    }

  }

}
