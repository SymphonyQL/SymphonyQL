package symphony

import symphony.parser.SymphonyError
import symphony.parser.value.OutputValue
import symphony.parser.value.OutputValue.*
import symphony.parser.value.Value.StringValue

final case class SymphonyResponse[+E](data: OutputValue, errors: List[E], extensions: Option[ObjectValue] = None) {

  def toOutputValue: OutputValue =
    ObjectValue(
      List(
        "data" -> (if (errors.isEmpty) Some(data) else None),
        "errors" -> (if (errors.nonEmpty)
                       Some(ListValue(errors.map {
                         case e: SymphonyError => e.toOutputValue
                         case e                => ObjectValue(List("message" -> StringValue(e.toString)))
                       }))
                     else None),
        "extensions" -> extensions
      ).collect { case (name, Some(v)) => name -> v }
    )
}
