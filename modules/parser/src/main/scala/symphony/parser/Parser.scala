package symphony.parser

import scala.concurrent.Future

import SymphonyError.ParsingError
import symphony.parser.adt.Document
import symphony.parser.value.InputValue

object Parser {
  def parseQuery(query: String): Either[ParsingError, Document] = ???
  def check(query: String): Option[String]                      = ???
}
