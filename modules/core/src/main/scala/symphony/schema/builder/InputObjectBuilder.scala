package symphony.schema.builder

import symphony.parser.adt.Directive
import symphony.parser.introspection.*
import symphony.schema.Schema

object InputObjectBuilder {
  def builder[A](): InputObjectBuilder[A] = new InputObjectBuilder[A]
}

final class InputObjectBuilder[A] private {
  private var name: String                          = _
  private var description: Option[String]           = None
  private var fields: List[FieldBuilder => __Field] = List.empty
  private var directives: List[Directive]           = List.empty
  private var isOptional: Boolean                   = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: Option[String]): this.type = {
    this.description = description
    this
  }

  def fields(fields: FieldBuilder => __Field*): this.type = {
    this.fields = fields.toList
    this
  }

  def directives(directives: List[Directive]): this.type = {
    this.directives = directives
    this
  }

  def isOptional(isOptional: Boolean): this.type = {
    this.isOptional = isOptional
    this
  }

  def build(): Schema[A] = Schema.mkInputObject(
    name,
    description,
    isOptional,
    fields.map(_.apply(FieldBuilder.builder())),
    directives
  )

}
