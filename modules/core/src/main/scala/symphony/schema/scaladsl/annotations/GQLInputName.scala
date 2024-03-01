package symphony.schema.scaladsl.annotations

import scala.annotation.StaticAnnotation

/**
 * Annotation used to customize the name of an input type.
 * This is usually needed to avoid a name clash when a type is used both as an input and an output.
 */
case class GQLInputName(name: String) extends StaticAnnotation
