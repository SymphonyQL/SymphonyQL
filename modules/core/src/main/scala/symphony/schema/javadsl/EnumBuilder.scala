package symphony.schema.javadsl

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.annotation.varargs
import scala.jdk.OptionConverters.*
import scala.jdk.FunctionConverters.*

object EnumBuilder {
  def newEnum[A](): EnumBuilder[A] = new EnumBuilder[A]
}

final class EnumBuilder[A] private {
  private var name: String                         = _
  private var description: Option[String]          = None
  private var serialize: JavaFunction[A, String]   = _
  private var values: List[IntrospectionEnumValue] = List.empty
  private var directives: List[Directive]          = List.empty

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
    this
  }

  def serialize(serialize: JavaFunction[A, String]): this.type = {
    this.serialize = serialize
    this
  }

  @varargs
  def values(values: IntrospectionEnumValue*): this.type = {
    this.values = values.toList
    this
  }

  def value(value: IntrospectionEnumValue): this.type = {
    this.values = values ::: List(value)
    this
  }

  @varargs
  def directives(directives: Directive*): this.type = {
    this.directives = directives.toList
    this
  }

  def build(): Schema[A] =
    Schema.mkEnum(
      name,
      description,
      values,
      serialize.asScala,
      directives
    )
}
