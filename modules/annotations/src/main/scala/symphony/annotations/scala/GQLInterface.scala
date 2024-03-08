package symphony.annotations.scala

import scala.annotation.StaticAnnotation

/**
 * Annotation to make a sealed trait an interface instead of a union type or an enum.
 *
 * @param excludedFields Optionally provide a list of field names that should be excluded from the interface.
 */
case class GQLInterface(excludedFields: String*) extends StaticAnnotation
