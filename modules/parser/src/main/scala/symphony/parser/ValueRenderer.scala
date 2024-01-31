package symphony
package parser

import SymphonyQLValue.*
import SymphonyQLValue.FloatValue.*
import SymphonyQLValue.IntValue.*

object ValueRenderer {

  lazy val inputValueRenderer: SymphonyQLRenderer[SymphonyQLInputValue] =
    (value: SymphonyQLInputValue, indent: Option[Int], write: StringBuilder) =>
      value match {
        case in: SymphonyQLInputValue.ListValue   => inputListValueRenderer.unsafeRender(in, indent, write)
        case in: SymphonyQLInputValue.ObjectValue => inputObjectValueRenderer.unsafeRender(in, indent, write)
        case SymphonyQLInputValue.VariableValue(name) =>
          write += '$'
          write ++= name
        case StringValue(str) =>
          write += '"'
          SymphonyQLRenderer.escapedString.unsafeRender(str, indent, write)
          write += '"'
        case SymphonyQLValue.EnumValue(value)    => SymphonyQLRenderer.escapedString.unsafeRender(value, indent, write)
        case SymphonyQLValue.BooleanValue(value) => write append value
        case SymphonyQLValue.NullValue           => write ++= "null"
        case IntNumber(value)                    => write append value
        case LongNumber(value)                   => write append value
        case BigIntNumber(value)                 => write append value
        case FloatNumber(value)                  => write append value
        case DoubleNumber(value)                 => write append value
        case BigDecimalNumber(value)             => write append value
      }

  lazy val inputObjectValueRenderer: SymphonyQLRenderer[SymphonyQLInputValue.ObjectValue] =
    SymphonyQLRenderer.char('{') ++ SymphonyQLRenderer
      .map(
        SymphonyQLRenderer.string,
        inputValueRenderer,
        SymphonyQLRenderer.char(',') ++ SymphonyQLRenderer.spaceOrEmpty,
        SymphonyQLRenderer.char(':') ++ SymphonyQLRenderer.spaceOrEmpty
      )
      .contramap[SymphonyQLInputValue.ObjectValue](_.fields) ++ SymphonyQLRenderer.char('}')

  lazy val inputListValueRenderer: SymphonyQLRenderer[SymphonyQLInputValue.ListValue] =
    SymphonyQLRenderer.char('[') ++ inputValueRenderer
      .list(SymphonyQLRenderer.char(',') ++ SymphonyQLRenderer.spaceOrEmpty)
      .contramap[SymphonyQLInputValue.ListValue](_.values) ++ SymphonyQLRenderer.char(']')

  lazy val enumInputValueRenderer: SymphonyQLRenderer[SymphonyQLValue.EnumValue] =
    (value: SymphonyQLValue.EnumValue, indent: Option[Int], write: StringBuilder) =>
      SymphonyQLRenderer.escapedString.unsafeRender(value.value, indent, write)

  lazy val outputValueRenderer: SymphonyQLRenderer[SymphonyQLOutputValue] =
    (value: SymphonyQLOutputValue, indent: Option[Int], write: StringBuilder) =>
      value match {
        case SymphonyQLOutputValue.ListValue(values) =>
          outputListValueRenderer.unsafeRender(SymphonyQLOutputValue.ListValue(values), indent, write)
        case in: SymphonyQLOutputValue.ObjectValue =>
          outputObjectValueRenderer.unsafeRender(in, indent, write)
        case StringValue(str) =>
          write += '"'
          SymphonyQLRenderer.escapedString.unsafeRender(str, indent, write)
          write += '"'
        case SymphonyQLValue.EnumValue(value) =>
          write += '"'
          SymphonyQLRenderer.escapedString.unsafeRender(value, indent, write)
          write += '"'
        case SymphonyQLValue.BooleanValue(value) => write append value
        case SymphonyQLValue.NullValue           => write append "null"
        case IntNumber(value)                    => write append value
        case LongNumber(value)                   => write append value
        case FloatNumber(value)                  => write append value
        case DoubleNumber(value)                 => write append value
        case BigDecimalNumber(value)             => write append value
        case BigIntNumber(value)                 => write append value
      }

  lazy val outputListValueRenderer: SymphonyQLRenderer[SymphonyQLOutputValue.ListValue] =
    SymphonyQLRenderer.char('[') ++ outputValueRenderer
      .list(SymphonyQLRenderer.char(',') ++ SymphonyQLRenderer.spaceOrEmpty)
      .contramap[SymphonyQLOutputValue.ListValue](_.values) ++ SymphonyQLRenderer.char(']')

  lazy val outputObjectValueRenderer: SymphonyQLRenderer[SymphonyQLOutputValue.ObjectValue] =
    (value: SymphonyQLOutputValue.ObjectValue, indent: Option[Int], write: StringBuilder) => {
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
