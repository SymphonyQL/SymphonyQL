package symphony.apt.tests

import org.scalactic.Explicitly.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.parser.*

class GeneratedEnumSpec extends AnyFunSpec with Matchers {

  describe("EnumSchema Spec") {
    it("simple enum schema") {
      val document = getDocument(OriginEnumSchema.schema)
      val str      = DocumentRenderer.render(document).trim
      str shouldEqual
        """enum GQLOriginEnum {
          |  "EARTH"
          |  EARTH @deprecated(reason: "deprecated")
          |  MARS
          |  BELT
          |}""".stripMargin
    }

    it("simple enum extractor with string value") {
      val enumValue = OriginEnumExtractor.extractor.extract(SymphonyQLValue.StringValue("MARS"))
      enumValue shouldEqual Right(
        OriginEnum.MARS
      )
    }

    it("simple enum extractor with enum value") {
      val enumValue = OriginEnumExtractor.extractor.extract(SymphonyQLValue.EnumValue("MARS"))
      enumValue shouldEqual Right(
        OriginEnum.MARS
      )
    }

    it("simple enum extractor with invalid value") {
      val enumValue = OriginEnumExtractor.extractor.extract(SymphonyQLValue.IntValue(1))
      enumValue shouldEqual Left(
        SymphonyQLError.ArgumentError("Expected EnumValue or StringValue")
      )
    }
  }
}
