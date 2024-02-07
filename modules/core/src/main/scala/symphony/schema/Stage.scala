package symphony.schema

import scala.concurrent.Future

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl
import org.apache.pekko.stream.javadsl

import symphony.parser.*
import symphony.parser.SymphonyQLValue.NullValue
import scala.jdk.FunctionConverters.*
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

  final case class ScalaSourceStage(source: scaladsl.Source[Stage, NotUsed])        extends Stage
  final case class JavaSourceStage(source: javadsl.Source[Stage, NotUsed])          extends Stage
  final case class FutureStage(future: Future[Stage])                               extends Stage
  final case class FunctionStage(stage: Map[String, SymphonyQLInputValue] => Stage) extends Stage
  final case class ListStage(stages: List[Stage])                                   extends Stage
  final case class ObjectStage(name: String, fields: Map[String, Stage])            extends Stage

  def createNull(): Stage                                                                                 = NullStage
  def createPure(value: SymphonyQLOutputValue): Stage                                                     = PureStage(value)
  def createSource(value: javadsl.Source[Stage, NotUsed]): Stage                                          = JavaSourceStage(value)
  def createList(value: java.util.List[Stage]): Stage                                                     = ListStage(value.asScala.toList)
  def createCompletionStage(value: java.util.concurrent.CompletionStage[Stage]): Stage                    = FutureStage(value.asScala)
  def createObject(name: String, fields: java.util.Map[String, Stage]): Stage                             = ObjectStage(name, fields.asScala.toMap)
  def createFunction(value: java.util.function.Function[Map[String, SymphonyQLInputValue], Stage]): Stage =
    FunctionStage(value.asScala)

  /**
   * Too slow, only for testing
   * We should automatically derive all stages for Java 21
   */
  private[symphony] def derivesStageByReflection[Input, Output](
    argumentExtractor: ArgumentExtractor[Input],
    value: java.util.function.Function[Input, Output]
  ): Stage =
    FunctionStage { args =>
      val input = argumentExtractor.extract(SymphonyQLInputValue.ObjectValue(args)).map(value.apply)
      input match
        case Left(error)  =>
          error.printStackTrace()
          Stage.createNull()
        case Right(value) =>
          Stage.createObject(value)
    }

  def createScalaFunction(value: Map[String, SymphonyQLInputValue] => Stage): Stage = FunctionStage(value)
  def createScalaSource(value: scaladsl.Source[Stage, NotUsed]): Stage              = ScalaSourceStage(value)
  def createScalaList(value: List[Stage]): Stage                                    = ListStage(value)
  def createScalaFuture(value: Future[Stage]): Stage                                = FutureStage(value)

  /**
   * Only for testing
   * We should automatically derive all stages for Java21
   */
  private[symphony] def createObject[T](obj: T): Stage = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def getStageValue(obj: Any): Stage = obj match {
      case value: String                                  => Stage.createPure(SymphonyQLValue.StringValue(value))
      case value: Int                                     => Stage.createPure(SymphonyQLValue.IntValue(value))
      case value: Long                                    => Stage.createPure(SymphonyQLValue.IntValue(value))
      case value: Float                                   => Stage.createPure(SymphonyQLValue.FloatValue(value))
      case value: Double                                  => Stage.createPure(SymphonyQLValue.FloatValue(value))
      case value: Boolean                                 => Stage.createPure(SymphonyQLValue.BooleanValue(value))
      case value: Enum[_]                                 => Stage.createPure(SymphonyQLValue.EnumValue(value.name()))
      case value: scala.reflect.Enum                      =>
        Stage.createPure(SymphonyQLValue.EnumValue(value.toString))
      case value: java.util.List[_]                       =>
        Stage.createList(value.stream().map(v => createObject(v)).collect(java.util.stream.Collectors.toList))
      case value: List[_]                                 =>
        Stage.createScalaList(value.map(v => createObject(v)))
      case value: javadsl.Source[_, NotUsed] @unchecked   =>
        Stage.createSource(value.map(any => createObject(any)))
      case value: scaladsl.Source[_, NotUsed] @unchecked  =>
        Stage.createScalaSource(value.map(any => createObject(any)))
      case value: Future[_]                               =>
        Stage.createScalaFuture(value.map(f => createObject(f)))
      case value: java.util.concurrent.CompletionStage[_] =>
        Stage.createCompletionStage(value.thenApply(a => createObject(a)))
      case _                                              => Stage.createNull()
    }

    def isCaseOrRecordClass(obj: Any): Boolean = {
      val clazz = obj.getClass
      clazz.isRecord || (
        obj.isInstanceOf[Product] &&
          !obj.isInstanceOf[Enum[_]] &&
          !obj.isInstanceOf[scala.reflect.Enum]
      )
    }

    if (obj == null) return Stage.createNull()
    if (!isCaseOrRecordClass(obj)) return getStageValue(obj)

    val clazz   = obj.getClass
    val fields  = clazz.getDeclaredConstructors.apply(0).getParameters.toList.map(p => p.getName -> p.getType).map(_._1)
    val methods =
      clazz.getMethods.filter(m => m.getParameterCount == 0 && fields.exists(f => m.getName.contains(f))).toList
    Stage.ObjectStage(
      clazz.getSimpleName,
      fields.map { fieldName =>
        val methodOpt = methods.find(_.getName == fieldName)
        fieldName -> {
          methodOpt match
            case None         => Stage.createNull()
            case Some(method) =>
              try {
                method.setAccessible(true)
                val fieldValue = method.invoke(obj)
                if (isCaseOrRecordClass(fieldValue)) createObject(fieldValue)
                else getStageValue(fieldValue)
              } catch {
                case _: Throwable => Stage.createNull()
              } finally method.setAccessible(false)
        }
      }.toMap
    )
  }
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
