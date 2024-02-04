package symphony.schema

import scala.concurrent.Future

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl
import org.apache.pekko.stream.javadsl

import symphony.parser.*
import symphony.parser.SymphonyQLValue.NullValue
import java.util.function
import scala.jdk.FunctionConverters.*
import scala.jdk.CollectionConverters.*

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

  final case class ScalaSourceStage(source: scaladsl.Source[Stage, NotUsed])        extends Stage
  final case class JavaSourceStage(source: javadsl.Source[Stage, NotUsed])          extends Stage
  final case class FutureStage(future: Future[Stage])                               extends Stage
  final case class FunctionStage(stage: Map[String, SymphonyQLInputValue] => Stage) extends Stage
  final case class ListStage(stages: List[Stage])                                   extends Stage
  final case class ObjectStage(name: String, fields: Map[String, Stage])            extends Stage

  def createNull(): Stage                                                                       = NullStage
  def createPure(value: SymphonyQLOutputValue): Stage                                           = PureStage(value)
  def createJavaSource(value: javadsl.Source[Stage, NotUsed]): Stage                            = JavaSourceStage(value)
  def createScalaSource(value: scaladsl.Source[Stage, NotUsed]): Stage                          = ScalaSourceStage(value)
  def createList(value: java.util.List[Stage]): Stage                                           = ListStage(value.asScala.toList)
  def createObject(name: String, fields: java.util.Map[String, Stage]): Stage                   = ObjectStage(name, fields.asScala.toMap)
  def createFunction(value: function.Function[Map[String, SymphonyQLInputValue], Stage]): Stage =
    FunctionStage(value.asScala)

}

sealed trait ExecutionStage

object ExecutionStage {
  final case class ScalaSourceStage(source: scaladsl.Source[ExecutionStage, NotUsed]) extends ExecutionStage
  final case class JavaSourceStage(source: javadsl.Source[ExecutionStage, NotUsed])   extends ExecutionStage
  final case class FutureStage(future: Future[ExecutionStage])                        extends ExecutionStage
  final case class ListStage(stages: List[ExecutionStage])                            extends ExecutionStage
  final case class ObjectStage(fields: List[(String, ExecutionStage)])                extends ExecutionStage
}

final case class PureStage(value: SymphonyQLOutputValue) extends Stage with ExecutionStage
