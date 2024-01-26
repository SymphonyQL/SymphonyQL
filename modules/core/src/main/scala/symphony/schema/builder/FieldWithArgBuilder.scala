package symphony
package schema
package builder

import symphony.parser.adt.Directive
import symphony.parser.introspection.*

object FieldWithArgBuilder {
  def builder(): FieldWithArgBuilder = new FieldWithArgBuilder
}

final class FieldWithArgBuilder private {
  private var name: String                      = _
  private var description: Option[String]       = None
  private var directives: List[Directive]       = List.empty
  private var schema: Schema[_]                 = _
  private var args: List[(String, Schema[_])]   = List.empty
  private var isDeprecated: Boolean             = false
  private var deprecationReason: Option[String] = None
  private var isOptional: Boolean               = false

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

  def args(args: (String, Schema[_])*): this.type = {
    this.args = args.toList
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

  def isOptional(isOptional: Boolean): this.type = {
    this.isOptional = isOptional
    this
  }

  def build(): __Field =
    Types.mkField(
      name,
      description,
      args
        .map((_name, schema) =>
          __InputValue(
            name = _name,
            description = description,
            `type` = () => if (schema.optional) schema.toType else Types.makeNonNull(schema.toType),
            defaultValue = None
          )
        ),
      () => if (isOptional) schema.toType else Types.makeNonNull(schema.toType),
      isDeprecated,
      deprecationReason,
      Option(directives)
    )

}
