package symphony
package parser

import scala.util.control.NonFatal
import scala.util.hashing.MurmurHash3

sealed trait OutputValue extends Serializable

object OutputValue {

  final case class ListValue(values: List[OutputValue]) extends OutputValue {
    override def toString: String = ValueRenderer.outputListValueRenderer.renderCompact(this)
  }

  final case class ObjectValue(fields: List[(String, OutputValue)]) extends OutputValue {
    override def toString: String = ValueRenderer.outputObjectValueRenderer.renderCompact(this)

    @transient override lazy val hashCode: Int = MurmurHash3.unorderedHash(fields)

    override def equals(other: Any): Boolean =
      other match {
        case o: ObjectValue => o.hashCode == hashCode
        case _              => false
      }
  }
}

sealed trait InputValue extends Serializable {
  self =>
  def toInputString: String = ValueRenderer.inputValueRenderer.renderCompact(self)
}

object InputValue {

  final case class ListValue(values: List[InputValue]) extends InputValue {
    override def toString: String = values.mkString("[", ",", "]")

    override def toInputString: String = ValueRenderer.inputListValueRenderer.render(this)
  }

  final case class ObjectValue(fields: Map[String, InputValue]) extends InputValue {

    override def toString: String =
      fields.map { case (name, value) => s""""$name:${value.toString}"""" }.mkString("{", ",", "}")

    override def toInputString: String = ValueRenderer.inputObjectValueRenderer.render(this)
  }

  final case class VariableValue(name: String) extends InputValue {
    override def toString: String = s"$$$name"
  }

}

sealed trait PathValue extends Value

object PathValue {
  def fromEither(either: Either[String, Int]): PathValue = either.fold(Key.apply, Index.apply)

  object Key {
    def apply(value: String): PathValue = Value.StringValue(value)

    def unapply(value: PathValue): Option[String] = value match {
      case Value.StringValue(s) => Some(s)
      case _                    => None
    }
  }

  object Index {
    def apply(value: Int): PathValue = Value.IntValue.IntNumber(value)

    def unapply(value: PathValue): Option[Int] = value match {
      case Value.IntValue.IntNumber(i) => Some(i)
      case _                           => None
    }
  }
}

sealed trait Value extends InputValue with OutputValue

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
        // Should never happen, but we leave it as a fallback
        case NonFatal(_) => BigIntNumber(BigInt(s))
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
