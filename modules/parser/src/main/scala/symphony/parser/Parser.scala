package symphony
package parser

import scala.util.*

import org.parboiled2.*
import org.parboiled2.Rule.*

import parsers.*
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

  def documentParser(input: ParserInput): DefinitionParser = new DefinitionParser(input)

  // ========================================Parser API===================================================================
  def parseQuery(query: String): Either[ParsingError, Document] = {
    val input  = ParserInput(query)
    val parser = Parser.documentParser(input)
    parser.document.run() match
      case Failure(exception) =>
        exception.printStackTrace()
        Left(ParsingError(s"Query parsing error", innerThrowable = Some(exception)))
      case Success(value) => Right(Document(value.definitions, SourceMapper(query)))
  }

  def check(query: String): Option[String] = {
    val input  = ParserInput(query)
    val parser = Parser.documentParser(input)
    parser.document.run() match
      case Failure(exception) => Some(exception.getMessage)
      case Success(_)         => None
  }

  def parseInputValue(query: String): Either[ParsingError, InputValue] = {
    val input  = ParserInput(query)
    val parser = Parser.documentParser(input)
    parser.value.run() match
      case Failure(exception) => Left(ParsingError(s"InputValue parsing error", innerThrowable = Some(exception)))
      case Success(value)     => Right(value)
  }
}
