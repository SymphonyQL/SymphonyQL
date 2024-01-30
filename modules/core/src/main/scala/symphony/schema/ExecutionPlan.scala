package symphony.schema

import symphony.parser.OutputValue
import symphony.parser.Value.NullValue

trait ExecutionPlan

object ExecutionPlan {

  val NullPlan: PureDataPlan = PureDataPlan(NullValue)

  case class ListDataPlan(plan: List[ExecutionPlan]) extends ExecutionPlan

  case class ObjectDataPlan(name: String, fields: Map[String, ExecutionPlan]) extends ExecutionPlan

  case class PureDataPlan(value: OutputValue) extends ExecutionPlan
}
