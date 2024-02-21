package symphony.schema.javadsl

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.annotation.varargs
import scala.jdk.OptionConverters.*

object InputObjectBuilder {
  def newObject[A](): InputObjectBuilder[A] = new InputObjectBuilder[A]
}

final class InputObjectBuilder[A] private {
  private var name: String                                      = _
  private var description: Option[String]                       = None
  private var fields: List[JavaFunction[FieldBuilder, __Field]] = List.empty
  private var directives: List[Directive]                       = List.empty
  private var isNullable: Boolean                               = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  def field(builder: JavaFunction[FieldBuilder, __Field]): this.type = {
    this.fields = fields ::: List(builder)
    this
  }

  @varargs
  def fields(builder: JavaFunction[FieldBuilder, __Field]*): this.type = {
    this.fields = builder.toList
    this
  }

  @varargs
  def directives(directives: Directive*): this.type = {
    this.directives = directives.toList
    this
  }

  def isNullable(isNullable: Boolean): this.type = {
    this.isNullable = isNullable
    this
  }

  def build(): Schema[A] =
    if (isNullable)
      Schema.createNullable(
        Schema.mkObject(
          name,
          description,
          * => fields.map(_.apply(FieldBuilder.newField())).map(f => f -> (* => Stage.NullStage)),
          directives
        )
      )
    else
      Schema.mkObject(
        name,
        description,
        * => fields.map(_.apply(FieldBuilder.newField())).map(f => f -> (* => Stage.NullStage)),
        directives
      )

}
