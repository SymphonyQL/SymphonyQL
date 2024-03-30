package symphony.schema.builder

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.annotation.*
import scala.jdk.OptionConverters.*
import scala.jdk.FunctionConverters.*

object EnumBuilder {
  def newEnum[A](): EnumBuilder[A] = new EnumBuilder[A]
}

final class EnumBuilder[A] private {
  private var name: String                                              = _
  private var description: Option[String]                               = None
  private var serialize: JavaFunction[A, String]                        = (t: A) => t.toString
  private var values: List[JavaFunction[EnumValueBuilder, __EnumValue]] = List.empty
  private var directives: List[Directive]                               = List.empty
  private var origin: Option[String]                                    = None

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: String): this.type = {
    this.description = Option(description)
    this
  }

  def serialize(serialize: JavaFunction[A, String]): this.type = {
    this.serialize = serialize
    this
  }

  def value(builder: JavaFunction[EnumValueBuilder, __EnumValue]): this.type = {
    this.values = builder :: values
    this
  }

  @varargs
  def directives(directives: Directive*): this.type = {
    this.directives = directives.toList
    this
  }

  def origin(origin: String): this.type = {
    this.origin = Option(origin)
    this
  }

  def build(): Schema[A] =
    Schema.mkEnum(
      name,
      description,
      values.reverse.map(_.apply(EnumValueBuilder.newEnumValue())),
      origin,
      serialize.asScala,
      directives
    )
}
