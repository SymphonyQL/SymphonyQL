package symphony
package parser

import value.*
import value.InputValue
import value.Value.*
import value.Value.FloatValue.*
import value.Value.IntValue.*

object ValueRenderer {

  lazy val inputValueRenderer: Renderer[InputValue] = (value: InputValue, indent: Option[Int], write: StringBuilder) =>
    value match {
      case in: InputValue.ListValue   => inputListValueRenderer.unsafeRender(in, indent, write)
      case in: InputValue.ObjectValue => inputObjectValueRenderer.unsafeRender(in, indent, write)
      case InputValue.VariableValue(name) =>
        write += '$'
        write ++= name
      case StringValue(str) =>
        write += '"'
        Renderer.escapedString.unsafeRender(str, indent, write)
        write += '"'
      case Value.EnumValue(value)    => Renderer.escapedString.unsafeRender(value, indent, write)
      case Value.BooleanValue(value) => write append value
      case Value.NullValue           => write ++= "null"
      case IntNumber(value)          => write append value
      case LongNumber(value)         => write append value
      case BigIntNumber(value)       => write append value
      case FloatNumber(value)        => write append value
      case DoubleNumber(value)       => write append value
      case BigDecimalNumber(value)   => write append value
    }

  lazy val inputObjectValueRenderer: Renderer[InputValue.ObjectValue] =
    Renderer.char('{') ++ Renderer
      .map(
        Renderer.string,
        inputValueRenderer,
        Renderer.char(',') ++ Renderer.spaceOrEmpty,
        Renderer.char(':') ++ Renderer.spaceOrEmpty
      )
      .contramap[InputValue.ObjectValue](_.fields) ++ Renderer.char('}')

  lazy val inputListValueRenderer: Renderer[InputValue.ListValue] =
    Renderer.char('[') ++ inputValueRenderer
      .list(Renderer.char(',') ++ Renderer.spaceOrEmpty)
      .contramap[InputValue.ListValue](_.values) ++ Renderer.char(']')

  lazy val enumInputValueRenderer: Renderer[Value.EnumValue] =
    (value: Value.EnumValue, indent: Option[Int], write: StringBuilder) =>
      Renderer.escapedString.unsafeRender(value.value, indent, write)

  lazy val responseValueRenderer: Renderer[ResponseValue] =
    (value: ResponseValue, indent: Option[Int], write: StringBuilder) =>
      value match {
        case ResponseValue.ListValue(values) =>
          responseListValueRenderer.unsafeRender(ResponseValue.ListValue(values), indent, write)
        case in: ResponseValue.ObjectValue =>
          responseObjectValueRenderer.unsafeRender(in, indent, write)
        case StringValue(str) =>
          write += '"'
          Renderer.escapedString.unsafeRender(str, indent, write)
          write += '"'
        case Value.EnumValue(value) =>
          write += '"'
          Renderer.escapedString.unsafeRender(value, indent, write)
          write += '"'
        case Value.BooleanValue(value) => write append value
        case Value.NullValue           => write append "null"
        case IntNumber(value)          => write append value
        case LongNumber(value)         => write append value
        case FloatNumber(value)        => write append value
        case DoubleNumber(value)       => write append value
        case BigDecimalNumber(value)   => write append value
        case BigIntNumber(value)       => write append value
      }

  lazy val responseListValueRenderer: Renderer[ResponseValue.ListValue] =
    Renderer.char('[') ++ responseValueRenderer
      .list(Renderer.char(',') ++ Renderer.spaceOrEmpty)
      .contramap[ResponseValue.ListValue](_.values) ++ Renderer.char(']')

  lazy val responseObjectValueRenderer: Renderer[ResponseValue.ObjectValue] =
    (value: ResponseValue.ObjectValue, indent: Option[Int], write: StringBuilder) => {
      write += '{'
      var first = true
      value.fields.foreach { field =>
        if (first) first = false
        else {
          write += ','
          if (indent.nonEmpty) write += ' '
        }
        write += '"'
        write ++= field._1
        write += '"'
        write += ':'
        if (indent.nonEmpty) write += ' '
        responseValueRenderer.unsafeRender(field._2, indent, write)
      }
      write += '}'
    }

}
