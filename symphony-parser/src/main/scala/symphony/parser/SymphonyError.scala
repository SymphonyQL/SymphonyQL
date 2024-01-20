package symphony.parser

import scala.util.control.NoStackTrace

import symphony.parser.adt.LocationInfo
import symphony.parser.value.ResponseValue
import symphony.parser.value.ResponseValue.*
import symphony.parser.value.ResponseValue.ListValue
import symphony.parser.value.Value.StringValue

sealed trait SymphonyError extends NoStackTrace with Product with Serializable {
  def msg: String
  override def getMessage: String = msg

  def toResponseValue: ResponseValue
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

    def toResponseValue: ResponseValue =
      ObjectValue(
        List(
          "message"    -> Some(StringValue(s"Parsing Error: $msg")),
          "locations"  -> locationInfo.map(li => ListValue(List(li.toResponseValue))),
          "extensions" -> extensions
        ).collect { case (name, Some(v)) => name -> v }
      )
  }
}
