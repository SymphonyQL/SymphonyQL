package symphony
package schema
package javadsl

import symphony.parser.SymphonyQLOutputValue
import symphony.parser.SymphonyQLValue.NullValue
import scala.jdk.FunctionConverters.*
import scala.jdk.OptionConverters.*

object ScalarBuilder {
  def newScalar[A](): ScalarBuilder[A] = new ScalarBuilder[A]
}

final class ScalarBuilder[A] {
  private var name: String                                                          = _
  private var description: Option[String]                                           = None
  private var toOutput: () => java.util.function.Function[A, SymphonyQLOutputValue] = () =>
    new java.util.function.Function[A, SymphonyQLOutputValue]() {
      override def apply(t: A): SymphonyQLOutputValue = NullValue
    }
  private var isNullable: Boolean                                                   = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  def toOutput(toOutput: java.util.function.Function[A, SymphonyQLOutputValue]): this.type = {
    this.toOutput = () => toOutput
    this
  }

  def isNullable(isOptional: Boolean): this.type = {
    this.isNullable = isOptional
    this
  }

  def build(): Schema[A] =
    if (isNullable) Schema.createNullable(Schema.mkScalar(name, description, toOutput.apply().asScala))
    else Schema.mkScalar(name, description, toOutput.apply().asScala)

}
