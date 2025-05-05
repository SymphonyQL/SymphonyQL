package symphony.schema

import scala.concurrent.Future
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl
import org.apache.pekko.stream.javadsl
import symphony.parser.*
import symphony.execution.*
import symphony.parser.SymphonyQLValue.NullValue

import java.util
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

sealed trait Stage

object Stage {

  def mergeStages(stage1: Stage, stage2: Stage): Stage =
    (stage1, stage2) match {
      case (ObjectStage(name, fields1), ObjectStage(_, fields2)) =>
        val r = fields1 ++ fields2
        ObjectStage(name, r)
      case (ObjectStage(_, _), _)                                => stage1
      case _                                                     => stage2
    }

  val NullStage: PureStage = PureStage(NullValue)

  final case class SourceStage(source: scaladsl.Source[Stage, NotUsed])             extends Stage
  final case class FutureStage(future: Future[Stage])                               extends Stage
  final case class FunctionStage(stage: Map[String, SymphonyQLInputValue] => Stage) extends Stage
  final case class ListStage(stages: List[Stage])                                   extends Stage
  final case class ObjectStage(name: String, fields: Map[String, Stage])            extends Stage
}

sealed trait ExecutionStage

object ExecutionStage {
  final case class SourceStage(source: scaladsl.Source[ExecutionStage, NotUsed])           extends ExecutionStage
  final case class FutureStage(future: Future[ExecutionStage])                             extends ExecutionStage
  final case class ListStage(stages: List[ExecutionStage], areItemsNullable: Boolean)      extends ExecutionStage
  final case class ObjectStage(fields: List[(String, ExecutionStage, ExecutionFieldInfo)]) extends ExecutionStage
}

final case class PureStage(value: SymphonyQLOutputValue) extends Stage with ExecutionStage
