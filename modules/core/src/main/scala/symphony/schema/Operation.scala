package symphony.schema

import scala.annotation.targetName

import symphony.parser.adt.introspection.__Type

final case class Operation(opType: __Type, stage: Stage) {

  @targetName("add")
  def ++(that: Operation): Operation =
    Operation(opType ++ that.opType, Stage.mergeStages(stage, that.stage))
}
