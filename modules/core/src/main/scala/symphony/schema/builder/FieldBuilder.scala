package symphony
package schema
package builder

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*

import scala.annotation.*
import scala.jdk.OptionConverters.*

object FieldBuilder {
  def newField(): FieldBuilder = new FieldBuilder
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

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  @varargs
  def directives(directives: Directive*): this.type = {
    this.directives = directives.toList
    this
  }

  protected[symphony] def getSchema[V]: Schema[V] = this.schema.asInstanceOf[Schema[V]]

  def schema[V](schema: Schema[V]): this.type = {
    this.schema = schema
    this
  }

  def isDeprecated(isDeprecated: Boolean): this.type = {
    this.isDeprecated = isDeprecated
    this
  }

  def deprecationReason(deprecationReason: java.util.Optional[String]): this.type = {
    this.deprecationReason = deprecationReason.toScala
    this
  }

  def hasArgs(hasArgs: Boolean): this.type = {
    this.hasArgs = hasArgs
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
      () => if (schema.optional) schema.lazyType(isInput) else Types.mkNonNull(schema.lazyType(isInput)),
      isDeprecated,
      deprecationReason,
      Some(directives)
    )

}
