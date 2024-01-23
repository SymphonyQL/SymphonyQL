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

  lazy val outputValueRenderer: Renderer[OutputValue] =
    (value: OutputValue, indent: Option[Int], write: StringBuilder) =>
      value match {
        case OutputValue.ListValue(values) =>
          outputListValueRenderer.unsafeRender(OutputValue.ListValue(values), indent, write)
        case in: OutputValue.ObjectValue =>
          outputObjectValueRenderer.unsafeRender(in, indent, write)
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

  lazy val outputListValueRenderer: Renderer[OutputValue.ListValue] =
    Renderer.char('[') ++ outputValueRenderer
      .list(Renderer.char(',') ++ Renderer.spaceOrEmpty)
      .contramap[OutputValue.ListValue](_.values) ++ Renderer.char(']')

  lazy val outputObjectValueRenderer: Renderer[OutputValue.ObjectValue] =
    (value: OutputValue.ObjectValue, indent: Option[Int], write: StringBuilder) => {
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
        outputValueRenderer.unsafeRender(field._2, indent, write)
      }
      write += '}'
    }

}
