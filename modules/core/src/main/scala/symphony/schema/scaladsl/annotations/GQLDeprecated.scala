package symphony.schema.scaladsl.annotations

import scala.annotation.StaticAnnotation

/**
 * Annotation used to indicate a type or a field is deprecated.
 */
case class GQLDeprecated(reason: String) extends StaticAnnotation
