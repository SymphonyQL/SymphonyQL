package symphony
package parser
package value

import scala.util.control.NonFatal

private[symphony] trait Value extends InputValue with ResponseValue

object Value {

  case object NullValue extends Value {
    override def toString: String = "null"
  }

  sealed trait IntValue extends Value {
    def toInt: Int
    def toLong: Long
    def toBigInt: BigInt
  }

  sealed trait FloatValue extends Value {
    def toFloat: Float
    def toDouble: Double
    def toBigDecimal: BigDecimal
  }

  final case class StringValue(value: String) extends Value with PathValue {
    override def toString: String = s""""${value.replace("\"", "\\\"").replace("\n", "\\n")}""""
  }

  final case class BooleanValue(value: Boolean) extends Value {
    override def toString: String = if (value) "true" else "false"
  }

  final case class EnumValue(value: String) extends Value {
    override def toString: String      = s""""${value.replace("\"", "\\\"")}""""
    override def toInputString: String = ValueRenderer.enumInputValueRenderer.render(this)
  }

  object IntValue {
    def apply(v: Int): IntValue    = IntNumber(v)
    def apply(v: Long): IntValue   = LongNumber(v)
    def apply(v: BigInt): IntValue = BigIntNumber(v)

    @throws[NumberFormatException]("if the string is not a valid representation of an integer")
    def fromStringUnsafe(s: String): IntValue =
      try {
        val mod  = if (s.charAt(0) == '-') 1 else 0
        val size = s.length - mod
        if (size < 10) IntNumber(s.toInt)
        else if (size < 19) LongNumber(s.toLong)
        else BigIntNumber(BigInt(s))
      } catch {
        case NonFatal(_) => BigIntNumber(BigInt(s)) // Should never happen, but we leave it as a fallback
      }

    final case class IntNumber(value: Int) extends IntValue with PathValue {
      override def toInt: Int       = value
      override def toLong: Long     = value.toLong
      override def toBigInt: BigInt = BigInt(value)
      override def toString: String = value.toString
    }

    final case class LongNumber(value: Long) extends IntValue {
      override def toInt: Int       = value.toInt
      override def toLong: Long     = value
      override def toBigInt: BigInt = BigInt(value)
      override def toString: String = value.toString
    }

    final case class BigIntNumber(value: BigInt) extends IntValue {
      override def toInt: Int       = value.toInt
      override def toLong: Long     = value.toLong
      override def toBigInt: BigInt = value
      override def toString: String = value.toString
    }
  }

  object FloatValue {
    def apply(v: Float): FloatValue      = FloatNumber(v)
    def apply(v: Double): FloatValue     = DoubleNumber(v)
    def apply(v: BigDecimal): FloatValue = BigDecimalNumber(v)
    def apply(s: String): FloatValue     = BigDecimalNumber(BigDecimal(s))

    final case class FloatNumber(value: Float) extends FloatValue {
      override def toFloat: Float           = value
      override def toDouble: Double         = value.toDouble
      override def toBigDecimal: BigDecimal = BigDecimal.decimal(value)
      override def toString: String         = value.toString
    }

    final case class DoubleNumber(value: Double) extends FloatValue {
      override def toFloat: Float           = value.toFloat
      override def toDouble: Double         = value
      override def toBigDecimal: BigDecimal = BigDecimal(value)
      override def toString: String         = value.toString
    }

    final case class BigDecimalNumber(value: BigDecimal) extends FloatValue {
      override def toFloat: Float           = value.toFloat
      override def toDouble: Double         = value.toDouble
      override def toBigDecimal: BigDecimal = value
      override def toString: String         = value.toString
    }
  }
}
