package symphony.schema.scaladsl.annotations

import scala.annotation.StaticAnnotation

/**
 * Annotation used to exclude a field from a type.
 */
case class GQLExcluded() extends StaticAnnotation
