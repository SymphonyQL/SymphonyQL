package symphony
package schema
package javadsl

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*

import scala.annotation.varargs
import scala.jdk.FunctionConverters.*
import scala.jdk.OptionConverters.*

object ObjectBuilder {
  def newObject[A](): ObjectBuilder[A] = new ObjectBuilder[A]
}

final class ObjectBuilder[A] private {
  private var name: String                                                                = _
  private var description: Option[String]                                                 = None
  private var fields: List[(JavaFunction[FieldBuilder, __Field], JavaFunction[A, Stage])] = List.empty
  private var directives: List[Directive]                                                 = List.empty
  private var isNullable: Boolean                                                         = false
  private var hasArg                                                                      = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  def field(
    builder: JavaFunction[FieldBuilder, __Field]
  ): this.type = {
    hasArg = false
    this.fields = fields ::: List(builder -> new JavaFunction[A, Stage] {
      override def apply(t: A): Stage = Stage.createNull()
    })
    this
  }

  def fieldWithArg(
    builder: JavaFunction[FieldBuilder, __Field],
    stage: JavaFunction[A, Stage]
  ): this.type = {
    hasArg = true
    this.fields = fields ::: List(builder -> stage)
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
          _ => fields.map(kv => kv._1(FieldBuilder.newField().hasArgs(hasArg)) -> kv._2.asScala),
          directives
        )
      )
    else
      Schema.mkObject(
        name,
        description,
        _ => fields.map(kv => kv._1(FieldBuilder.newField().hasArgs(hasArg)) -> kv._2.asScala),
        directives
      )

}
