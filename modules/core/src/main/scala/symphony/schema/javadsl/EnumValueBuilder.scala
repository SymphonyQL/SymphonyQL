package symphony.schema.javadsl

import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*
import symphony.schema.*

import scala.annotation.varargs
import scala.jdk.OptionConverters.*

object EnumValueBuilder {
  def newEnumValue(): EnumValueBuilder = new EnumValueBuilder
}

final class EnumValueBuilder private {
  private var name: String                        = _
  private var description: Option[String]         = None
  private var isDeprecated: Boolean               = false
  private var deprecationReason: Option[String]   = None
  private var directives: Option[List[Directive]] = None

  def name(name: String): this.type = {
    this.name = name
    this
  }

  def description(description: java.util.Optional[String]): this.type = {
    this.description = description.toScala
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

  @varargs
  def directives(directives: Directive*): this.type = {
    this.directives = Option(directives.toList)
    this
  }

  def build(): __EnumValue = __EnumValue(
    name,
    description,
    isDeprecated,
    deprecationReason,
    directives
  )
}
