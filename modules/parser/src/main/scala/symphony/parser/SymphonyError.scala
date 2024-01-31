package symphony.parser

import scala.util.control.NoStackTrace

import symphony.parser.*
import symphony.parser.OutputValue.*
import symphony.parser.Value.StringValue
import symphony.parser.adt.LocationInfo

sealed trait SymphonyError extends NoStackTrace with Product with Serializable {
  def msg: String
  override def getMessage: String = msg

  def toOutputValue: OutputValue
}

object SymphonyError {

  final case class ParsingError(
    msg: String,
    locationInfo: Option[LocationInfo] = None,
    innerThrowable: Option[Throwable] = None,
    extensions: Option[ObjectValue] = None
  ) extends SymphonyError {
    override def toString: String = s"Parsing Error: $msg ${innerThrowable.fold("")(_.toString)}"

    override def getCause: Throwable = innerThrowable.orNull

    def toOutputValue: OutputValue =
      ObjectValue(
        List(
          "message"    -> Some(StringValue(s"Parsing Error: $msg")),
          "locations"  -> locationInfo.map(li => ListValue(List(li.toOutputValue))),
          "extensions" -> extensions
        ).collect { case (name, Some(v)) => name -> v }
      )
  }

  final case class ValidationError(
    msg: String,
    explanatoryText: String,
    locationInfo: Option[LocationInfo] = None,
    extensions: Option[ObjectValue] = None
  ) extends SymphonyError {
    override def toString: String = s"ValidationError Error: $msg"

    def toOutputValue: OutputValue =
      ObjectValue(
        List(
          "message"    -> Some(StringValue(msg)),
          "locations"  -> locationInfo.map(li => ListValue(List(li.toOutputValue))),
          "extensions" -> extensions
        ).collect { case (name, Some(v)) => name -> v }
      )
  }

  final case class ArgumentError(
    msg: String,
    locationInfo: Option[LocationInfo] = None,
    extensions: Option[ObjectValue] = None
  ) extends SymphonyError {
    override def toString: String = s"ArgumentError Error: $msg"

    def toOutputValue: OutputValue =
      ObjectValue(
        List(
          "message"    -> Some(StringValue(msg)),
          "locations"  -> locationInfo.map(li => ListValue(List(li.toOutputValue))),
          "extensions" -> extensions
        ).collect { case (name, Some(v)) => name -> v }
      )
  }

  final case class ExecutionError(
    msg: String,
    path: List[PathValue] = Nil,
    locationInfo: Option[LocationInfo] = None,
    innerThrowable: Option[Throwable] = None,
    extensions: Option[ObjectValue] = None
  ) extends SymphonyError {
    override def toString: String = s"Execution Error: $msg ${innerThrowable.fold("")(_.toString)}"

    override def getCause: Throwable = innerThrowable.orNull

    def toOutputValue: OutputValue =
      ObjectValue(
        List(
          "message"    -> Some(StringValue(msg)),
          "locations"  -> locationInfo.map(li => ListValue(List(li.toOutputValue))),
          "path"       -> Some(path).collect { case p if p.nonEmpty => ListValue(p) },
          "extensions" -> extensions
        ).collect { case (name, Some(v)) => name -> v }
      )
  }
}
