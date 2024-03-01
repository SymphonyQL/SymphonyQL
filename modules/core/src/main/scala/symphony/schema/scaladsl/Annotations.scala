package symphony.schema.scaladsl

import symphony.parser.adt.Directive

import scala.annotation.StaticAnnotation

object Annotations {

  /**
   * Annotation used to indicate a type or a field is deprecated.
   */
  case class GQLDeprecated(reason: String) extends StaticAnnotation

  /**
   * Annotation used to provide a description to a field or a type.
   */
  case class GQLDescription(value: String) extends StaticAnnotation

  /**
   * Annotation used to exclude a field from a type.
   */
  case class GQLExcluded() extends StaticAnnotation

  /**
   * Annotation used to customize the name of an input type.
   * This is usually needed to avoid a name clash when a type is used both as an input and an output.
   */
  case class GQLInputName(name: String) extends StaticAnnotation

  /**
   * Annotation used to provide an alternative name to a field or a type.
   */
  case class GQLName(value: String) extends StaticAnnotation

  /**
   * Annotation to make a sealed trait an interface instead of a union type or an enum
   *
   * @param excludeFields Optionally provide a list of field names that should be excluded from the interface
   */
  case class GQLInterface(excludedFields: String*) extends StaticAnnotation

  /**
   * Annotation to make a sealed trait a union instead of an enum.
   */
  case class GQLUnion() extends StaticAnnotation

  /**
   * Annotation to specify the default value of an input field.
   */
  case class GQLDefault(value: String) extends StaticAnnotation
}
