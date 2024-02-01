package symphony

import symphony.parser.SymphonyQLError
import symphony.parser.SymphonyQLOutputValue
import symphony.parser.SymphonyQLOutputValue.*
import symphony.parser.SymphonyQLValue.StringValue

final case class SymphonyQLResponse[+E](
  data: SymphonyQLOutputValue,
  errors: List[E],
  extensions: Option[ObjectValue] = None
) {

  def toOutputValue: SymphonyQLOutputValue =
    ObjectValue(
      List(
        "data"       -> (if (errors.isEmpty) Some(data) else None),
        "errors"     -> (if (errors.nonEmpty)
                       Some(ListValue(errors.map {
                         case e: SymphonyQLError => e.toOutputValue
                         case e                  => ObjectValue(List("message" -> StringValue(e.toString)))
                       }))
                     else None),
        "extensions" -> extensions
      ).collect { case (name, Some(v)) => name -> v }
    )
}
