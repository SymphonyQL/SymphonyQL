package symphony
package schema
package builder

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*

import scala.annotation.varargs
import scala.jdk.OptionConverters.*

object ObjectBuilder {
  def newObject[A](): ObjectBuilder[A] = new ObjectBuilder[A]
}

final class ObjectBuilder[A] private {
  private var name: String                                                                   = _
  private var description: Option[String]                                                    = None
  private var fieldWithArgs: List[(JavaFunction[FieldBuilder, __Field], JavaFunction[A, ?])] = List.empty
  private var fields: List[(JavaFunction[FieldBuilder, __Field], JavaFunction[A, ?])]        = List.empty
  private var directives: List[Directive]                                                    = List.empty
  private var isNullable: Boolean                                                            = false

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  def field[V](
    builder: JavaFunction[FieldBuilder, __Field],
    fieldValue: JavaFunction[A, V]
  ): this.type = {
    this.fields = (builder -> fieldValue) :: fields
    this
  }

  def fieldWithArg[V](
    builder: JavaFunction[FieldBuilder, __Field],
    fieldValue: JavaFunction[A, V]
  ): this.type = {
    this.fieldWithArgs = (builder -> fieldValue) :: fieldWithArgs
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
          _ =>
            fieldWithArgs.reverse.map { kv =>
              val builder = FieldBuilder.newField().hasArgs(true)
              kv._1(builder) -> new Function[A, Stage]() {
                override def apply(v1: A): Stage =
                  builder.getSchema.analyze(kv._2.apply(v1))
              }

            } ++
              fields.reverse.map { kv =>
                val builder = FieldBuilder.newField().hasArgs(false)
                kv._1(builder) -> new Function[A, Stage]() {
                  override def apply(v1: A): Stage =
                    builder.getSchema.analyze(kv._2.apply(v1))
                }
              },
          directives
        )
      )
    else
      Schema.mkObject(
        name,
        description,
        _ =>
          fieldWithArgs.reverse.map { kv =>
            val builder = FieldBuilder.newField().hasArgs(true)
            kv._1(builder) -> new Function[A, Stage]() {
              override def apply(v1: A): Stage =
                builder.getSchema.analyze(kv._2.apply(v1))
            }

          } ++
            fields.reverse.map { kv =>
              val builder = FieldBuilder.newField().hasArgs(false)
              kv._1(builder) -> new Function[A, Stage]() {
                override def apply(v1: A): Stage =
                  builder.getSchema.analyze(kv._2.apply(v1))
              }
            },
        directives
      )

}
