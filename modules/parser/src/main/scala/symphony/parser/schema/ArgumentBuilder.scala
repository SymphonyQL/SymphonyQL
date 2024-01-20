package symphony.parser.schema

import symphony.parser.SymphonyError.ExecutionError
import symphony.parser.value.InputValue

trait ArgumentBuilder[T] {

  def build(input: InputValue): Either[ExecutionError, T]

}
