package symphony.apt.tests

import org.scalactic.Explicitly.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*

import symphony.parser.*
import symphony.schema.*
import symphony.schema.derivation.*

class GeneratedObjectSpec extends AnyFunSpec with Matchers {

  describe("ObjectSchema Spec") {
    it("simple object schema") {
      val document = getDocument(AuthorSchema.schema)
      val str      = DocumentRenderer.render(document).trim
      str shouldEqual """type Author {
                        |  name: String
                        |}""".stripMargin
    }

    it("union schema stage") {
      val author = SearchResultSchema.schema.analyze(SearchResult.author)
      val book   = SearchResultSchema.schema.analyze(SearchResult.book)
      (author, book) shouldEqual (
        Stage.ObjectStage("Author", Map("name" -> PureStage(SymphonyQLValue.StringValue("author")))),
        Stage.ObjectStage("Book", Map("title" -> PureStage(SymphonyQLValue.StringValue("book"))))
      )
    }

    it("simple union schema") {
      val document = getDocument(SearchResultSchema.schema)
      val str      = DocumentRenderer.render(document).trim
      str shouldEqual
        """"SearchResult"
          |union SearchResult = Book | Author
          |
          |type Author {
          |  name: String
          |}
          |
          |type Book {
          |  title: String
          |}""".stripMargin
    }

    it("interface schema") {
      val document = getDocument(NestedInterfaceSchema.schema)
      val str      = DocumentRenderer.render(document).trim
      str shouldEqual
        """interface Mid1 implements NestedInterface {
          |  b: String
          |  c: String
          |}
          |
          |interface Mid2 implements NestedInterface {
          |  b: String
          |  d: String
          |}
          |
          |interface NestedInterface {
          |  b: String
          |}
          |
          |type FooA implements Mid1 {
          |  a: String
          |  b: String
          |  c: String
          |}
          |
          |type FooB implements Mid1 & Mid2 {
          |  b: String
          |  c: String
          |  d: String
          |}
          |
          |type FooC implements Mid2 {
          |  b: String
          |  d: String
          |  e: String
          |}""".stripMargin
    }

    it("object schema for resolver") {
      val document = getDocument(QueriesObjectSchema.schema)
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
          |}
          |
          |"QueriesObject"
          |type GQLQueriesObject {
          |  "Function"
          |  characters(originEnum: GQLOriginEnum!, org: GQLOriginEnum, "Optional" optionalString: String, ssOptionalEnum: [[GQLOriginEnum]!]!, sString: [String]!, ssString: [[String]!]!, stringV: String, intV: Int!, doubleV: Float!, floatV: Float!, shortV: Short!, bigDecimalV: BigDecimal, optionalBigDecimal: BigDecimal, optionalNestedObject: NObjectInput, sNestedObject: [NObjectInput!]!, optionalNestedObjects: [NObjectInput!]): [OutputObject!] @deprecated(reason: "deprecated")
          |  character(originEnum: GQLOriginEnum!, org: GQLOriginEnum, "Optional" optionalString: String, ssOptionalEnum: [[GQLOriginEnum]!]!, sString: [String]!, ssString: [[String]!]!, stringV: String, intV: Int!, doubleV: Float!, floatV: Float!, shortV: Short!, bigDecimalV: BigDecimal, optionalBigDecimal: BigDecimal, optionalNestedObject: NObjectInput, sNestedObject: [NObjectInput!]!, optionalNestedObjects: [NObjectInput!]): OutputObject!
          |  intCharacter(value: Int): OutputObject!
          |  intString(value: String): String
          |  supplierArgCharacter: OutputObject!
          |  scalarField: String
          |  simpleNestedObject: SimpleNestedObject!
          |}
          |
          |"A key-value pair of ListOfKVNestedObjectString and ListOfListOfKVStringListOfNestedObject"
          |type KVListOfKVNestedObjectStringListOfListOfKVStringListOfNestedObject {
          |  "Key"
          |  key: [KVNestedObjectString!]!
          |  "Value"
          |  value: [[KVStringListOfNestedObject!]]!
          |}
          |
          |"A key-value pair of ListOfNestedObject and ListOfString"
          |type KVListOfNestedObjectListOfString {
          |  "Key"
          |  key: [NestedObject!]!
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of ListOfString and ListOfString"
          |type KVListOfStringListOfString {
          |  "Key"
          |  key: [String]!
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of NestedObject and ListOfNestedObject"
          |type KVNestedObjectListOfNestedObject {
          |  "Key"
          |  key: NestedObject
          |  "Value"
          |  value: [NestedObject]!
          |}
          |
          |"A key-value pair of NestedObject and ListOfString"
          |type KVNestedObjectListOfString {
          |  "Key"
          |  key: NestedObject!
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of NestedObject and String"
          |type KVNestedObjectString {
          |  "Key"
          |  key: NestedObject!
          |  "Value"
          |  value: String
          |}
          |
          |"A key-value pair of String and ListOfNestedObject"
          |type KVStringListOfNestedObject {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: [NestedObject!]!
          |}
          |
          |"A key-value pair of String and ListOfString"
          |type KVStringListOfString {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of String and NestedObject"
          |type KVStringNestedObject {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: NestedObject!
          |}
          |
          |"A key-value pair of String and String"
          |type KVStringString {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: String
          |}
          |
          |"NestedObject"
          |type NestedObject {
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
          |}
          |
          |type OutputObject {
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
          |  bigDecimalV: BigDecimal
          |  optionalBigDecimal: BigDecimal
          |  optionalNestedObject: NestedObject
          |  sNestedObject: [NestedObject!]!
          |  sStringMap: [[KVStringString!]!]!
          |  sNestedObjectMap: [[KVStringNestedObject!]!]!
          |  sOptionalStringListNestedObjectMap: [[KVStringListOfNestedObject!]!]!
          |  stringMap: [KVStringString!]!
          |  nestedObjectMap: [KVStringNestedObject!]!
          |  optionalNestedObjectMap: [KVStringNestedObject!]!
          |  listNestedObjectMap: [KVStringListOfNestedObject!]!
          |  listOptionalNestedObjectMap: [KVStringListOfNestedObject!]!
          |  listOptionalStringMap: [KVStringListOfString!]!
          |  optionalListStringMap: [KVStringListOfString!]!
          |  optionalListOutputNestedObjectMap: [KVStringListOfNestedObject!]!
          |  optionalListOptionalStringMap: [KVStringListOfString!]!
          |  listListOptionalStringMap: [KVListOfStringListOfString!]!
          |  nestedObjectStringMap: [KVNestedObjectString!]!
          |  nestedObjectListStringMap: [KVNestedObjectListOfString!]!
          |  optionalNestedObjectListOptionalNestedObjectMap: [KVNestedObjectListOfNestedObject!]!
          |  listNestedObjectMapStringListOptionalMap: [KVListOfNestedObjectListOfString!]!
          |  complexMap: [KVListOfKVNestedObjectStringListOfListOfKVStringListOfNestedObject!]!
          |}
          |
          |type SimpleNestedObject {
          |  originEnum: GQLOriginEnum!
          |}""".stripMargin
    }

    it("complex object schema") {
      val document = getDocument(OutputObjectSchema.schema)
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
          |"A key-value pair of ListOfKVNestedObjectString and ListOfListOfKVStringListOfNestedObject"
          |type KVListOfKVNestedObjectStringListOfListOfKVStringListOfNestedObject {
          |  "Key"
          |  key: [KVNestedObjectString!]!
          |  "Value"
          |  value: [[KVStringListOfNestedObject!]]!
          |}
          |
          |"A key-value pair of ListOfNestedObject and ListOfString"
          |type KVListOfNestedObjectListOfString {
          |  "Key"
          |  key: [NestedObject!]!
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of ListOfString and ListOfString"
          |type KVListOfStringListOfString {
          |  "Key"
          |  key: [String]!
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of NestedObject and ListOfNestedObject"
          |type KVNestedObjectListOfNestedObject {
          |  "Key"
          |  key: NestedObject
          |  "Value"
          |  value: [NestedObject]!
          |}
          |
          |"A key-value pair of NestedObject and ListOfString"
          |type KVNestedObjectListOfString {
          |  "Key"
          |  key: NestedObject!
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of NestedObject and String"
          |type KVNestedObjectString {
          |  "Key"
          |  key: NestedObject!
          |  "Value"
          |  value: String
          |}
          |
          |"A key-value pair of String and ListOfNestedObject"
          |type KVStringListOfNestedObject {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: [NestedObject!]!
          |}
          |
          |"A key-value pair of String and ListOfString"
          |type KVStringListOfString {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: [String]!
          |}
          |
          |"A key-value pair of String and NestedObject"
          |type KVStringNestedObject {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: NestedObject!
          |}
          |
          |"A key-value pair of String and String"
          |type KVStringString {
          |  "Key"
          |  key: String
          |  "Value"
          |  value: String
          |}
          |
          |"NestedObject"
          |type NestedObject {
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
          |}
          |
          |type OutputObject {
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
          |  bigDecimalV: BigDecimal
          |  optionalBigDecimal: BigDecimal
          |  optionalNestedObject: NestedObject
          |  sNestedObject: [NestedObject!]!
          |  sStringMap: [[KVStringString!]!]!
          |  sNestedObjectMap: [[KVStringNestedObject!]!]!
          |  sOptionalStringListNestedObjectMap: [[KVStringListOfNestedObject!]!]!
          |  stringMap: [KVStringString!]!
          |  nestedObjectMap: [KVStringNestedObject!]!
          |  optionalNestedObjectMap: [KVStringNestedObject!]!
          |  listNestedObjectMap: [KVStringListOfNestedObject!]!
          |  listOptionalNestedObjectMap: [KVStringListOfNestedObject!]!
          |  listOptionalStringMap: [KVStringListOfString!]!
          |  optionalListStringMap: [KVStringListOfString!]!
          |  optionalListOutputNestedObjectMap: [KVStringListOfNestedObject!]!
          |  optionalListOptionalStringMap: [KVStringListOfString!]!
          |  listListOptionalStringMap: [KVListOfStringListOfString!]!
          |  nestedObjectStringMap: [KVNestedObjectString!]!
          |  nestedObjectListStringMap: [KVNestedObjectListOfString!]!
          |  optionalNestedObjectListOptionalNestedObjectMap: [KVNestedObjectListOfNestedObject!]!
          |  listNestedObjectMapStringListOptionalMap: [KVListOfNestedObjectListOfString!]!
          |  complexMap: [KVListOfKVNestedObjectStringListOfListOfKVStringListOfNestedObject!]!
          |}""".stripMargin
    }
  }
}
