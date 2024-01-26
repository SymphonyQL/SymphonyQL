package symphony
package schema
package builder

import symphony.parser.adt.Directive
import symphony.parser.introspection.*

object ObjectBuilder {
  def builder[A](): ObjectBuilder[A] = new ObjectBuilder[A]
}

final class ObjectBuilder[A] private {
  private var name: String                                                              = _
  private var description: Option[String]                                               = None
  private var fieldWithArgs: List[FieldWithArgBuilder => (__Field, A => ExecutionPlan)] = List.empty
  private var directives: List[Directive]                                               = List.empty
  private var isOptional: Boolean                                                       = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: Option[String]): this.type = {
    this.description = description
    this
  }

  def fieldWithArgs(fields: (FieldWithArgBuilder => (__Field, A => ExecutionPlan))*): this.type = {
    this.fieldWithArgs = fields.toList
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

  def build(): Schema[A] =
    Schema.mkObject(
      name,
      description,
      isOptional,
      fieldWithArgs.map(_.apply(FieldWithArgBuilder.builder())),
      directives
    )

}
