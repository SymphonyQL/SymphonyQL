package symphony
package schema
package builder

import symphony.parser.SymphonyQLOutputValue
import symphony.parser.SymphonyQLValue.NullValue

object ScalarBuilder {
  def builder[A](): ScalarBuilder[A] = new ScalarBuilder[A]
}

final class ScalarBuilder[A] {
  private var name: String                         = _
  private var description: Option[String]          = None
  private var toOutput: A => SymphonyQLOutputValue = _ => NullValue
  private var isNullable: Boolean                  = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: Option[String]): this.type = {
    this.description = description
    this
  }

  def toOutput(toOutput: A => SymphonyQLOutputValue): this.type = {
    this.toOutput = toOutput
    this
  }

  def isNullable(isOptional: Boolean): this.type = {
    this.isNullable = isOptional
    this
  }

  def build(): Schema[A] =
    if (isNullable) Schema.mkNullable(Schema.mkScalar(name, description, toOutput))
    else Schema.mkScalar(name, description, toOutput)

}
