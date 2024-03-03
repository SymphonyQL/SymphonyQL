package symphony
package schema
package builder

import symphony.parser.SymphonyQLOutputValue
import symphony.parser.SymphonyQLValue.NullValue
import scala.jdk.FunctionConverters.*
import scala.jdk.OptionConverters.*

object ScalarBuilder {
  def newScalar[A](): ScalarBuilder[A] = new ScalarBuilder[A]
}

final class ScalarBuilder[A] {
  private var name: String                                           = _
  private var description: Option[String]                            = None
  private var toOutput: () => JavaFunction[A, SymphonyQLOutputValue] = () =>
    new JavaFunction[A, SymphonyQLOutputValue]() {
      override def apply(t: A): SymphonyQLOutputValue = NullValue
    }
  private var isNullable: Boolean                                    = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  def toOutput(toOutput: JavaFunction[A, SymphonyQLOutputValue]): this.type = {
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
