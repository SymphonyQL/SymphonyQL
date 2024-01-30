package symphony
package parser

import scala.util.{ Failure, Success, Try }

import org.parboiled2.*
import org.parboiled2.support.hlist
import org.parboiled2.support.hlist.HNil

import symphony.parser.*
import symphony.parser.InputValue.*
import symphony.parser.SymphonyError.ParsingError
import symphony.parser.Value.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.*
import symphony.parser.adt.Type.*

object Parser {

  final case class ParsedDocument(definitions: List[Definition])

  final class GraphQLParser(val input: ParserInput) extends Parser {

    private val charPredicate: CharPredicate = CharPredicate('\u0009', '\u0020' to '\uFFFF') -- "\n\r"

    def sourceCharacter: Rule0 = rule {
      CharPredicate('\u0009', '\u000A', '\u000D', '\u0020' to '\uFFFF')
    }

    def sourceCharacterWithoutLineTerminator: Rule0 = rule {
      charPredicate
    }

    def unicodeBOM: Rule0 = rule { "\uFEFF" }

    def whiteSpace: Rule0 = rule { anyOf("\u0009\u0020") }

    def lineTerminator: Rule0 = rule {
      "\u000A" | "\u000D" ~ ignored ~ !"\u000A" | "\u000D\u000A"
    }

    def comma: Rule0 = rule { "," }

    def commentChar: Rule0 = rule { sourceCharacterWithoutLineTerminator }

    def comment: Rule0 = rule {
      "#" ~ ignored ~ commentChar.*
    }

    def ignored: Rule0 = rule {
      (unicodeBOM | whiteSpace | lineTerminator | comment | comma).*
    }

    def sign: Rule0 = rule { "-" | "+" }

    def negativeSign: Rule0 = rule { "-" }

    def booleanValue: Rule1[BooleanValue] = rule {
      "true" ~ push(BooleanValue(true)) | "false" ~ push(BooleanValue(false))
    }

    def integerPart: Rule0 = rule {
      (negativeSign.? ~ "0") | (negativeSign.? ~ CharPredicate.Digit19 ~ CharPredicate.Digit.*)
    }

    def intValue: Rule1[IntValue] = rule {
      capture(integerPart) ~> ((v: String) => IntValue.fromStringUnsafe(v))
    }

    def exponentIndicator: Rule0 = rule { anyOf("eE") }

    def exponentPart: Rule0 = rule {
      exponentIndicator ~ ignored ~ sign.? ~ ignored ~ CharPredicate.Digit.+
    }

    def fractionalPart: Rule0 = rule { "." ~ ignored ~ CharPredicate.Digit.+ }

    def floatValue: Rule1[FloatValue] = rule {
      capture(
        (integerPart ~ ignored ~ fractionalPart) |
          (integerPart ~ ignored ~ exponentPart) |
          (integerPart ~ ignored ~ fractionalPart ~ ignored ~ exponentPart)
      ) ~> { t => FloatValue(t) }
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
      val commonIndent = l1 match {
        case Nil => None
        case _ :: tail =>
          tail.foldLeft(Option.empty[Int]) { case (commonIndent, line) =>
            val indent = "[ \t]*".r.findPrefixOf(line).map(_.length).getOrElse(0)
            if (indent < line.length && commonIndent.fold(true)(_ > indent)) Some(indent) else commonIndent
          }
      }
      // remove indentation
      val l2 = (commonIndent, l1) match {
        case (Some(value), head :: tail) => head :: tail.map(_.drop(value))
        case _                           => l1
      }
      // remove start lines that are only whitespaces
      val l3 = l2.dropWhile("[ \t]*".r.replaceAllIn(_, "").isEmpty)
      // remove end lines that are only whitespaces
      val l4 = l3.reverse.dropWhile("[ \t]*".r.replaceAllIn(_, "").isEmpty).reverse
      l4.mkString("\n")
    }

    // ========================================Value===================================================================
    def name: Rule1[String] = rule {
      capture(CharPredicate.Alpha ~ ignored ~ CharPredicate.AlphaNum.*)
    }

    def nullValue: Rule1[InputValue] = rule {
      str("null") ~> (() => NullValue)
    }

    def enumValue: Rule1[InputValue] = rule {
      name ~> EnumValue.apply
    }

    def listValue: Rule1[ListValue] = rule {
      "[" ~!~ value.*.separatedBy(ignored) ~ "]" ~> (values => ListValue(values.toList))
    }

    def objectField: Rule1[(String, InputValue)] = rule {
      name ~ ":" ~!~ ignored ~ value ~> { (n, v) => n -> v }
    }

    def objectValue: Rule1[ObjectValue] = rule {
      "{" ~ ignored ~ objectField.*.separatedBy(ignored) ~ ignored ~ "}" ~> (values => ObjectValue(values.toMap))
    }

    def value: Rule1[InputValue] =
      rule {
        floatValue | intValue | booleanValue | stringValue | nullValue | enumValue | listValue | objectValue | variable
      }

    // ========================================Definition===================================================================
    def alias: Rule1[String] = rule {
      name ~ ":" ~ ignored
    }

    def argument: Rule1[(String, InputValue)] = rule {
      name ~ ":" ~ ignored ~ value ~> { (n, v) =>
        n -> v
      }
    }

    def arguments: Rule1[Map[String, InputValue]] = rule {
      "(" ~!~ ignored ~ argument.*.separatedBy(ignored) ~ ignored ~ ")" ~> (_.toMap)
    }

    def directive: Rule1[Directive] = rule("@" ~ name ~ arguments ~> { (name, arguments) =>
      Directive(name, arguments)
    })

    def directives: Rule1[List[Directive]] = rule { directive.*.separatedBy(ignored) ~> (_.toList) }

    def selection: Rule1[Selection] = rule { field | fragmentSpread | inlineFragment }

    def selectionSet: Rule1[List[Selection]] = rule {
      "{" ~!~ ignored ~ selection.*.separatedBy(ignored) ~ ignored ~ "}" ~> { x => x.toList }
    }

    def namedType: Rule1[NamedType] = rule { name ~ !(str("null") ~ EOI) ~> (NamedType(_, nonNull = false)) }

    def listType: Rule1[ListType] = rule {
      "[" ~ type_ ~ "]" ~> (t => ListType(t, nonNull = false))
    }

    def nonNullType: Rule1[Type] = rule {
      (namedType | listType) ~ "!" ~> {
        case t: NamedType => t.copy(nonNull = true)
        case t: ListType  => t.copy(nonNull = true)
      }
    }

    def type_ : Rule1[Type] = rule { nonNullType | namedType | listType }

    def variable: Rule1[VariableValue] = rule { "$" ~ name ~> VariableValue.apply }

    def variableDefinitions: Rule1[List[VariableDefinition]] = rule {
      "(" ~!~ ignored ~ variableDefinition.*.separatedBy(ignored) ~ ignored ~ ")" ~> (_.toList)
    }

    def variableDefinition: Rule1[VariableDefinition] = rule {
      variable ~ ":" ~ ignored ~!~ type_ ~ ignored ~ defaultValue.? ~ ignored ~ directives.? ~> {
        (v, t, default, dirs) =>
          VariableDefinition(v.name, t, default, dirs.toList.flatten)
      }
    }

    def defaultValue: Rule1[InputValue] = rule { "=" ~!~ value }

    def field: Rule1[Field] = rule {
      alias.? ~ ignored ~ name ~ ignored ~ arguments.? ~ ignored ~ directives.? ~ ignored ~ selectionSet.? ~> {
        (alias, name, args, dirs, sels) =>
          Field(
            alias,
            name,
            args.getOrElse(Map()),
            dirs.getOrElse(Nil),
            sels.getOrElse(Nil)
          )
      }
    }

    def fragmentName: Rule1[String] = rule {
      name ~ !(str("on") ~ EOI)
    }

    def fragmentSpread: Rule1[FragmentSpread] = rule {
      "..." ~ fragmentName ~ ignored ~ directives.? ~> { (name, dirs) =>
        FragmentSpread(name, dirs.toList.flatten)
      }
    }

    def typeCondition: Rule1[NamedType] = rule {
      "on" ~ ignored ~!~ namedType
    }

    def inlineFragment: Rule1[InlineFragment] = rule {
      "..." ~ typeCondition.? ~ ignored ~ directives.? ~ ignored ~ selectionSet ~> { (typeCondition, dirs, sel) =>
        InlineFragment(typeCondition, dirs.toList.flatten, sel)
      }
    }

    def operationType: Rule1[OperationType] = rule {
      "query" ~ push(OperationType.Query) |
        "mutation" ~ push(OperationType.Mutation) |
        "subscription" ~ push(OperationType.Subscription)
    }

    def operationDefinition: Rule1[OperationDefinition] = rule {
      operationType ~!~ ignored ~ name.? ~ ignored ~ variableDefinitions.? ~ ignored ~ directives ~ ignored ~ selectionSet ~> {
        (operationType, name, variableDefinitions, directives, selection) =>
          {
            OperationDefinition(operationType, name, variableDefinitions.getOrElse(Nil), directives, selection)
          }
      } | ignored ~ selectionSet ~> { sel =>
        OperationDefinition(OperationType.Query, None, Nil, Nil, sel)
      }
    }

    def fragmentDefinition: Rule1[FragmentDefinition] = rule {
      "fragment" ~!~ ignored ~ fragmentName ~ ignored ~ typeCondition ~ ignored ~ directives ~ ignored ~ selectionSet ~> {
        (name, typeCondition, dirs, sel) =>
          FragmentDefinition(name, typeCondition, dirs, sel)
      }
    }

    // ========================================Definition Set===================================================================
    def executableDefinition: Rule1[ExecutableDefinition] = rule { operationDefinition | fragmentDefinition }

    // TODO typeSystemDefinition
    def definition: Rule1[ExecutableDefinition] = executableDefinition

    def document: Rule1[ParsedDocument] = rule {
      ignored ~ definition.*.separatedBy(ignored) ~ ignored ~ EOI ~> { seq =>
        ParsedDocument(seq.toList)
      }
    }
  }

  // ========================================Parser API===================================================================
  def parseQuery(query: String): Either[ParsingError, Document] = {
    val input  = ParserInput(query)
    val parser = new GraphQLParser(input)
    parser.document.run() match
      case Failure(exception) =>
        exception.printStackTrace()
        Left(ParsingError(s"Query parsing error", innerThrowable = Some(exception)))
      case Success(value) => Right(Document(value.definitions, SourceMapper(query)))
  }

  def check(query: String): Option[String] = {
    val input  = ParserInput(query)
    val parser = new GraphQLParser(input)
    parser.document.run() match
      case Failure(exception) => Some(exception.getMessage)
      case Success(_)         => None
  }

  def parseInputValue(query: String): Either[ParsingError, InputValue] = {
    val input  = ParserInput(query)
    val parser = new GraphQLParser(input)
    parser.value.run() match
      case Failure(exception) => Left(ParsingError(s"Input parsing error", innerThrowable = Some(exception)))
      case Success(value)     => Right(value)
  }
}
