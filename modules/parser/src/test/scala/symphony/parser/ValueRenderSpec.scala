package symphony.parser

import org.scalactic.Explicitly.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.parser.*
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.OperationType.{ Mutation, Query }
import symphony.parser.adt.Selection.*
import symphony.parser.adt.Type.*
import org.apache.pekko.stream.scaladsl.Source

class ValueRenderSpec extends AnyFunSpec with Matchers {

  describe("ValueRender SymphonyQLOutputValue Spec") {
    it("SymphonyQLOutputValue ObjectValue toString") {
      val outputValue =
        SymphonyQLOutputValue.ObjectValue(List("name" -> SymphonyQLValue.StringValue("string"))).toString
      outputValue shouldEqual """{"name":"string"}"""
    }

    it("SymphonyQLOutputValue ObjectValue equals") {
      val outputValue1 =
        SymphonyQLOutputValue.ObjectValue(List("name" -> SymphonyQLValue.StringValue("string"))).toString
      val outputValue2 =
        SymphonyQLOutputValue.ObjectValue(List("name" -> SymphonyQLValue.StringValue("string"))).toString
      outputValue1 shouldEqual outputValue2
    }

    it("SymphonyQLOutputValue ListValue toString") {
      val outputValue = SymphonyQLOutputValue.ListValue(List(SymphonyQLValue.StringValue("string"))).toString
      outputValue shouldEqual """["string"]"""
    }

    it("SymphonyQLOutputValue StreamValue toString") {
      val outputValue = SymphonyQLOutputValue.StreamValue(Source.single(SymphonyQLValue.StringValue("string"))).toString
      println(outputValue)
      outputValue shouldEqual """<stream>"""
    }
  }

  describe("ValueRender SymphonyQLInputValue Spec") {
    it("SymphonyQLInputValue ObjectValue toInputString") {
      val inputValue =
        SymphonyQLInputValue.ObjectValue(Map("name" -> SymphonyQLValue.StringValue("string"))).toInputString
      inputValue shouldEqual """{name: "string"}"""
    }

    it("SymphonyQLInputValue ListValue toInputString") {
      val inputValue = SymphonyQLInputValue.ListValue(List(SymphonyQLValue.StringValue("string"))).toInputString
      inputValue shouldEqual """["string"]"""
    }

    it("SymphonyQLInputValue StreamValue toString") {
      val inputValue = SymphonyQLInputValue.VariableValue("name").toInputString
      inputValue shouldEqual """$name"""
    }
  }
}
