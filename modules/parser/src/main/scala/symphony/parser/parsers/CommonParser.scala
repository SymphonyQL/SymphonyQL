package symphony.parser.parsers

import org.parboiled2.*
import org.parboiled2.support.hlist
import org.parboiled2.support.hlist.HNil

import symphony.parser.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.Type.*

abstract class CommonParser extends Parser {

  def input: ParserInput

  private val charPredicate: CharPredicate = CharPredicate('\u0009', '\u0020' to '\uFFFF') -- "\n\r"

  def sourceCharacter: Rule0 = rule {
    CharPredicate('\u0009', '\u000A', '\u000D', '\u0020' to '\uFFFF')
  }

  def sourceCharacterWithoutLineTerminator: Rule0 = rule {
    charPredicate
  }

  def unicodeBOM: Rule0 = rule {
    "\uFEFF"
  }

  def whiteSpace: Rule0 = rule {
    anyOf("\u0009\u0020")
  }

  def lineTerminator: Rule0 = rule {
    "\u000A" | "\u000D" ~ ignored ~ !"\u000A" | "\u000D\u000A"
  }

  def comma: Rule0 = rule {
    ","
  }

  def commentChar: Rule0 = rule {
    sourceCharacterWithoutLineTerminator
  }

  def comment: Rule0 = rule {
    "#" ~ ignored ~ commentChar.*
  }

  def ignored: Rule0 = rule {
    (unicodeBOM | whiteSpace | lineTerminator | comment | comma).*
  }

  def sign: Rule0 = rule {
    "-" | "+"
  }

  def negativeSign: Rule0 = rule {
    "-"
  }

  def integerPart: Rule0 = rule {
    (negativeSign.? ~ "0") | (negativeSign.? ~ CharPredicate.Digit19 ~ CharPredicate.Digit.*)
  }

  def exponentIndicator: Rule0 = rule {
    anyOf("eE")
  }

  def exponentPart: Rule0 = rule {
    exponentIndicator ~ ignored ~ sign.? ~ ignored ~ CharPredicate.Digit.+
  }

  def fractionalPart: Rule0 = rule {
    "." ~ ignored ~ CharPredicate.Digit.+
  }

  // ========================================String Value===================================================================
  def escapedUnicode: Rule1[String] = rule {
    capture(
      CharPredicate.HexDigit ~ ignored ~
        CharPredicate.HexDigit ~ ignored ~
        CharPredicate.HexDigit ~ ignored ~ CharPredicate.HexDigit
    ) ~> (i => Integer.parseInt(i, 16).toChar.toString)
  }

  def escapedCharacter: Rule1[String] = rule {
    capture(CharPredicate.Empty.++("\"\\\\/bfnrt")) ~> { t =>
      t match
        case "b"   => "\b"
        case "n"   => "\n"
        case "f"   => "\f"
        case "r"   => "\r"
        case "t"   => "\t"
        case other => other
    }
  }

  def stringCharacter: Rule1[String] = rule {
    capture(
      oneOrMore(charPredicate -- '"' -- '\\' -- '\n' -- '\r')
    ) | "\\u" ~ ignored ~ escapedUnicode | "\\" ~ ignored ~ escapedCharacter
  }

  def blockStringCharacter: Rule1[String] = rule {
    capture("\\\"\"\"") ~> (_ => "\"\"\"") | capture(sourceCharacter)
  }

  def stringValue: Rule1[StringValue] = rule {
    (("\"\"\"" ~ ignored ~ ((!"\"\"\"") ~ blockStringCharacter).* ~> (s =>
      blockStringValue(s.mkString)
    ) ~ ignored ~ "\"\"\"") |
      ("\"" ~ ignored ~ stringCharacter.* ~> (_.mkString) ~ ignored ~ "\""))
      ~> (v => StringValue(v))
  }

  def blockStringValue(rawValue: String): String = {
    val l1: List[String] = rawValue.split("\r?\n").toList
    val commonIndent     = l1 match {
      case Nil       => None
      case _ :: tail =>
        tail.foldLeft(Option.empty[Int]) { case (commonIndent, line) =>
          val indent = "[ \t]*".r.findPrefixOf(line).map(_.length).getOrElse(0)
          if (indent < line.length && commonIndent.fold(true)(_ > indent)) Some(indent) else commonIndent
        }
    }
    // remove indentation
    val l2               = (commonIndent, l1) match {
      case (Some(value), head :: tail) => head :: tail.map(_.drop(value))
      case _                           => l1
    }
    // remove start lines that are only whitespaces
    val l3               = l2.dropWhile("[ \t]*".r.replaceAllIn(_, "").isEmpty)
    // remove end lines that are only whitespaces
    val l4               = l3.reverse.dropWhile("[ \t]*".r.replaceAllIn(_, "").isEmpty).reverse
    l4.mkString("\n")
  }

  // ========================================Value===================================================================
  def name: Rule1[String] = rule {
    capture(CharPredicate.Alpha ~ ignored ~ CharPredicate.AlphaNum.*)
  }

  def operationType: Rule1[OperationType] = rule {
    "query" ~ push(OperationType.Query) |
      "mutation" ~ push(OperationType.Mutation) |
      "subscription" ~ push(OperationType.Subscription)
  }
}
