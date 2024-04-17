package symphony.execution

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import symphony.parser.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.*

import scala.collection.mutable.ListBuffer

final case class ExecutionOutputValue(
  data: Source[SymphonyQLOutputValue, NotUsed],
  errors: ListBuffer[SymphonyQLError]
)
