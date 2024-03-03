package symphony.annotations.scala

import scala.annotation.StaticAnnotation

/**
 * Annotation used to provide an alternative name to a field or a type.
 */
case class GQLName(value: String) extends StaticAnnotation
