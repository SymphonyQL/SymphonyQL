package symphony.schema

import symphony.parser.SymphonyError.*
import symphony.parser.value.InputValue

trait ArgumentExtractor[T] {
  def extract(input: InputValue): Either[ArgumentError, T]
}
