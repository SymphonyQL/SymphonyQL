package symphony
package schema
package builder

import symphony.parser.adt.Directive
import symphony.parser.introspection.*

object FieldBuilder {
  def builder(): FieldBuilder = new FieldBuilder
}

final class FieldBuilder private {
  private var name: String                      = _
  private var description: Option[String]       = None
  private var directives: List[Directive]       = List.empty
  private var schema: Schema[_]                 = _
  private var isDeprecated: Boolean             = false
  private var deprecationReason: Option[String] = None
  private var hasArgs: Boolean                  = false
  private var isInput: Boolean                  = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: Option[String]): this.type = {
    this.description = description
    this
  }

  def directives(directives: List[Directive]): this.type = {
    this.directives = directives
    this
  }

  def schema(schema: Schema[_]): this.type = {
    this.schema = schema
    this
  }

  def isDeprecated(isDeprecated: Boolean): this.type = {
    this.isDeprecated = isDeprecated
    this
  }

  def deprecationReason(deprecationReason: Option[String]): this.type = {
    this.deprecationReason = deprecationReason
    this
  }

  def hasArgs(isInputArgs: Boolean): this.type = {
    this.hasArgs = isInputArgs
    this
  }

  def isInput(isInput: Boolean): this.type = {
    this.isInput = isInput
    this
  }
  
  def build(): __Field =
    Types.mkField(
      name,
      description,
      if (hasArgs) schema.arguments else List.empty,
      () => if (schema.optional) schema.toType(isInput) else Types.mkNonNull(schema.toType(isInput)),
      isDeprecated,
      deprecationReason,
      Option(directives)
    )

}
