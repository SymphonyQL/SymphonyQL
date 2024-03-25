package symphony.server

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*

import spray.json.*
import symphony.*
import symphony.parser.*
import symphony.parser.adt.LocationInfo

class JsonFormatsSpec extends AnyFunSpec with Matchers with JsonFormats with SprayJsonSupport {

  describe("JsonFormats Spec") {

    it("Deserialize SymphonyQLResponse") {
      val value     = SymphonyQLResponse(
        SymphonyQLOutputValue.ObjectValue(
          List("op" -> SymphonyQLValue.StringValue("name"))
        ),
        List.empty[SymphonyQLError]
      )
      val jsonValue = JsObject(
        "data" -> JsObject("op" -> JsString("name"))
      )
      val json      = symphonyQLResponseJsonFormat.write(value)
      val obj       = symphonyQLResponseJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }

    it("Deserialize SymphonyQLRequest") {
      val value     = SymphonyQLRequest("query")
      val jsonValue = JsObject(
        "extensions"    -> JsNull,
        "operationName" -> JsNull,
        "query"         -> JsString("query"),
        "variables"     -> JsNull
      )
      val json      = symphonyQLRequestJsonFormat.write(value)
      val obj       = symphonyQLRequestJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }

    it("Deserialize LocationInfo Index") {
      val value     = LocationInfo(1, 2)
      val jsonValue = JsObject("column" -> JsNumber(1), "line" -> JsNumber(2))
      val json      = locationInfoJsonFormat.write(value)
      val obj       = locationInfoJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }

    it("Deserialize SymphonyQLPathValue Index") {
      val value     = SymphonyQLPathValue.Index(1)
      val jsonValue = JsNumber(1)
      val json      = symphonyQLPathValueJsonFormat.write(value)
      val obj       = symphonyQLPathValueJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }

    it("Deserialize SymphonyQLPathValue Key") {
      val value     = SymphonyQLPathValue.Key("key")
      val jsonValue = JsString("key")
      val json      = symphonyQLPathValueJsonFormat.write(value)
      val obj       = symphonyQLPathValueJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }

    it("Deserialize SymphonyQLOutputValue") {
      val value     = SymphonyQLOutputValue.ListValue(
        List(
          SymphonyQLValue.NullValue,
          SymphonyQLOutputValue.ObjectValue(
            List(
              "enum"   -> SymphonyQLValue.StringValue("enum"),
              "float"  -> SymphonyQLValue.FloatValue(12.1),
              "int"    -> SymphonyQLValue.IntValue(12),
              "string" -> SymphonyQLValue.StringValue("string")
            )
          )
        )
      )
      val jsonValue = JsArray(
        JsNull,
        JsObject(
          "string" -> JsString("string"),
          "int"    -> JsNumber(12),
          "enum"   -> JsString("enum"),
          "float"  -> JsNumber(12.1)
        )
      )
      val json      = symphonyQLOutputValueJsonFormat.write(value)
      val obj       = symphonyQLOutputValueJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }

    it("Deserialize SymphonyQLInputValue") {
      val value     = SymphonyQLInputValue.ListValue(
        List(
          SymphonyQLValue.NullValue,
          SymphonyQLInputValue.ObjectValue(
            Map(
              "enum"   -> SymphonyQLValue.StringValue("enum"),
              "float"  -> SymphonyQLValue.FloatValue(12.1),
              "int"    -> SymphonyQLValue.IntValue(12),
              "string" -> SymphonyQLValue.StringValue("string")
            )
          )
        )
      )
      val jsonValue = JsArray(
        JsNull,
        JsObject(
          "string" -> JsString("string"),
          "int"    -> JsNumber(12),
          "enum"   -> JsString("enum"),
          "float"  -> JsNumber(12.1)
        )
      )
      val json      = symphonyQLInputValueJsonFormat.write(value)
      val obj       = symphonyQLInputValueJsonFormat.read(jsonValue)

      // toString has a bug: fix me
      print(obj.toString)
      json -> obj.toInputString shouldEqual jsonValue -> value.toInputString
    }

    it("Deserialize SymphonyQLValue") {
      val value     = SymphonyQLValue.StringValue("string")
      val jsonValue = JsString("string")
      val json      = symphonyQLInputValueJsonFormat.write(value)
      val obj       = symphonyQLInputValueJsonFormat.read(jsonValue)

      json -> obj.toString shouldEqual jsonValue -> value.toString
    }
  }

}
