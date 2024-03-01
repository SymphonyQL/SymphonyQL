package symphony.schema.scaladsl.annotations

import scala.annotation.StaticAnnotation

/**
 * Annotation used to provide a description to a field or a type.
 */
case class GQLDescription(value: String) extends StaticAnnotation
