package symphony.parser.parsers

import org.parboiled2.*
import org.parboiled2.support.hlist
import org.parboiled2.support.hlist.HNil

import symphony.parser.*
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*

abstract class ValueParser extends CommonParser {

  def input: ParserInput

  // ========================================Values===================================================================
  def booleanValue: Rule1[BooleanValue] = rule {
    "true" ~ push(BooleanValue(true)) | "false" ~ push(BooleanValue(false))
  }

  def intValue: Rule1[IntValue] = rule {
    capture(integerPart) ~> ((v: String) => IntValue.stringToIntValue(v))
  }

  def floatValue: Rule1[FloatValue] = rule {
    capture(
      (integerPart ~ ignored ~ fractionalPart) |
        (integerPart ~ ignored ~ exponentPart) |
        (integerPart ~ ignored ~ fractionalPart ~ ignored ~ exponentPart)
    ) ~> { t => FloatValue(t) }
  }

  def nullValue: Rule1[SymphonyQLInputValue] = rule {
    str("null") ~> (() => NullValue)
  }

  def enumValue: Rule1[SymphonyQLInputValue] = rule {
    name ~> EnumValue.apply
  }

  def listValue: Rule1[ListValue] = rule {
    "[" ~!~ value.*.separatedBy(ignored) ~ "]" ~> (values => ListValue(values.toList))
  }

  def objectField: Rule1[(String, SymphonyQLInputValue)] = rule {
    name ~ ":" ~!~ ignored ~ value ~> { (n, v) => n -> v }
  }

  def objectValue: Rule1[ObjectValue] = rule {
    "{" ~ ignored ~ objectField.*.separatedBy(ignored) ~ ignored ~ "}" ~> (values => ObjectValue(values.toMap))
  }

  def variableValue: Rule1[VariableValue] = rule {
    "$" ~ name ~> VariableValue.apply
  }

  def defaultValue: Rule1[SymphonyQLInputValue] = rule {
    "=" ~ ignored ~!~ value
  }

  def value: Rule1[SymphonyQLInputValue] =
    rule {
      floatValue | intValue | booleanValue | stringValue | nullValue | enumValue | listValue | objectValue | variableValue
    }
}
