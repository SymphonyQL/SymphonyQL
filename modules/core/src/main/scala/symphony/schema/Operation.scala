package symphony.schema

import scala.annotation.targetName

import symphony.parser.adt.introspection.IntrospectionType

final case class Operation(opType: IntrospectionType, stage: Stage) {

  @targetName("add")
  def ++(that: Operation): Operation =
    Operation(opType ++ that.opType, Stage.mergeStages(stage, that.stage))
}
