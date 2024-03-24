package symphony.server

import scala.util.Try

import spray.json.*
import symphony.*
import symphony.parser.*
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.LocationInfo

trait InputFormats extends DefaultJsonProtocol {

  implicit lazy val symphonyQLInputValueJsonFormat: JsonFormat[SymphonyQLInputValue] =
    new JsonFormat[SymphonyQLInputValue] {
      override def read(json: JsValue): SymphonyQLInputValue = jsonToInputValue(json)

      override def write(obj: SymphonyQLInputValue): JsValue =
        obj match
          case NullValue                                => JsNull
          case IntValue.IntNumber(value)                => JsNumber(value)
          case IntValue.LongNumber(value)               => JsNumber(value)
          case IntValue.BigIntNumber(value)             => JsNumber(value)
          case FloatValue.FloatNumber(value)            => JsNumber(value)
          case FloatValue.DoubleNumber(value)           => JsNumber(value)
          case FloatValue.BigDecimalNumber(value)       => JsNumber(value)
          case StringValue(value)                       => JsString(value)
          case BooleanValue(value)                      => JsBoolean(value)
          case EnumValue(value)                         => JsString(value)
          case SymphonyQLInputValue.ListValue(values)   => JsArray(values.map(symphonyQLInputValueJsonFormat.write): _*)
          case SymphonyQLInputValue.ObjectValue(fields) =>
            JsObject(fields.map(kv => kv._1 -> symphonyQLInputValueJsonFormat.write(kv._2)).toList: _*)
          case SymphonyQLInputValue.VariableValue(name) => JsString(name)
    }

  def jsonToInputValue(json: JsValue): SymphonyQLInputValue =
    json match
      case JsObject(fields)   =>
        SymphonyQLInputValue.ObjectValue(fields.map(kv => kv._1 -> jsonToInputValue(kv._2)))
      case JsArray(elements)  => SymphonyQLInputValue.ListValue(elements.map(jsonToInputValue).toList)
      case JsString(str)      => SymphonyQLValue.StringValue(str)
      case JsNumber(number)   =>
        Try(IntValue(number.toBigInt)).orElse(Try(FloatValue(number))).getOrElse(FloatValue(number.toDouble))
      case boolean: JsBoolean => SymphonyQLValue.BooleanValue(boolean.value)
      case JsNull             => SymphonyQLValue.NullValue

}

trait OutputFormats extends DefaultJsonProtocol {

  implicit lazy val objectValueJsonFormat: JsonFormat[SymphonyQLOutputValue.ObjectValue] =
    new JsonFormat[SymphonyQLOutputValue.ObjectValue] {

      override def read(json: JsValue): SymphonyQLOutputValue.ObjectValue = json match
        case JsObject(fields) =>
          SymphonyQLOutputValue.ObjectValue(fields.toList.map(kv => kv._1 -> jsonToOutputValue(kv._2)))
        case _                => SymphonyQLOutputValue.ObjectValue.apply(List.empty)

      override def write(obj: SymphonyQLOutputValue.ObjectValue): JsValue =
        JsObject(obj.fields.map(kv => kv._1 -> symphonyQLOutputValueJsonFormat.write(kv._2)).toList: _*)
    }

  implicit lazy val symphonyQLOutputValueJsonFormat: JsonFormat[SymphonyQLOutputValue] =
    new JsonFormat[SymphonyQLOutputValue] {
      override def read(json: JsValue): SymphonyQLOutputValue = jsonToOutputValue(json)

      override def write(obj: SymphonyQLOutputValue): JsValue = obj match {
        case NullValue                                       => JsNull
        case IntValue.IntNumber(value)                       => JsNumber(value)
        case IntValue.LongNumber(value)                      => JsNumber(value)
        case IntValue.BigIntNumber(value)                    => JsNumber(value)
        case FloatValue.FloatNumber(value)                   => JsNumber(value)
        case FloatValue.DoubleNumber(value)                  => JsNumber(value)
        case FloatValue.BigDecimalNumber(value)              => JsNumber(value)
        case StringValue(value)                              => JsString(value)
        case BooleanValue(value)                             => JsBoolean(value)
        case EnumValue(value)                                => JsString(value)
        case SymphonyQLOutputValue.ListValue(values)         => JsArray(values.map(symphonyQLOutputValueJsonFormat.write): _*)
        case obj @ SymphonyQLOutputValue.ObjectValue(fields) => objectValueJsonFormat.write(obj)
        case s: SymphonyQLOutputValue.StreamValue            => JsString(s.toString)
      }
    }

  def jsonToOutputValue(json: JsValue): SymphonyQLOutputValue =
    json match
      case JsObject(fields)   =>
        SymphonyQLOutputValue.ObjectValue(fields.toList.map(kv => kv._1 -> jsonToOutputValue(kv._2)))
      case JsArray(elements)  => SymphonyQLOutputValue.ListValue(elements.map(jsonToOutputValue).toList)
      case JsString(str)      => SymphonyQLValue.StringValue(str)
      case JsNumber(number)   =>
        Try(IntValue(number.toBigInt)).orElse(Try(FloatValue(number))).getOrElse(FloatValue(number.toDouble))
      case boolean: JsBoolean => SymphonyQLValue.BooleanValue(boolean.value)
      case JsNull             => SymphonyQLValue.NullValue

}

trait JsonFormats extends InputFormats with OutputFormats {

  implicit lazy val symphonyQLPathValueJsonFormat: JsonFormat[SymphonyQLPathValue] =
    new JsonFormat[SymphonyQLPathValue] {

      override def read(json: JsValue): SymphonyQLPathValue = json match {
        case JsString(value) => SymphonyQLPathValue.Key(value)
        case JsNumber(value) => SymphonyQLPathValue.Index(value.toInt)
        case _               => throw DeserializationException("failed to decode as string or int")
      }

      override def write(obj: SymphonyQLPathValue): JsValue =
        obj match
          case StringValue(value)        => JsString(value)
          case IntValue.IntNumber(value) => JsNumber(value)
    }

  implicit lazy val locationInfoJsonFormat: JsonFormat[LocationInfo] = new JsonFormat[LocationInfo] {

    override def read(json: JsValue): LocationInfo = json match {
      case JsObject(fields) =>
        LocationInfo(
          fields.get("column").map(_.convertTo[Int]).getOrElse(0),
          fields.get("line").map(_.convertTo[Int]).getOrElse(0)
        )
      case _                => throw DeserializationException("failed to decode as int")
    }

    override def write(obj: LocationInfo): JsValue = obj.toOutputValue.toJson
  }

  implicit lazy val symphonyQLErrorJsonFormat: JsonFormat[SymphonyQLError] = new JsonFormat[SymphonyQLError] {
    override def write(obj: SymphonyQLError): JsValue = obj.toOutputValue.toJson

    override def read(json: JsValue): SymphonyQLError =
      json match
        case JsObject(fields) =>
          SymphonyQLError.ExecutionError(
            fields.getOrElse("message", JsNull).convertTo[String],
            fields.get("path").map(_.convertTo[List[SymphonyQLPathValue]]).getOrElse(List.empty),
            fields.get("locations").map(_.convertTo[LocationInfo]),
            None,
            fields.get("extensions").map(_.convertTo[SymphonyQLOutputValue.ObjectValue])
          )
        case _                =>
          throw DeserializationException(s"Invalid json format: $json")
  }

  implicit lazy val symphonyQLRequestJsonFormat: JsonFormat[SymphonyQLRequest] = new JsonFormat[SymphonyQLRequest] {

    override def write(obj: SymphonyQLRequest): JsValue = JsObject(
      "query"         -> obj.query.toJson,
      "operationName" -> obj.operationName.toJson,
      "variables"     -> obj.variables.toJson,
      "extensions"    -> obj.extensions.toJson
    )

    override def read(json: JsValue): SymphonyQLRequest =
      json match
        case JsObject(fields) =>
          SymphonyQLRequest(
            fields.getOrElse("query", JsNull).convertTo[String],
            fields.get("operationName").map(_.convertTo[String]),
            fields.get("variables").map(_.convertTo[Map[String, SymphonyQLInputValue]]),
            fields.get("extensions").map(_.convertTo[Map[String, SymphonyQLInputValue]])
          )
        case _                =>
          throw DeserializationException(s"Invalid json format: $json")

  }

  implicit lazy val symphonyQLResponseJsonFormat: JsonFormat[SymphonyQLResponse[SymphonyQLError]] =
    new JsonFormat[SymphonyQLResponse[SymphonyQLError]] {

      override def write(obj: SymphonyQLResponse[SymphonyQLError]): JsValue = obj.toOutputValue.toJson

      override def read(json: JsValue): SymphonyQLResponse[SymphonyQLError] =
        json match
          case JsObject(fields) =>
            SymphonyQLResponse(
              fields.get("data").map(_.convertTo[SymphonyQLOutputValue]).getOrElse(SymphonyQLValue.NullValue),
              fields.get("errors").map(_.convertTo[List[SymphonyQLError]]).getOrElse(List.empty)
            )
          case _                =>
            throw DeserializationException(s"Invalid json format: $json")
    }

}
