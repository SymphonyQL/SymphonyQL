package symphony.schema

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

import symphony.parser.*
import symphony.parser.SymphonyQLValue.NullValue

sealed trait Stage

object Stage {

  def mergeStages(stage1: Stage, stage2: Stage): Stage =
    (stage1, stage2) match {
      case (ObjectStage(name, fields1), ObjectStage(_, fields2)) =>
        val r = fields1 ++ fields2
        ObjectStage(name, r)
      case (ObjectStage(_, _), _) => stage1
      case _                      => stage2
    }

  val NullStage: PureStage = PureStage(NullValue)

  final case class SourceStage(source: Source[Stage, NotUsed])                      extends Stage
  final case class FunctionStage(stage: Map[String, SymphonyQLInputValue] => Stage) extends Stage
  final case class ListStage(stages: List[Stage])                                   extends Stage
  final case class ObjectStage(name: String, fields: Map[String, Stage])            extends Stage
  final case class PureStage(value: SymphonyQLOutputValue)                          extends Stage
}

sealed trait ExecutionStage

object ExecutionStage {
  final case class SourceStage(source: Source[ExecutionStage, NotUsed]) extends ExecutionStage
  final case class ListStage(stages: List[ExecutionStage])              extends ExecutionStage
  final case class ObjectStage(fields: List[(String, ExecutionStage)])  extends ExecutionStage
}

final case class PureExecutionStage(value: SymphonyQLOutputValue) extends Stage with ExecutionStage
