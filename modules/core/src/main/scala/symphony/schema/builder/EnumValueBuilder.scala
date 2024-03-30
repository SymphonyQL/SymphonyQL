package symphony.schema.builder

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.annotation.*
import scala.jdk.OptionConverters.*

object EnumValueBuilder {
  def newEnumValue(): EnumValueBuilder = new EnumValueBuilder
}

final class EnumValueBuilder private {
  private var name: String                      = _
  private var description: Option[String]       = None
  private var isDeprecated: Boolean             = false
  private var deprecationReason: Option[String] = None
  private var directives: List[Directive]       = List.empty

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: String): this.type = {
    this.description = Option(description)
    this
  }

  def isDeprecated(isDeprecated: Boolean): this.type = {
    this.isDeprecated = isDeprecated
    this
  }

  def deprecationReason(deprecationReason: String): this.type = {
    this.deprecationReason = Option(deprecationReason)
    this
  }

  @varargs
  def directives(directives: Directive*): this.type = {
    this.directives = directives.toList
    this
  }

  def build(): __EnumValue = __EnumValue(
    name,
    description,
    isDeprecated,
    deprecationReason,
    Some(directives)
  )
}
