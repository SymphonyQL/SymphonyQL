package symphony.apt.tests

import org.scalactic.Explicitly.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.parser.*
import symphony.schema.derivation.*

class GeneratedInputObjectSpec extends AnyFunSpec with Matchers {

  describe("InputSchema Spec") {
    it("simple input schema") {
      val document = getDocument(NestedObjectInputSchema.schema)
      val str      = DocumentRenderer.render(document).trim
      str shouldEqual """scalar BigDecimal
                        |scalar Short
                        |
                        |enum GQLOriginEnum {
                        |  "EARTH"
                        |  EARTH @deprecated(reason: "deprecated")
                        |  MARS
                        |  BELT
                        |}
                        |
                        |"NestedObject"
                        |type NObject {
                        |  "OriginEnum"
                        |  originEnum: GQLOriginEnum! @deprecated(reason: "deprecated")
                        |  optionalEnum: GQLOriginEnum
                        |  optionalString: String
                        |  ssOptionalEnum: [[GQLOriginEnum]!]!
                        |  sString: [String]!
                        |  ssString: [[String]!]!
                        |  stringV: String
                        |  intV: Int!
                        |  doubleV: Float!
                        |  floatV: Float!
                        |  shortV: Short!
                        |  bigDecimalV: BigDecimal
                        |  optionalBigDecimal: BigDecimal
                        |}""".stripMargin
    }

    it("simple input extractor") {
      val obj = SimpleNestedObjectExtractor.extractor.extract(
        SymphonyQLInputValue.ObjectValue(
          Map(
            "originEnum" -> SymphonyQLValue.StringValue("BELT")
          )
        )
      )
      obj shouldEqual Right(SimpleNestedObject(OriginEnum.BELT))
    }

    it("complex input schema") {
      val document = getDocument(InputObjectInputSchema.schema, true)
      val str      = DocumentRenderer.render(document).trim
      str shouldEqual
        """scalar BigDecimal
          |scalar Short
          |
          |enum GQLOriginEnum {
          |  "EARTH"
          |  EARTH @deprecated(reason: "deprecated")
          |  MARS
          |  BELT
          |}
          |
          |"InputObject"
          |input InputObjectInput {
          |  originEnum: GQLOriginEnum!
          |  org: GQLOriginEnum
          |  "Optional"
          |  optionalString: String
          |  ssOptionalEnum: [[GQLOriginEnum]!]!
          |  sString: [String]!
          |  ssString: [[String]!]!
          |  stringV: String
          |  intV: Int!
          |  doubleV: Float!
          |  floatV: Float!
          |  shortV: Short!
          |  bigDecimalV: BigDecimal
          |  optionalBigDecimal: BigDecimal
          |  optionalNestedObject: NObjectInput
          |  sNestedObject: [NObjectInput!]!
          |  optionalNestedObjects: [NObjectInput!]
          |}
          |
          |"NestedObject"
          |input NObjectInput {
          |  "OriginEnum"
          |  originEnum: GQLOriginEnum!
          |  optionalEnum: GQLOriginEnum
          |  optionalString: String
          |  ssOptionalEnum: [[GQLOriginEnum]!]!
          |  sString: [String]!
          |  ssString: [[String]!]!
          |  stringV: String
          |  intV: Int!
          |  doubleV: Float!
          |  floatV: Float!
          |  shortV: Short!
          |  bigDecimalV: BigDecimal
          |  optionalBigDecimal: BigDecimal
          |}""".stripMargin
    }

  }
}
