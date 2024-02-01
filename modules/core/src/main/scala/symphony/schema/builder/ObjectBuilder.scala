package symphony
package schema
package builder

import symphony.parser.adt.Directive
import symphony.parser.introspection.*

object ObjectBuilder {
  def builder[A](): ObjectBuilder[A] = new ObjectBuilder[A]
}

final class ObjectBuilder[A] private {
  private var name: String                                        = _
  private var description: Option[String]                         = None
  private var fields: List[FieldBuilder => (__Field, A => Stage)] = List.empty
  private var directives: List[Directive]                         = List.empty
  private var isNullable: Boolean                                 = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: Option[String]): this.type = {
    this.description = description
    this
  }

  def fields(fields: (FieldBuilder => (__Field, A => Stage))*): this.type = {
    this.fields = fields.toList
    this
  }

  def directives(directives: List[Directive]): this.type = {
    this.directives = directives
    this
  }

  def isNullable(isNullable: Boolean): this.type = {
    this.isNullable = isNullable
    this
  }

  def build(): Schema[A] =
    if (isNullable)
      Schema.mkNullable(
        Schema.mkObject(
          name,
          description,
          _ => fields.map(_.apply(FieldBuilder.builder())),
          directives
        )
      )
    else
      Schema.mkObject(
        name,
        description,
        _ => fields.map(_.apply(FieldBuilder.builder())),
        directives
      )

}
