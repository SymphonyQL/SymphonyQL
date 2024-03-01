package symphony.schema.scaladsl.annotations
import scala.annotation.StaticAnnotation

/**
 * Annotation to specify the default value of an input field.
 */
case class GQLDefault(value: String) extends StaticAnnotation
