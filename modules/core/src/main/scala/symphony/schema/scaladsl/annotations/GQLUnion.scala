package symphony.schema.scaladsl.annotations

import scala.annotation.StaticAnnotation

/**
 * Annotation to make a sealed trait a union instead of an enum.
 */
case class GQLUnion() extends StaticAnnotation
