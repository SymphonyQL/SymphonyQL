package symphony.annotations.scala

import scala.annotation.StaticAnnotation

/**
 * Annotation to make a sealed trait a union instead of an enum.
 */
case class GQLUnion() extends StaticAnnotation
