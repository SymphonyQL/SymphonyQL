package symphony.schema

import symphony.parser.introspection.__Type

final case class Operation(opType: __Type, stage: Stage) {

  def ++(that: Operation): Operation =
    Operation(opType ++ that.opType, Stage.mergeStages(stage, that.stage))
}
