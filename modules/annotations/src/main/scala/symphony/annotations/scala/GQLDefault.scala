package symphony.annotations.scala

import scala.annotation.StaticAnnotation

/**
 * Annotation to specify the default value of an input field.
 */
case class GQLDefault(value: String) extends StaticAnnotation
