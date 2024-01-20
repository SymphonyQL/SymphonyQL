package symphony.parser

import scala.concurrent.Future

import SymphonyError.ParsingError
import symphony.parser.adt.Document
import symphony.parser.value.InputValue

object Parser {
  def parseQuery(query: String): Future[Either[ParsingError, Document]]   = ???
  def parseInputValue(rawValue: String): Either[ParsingError, InputValue] = ???
  def parseName(rawValue: String): Either[ParsingError, String]           = ???
  def check(query: String): Option[String]                                = ???
}
