package symphony
package schema

import scala.collection.immutable

import symphony.schema.ArgumentExtractor.*

object ArgumentExtractorRegistry {

  private var CUSTOM_ARGUMENT_EXTRACTOR = Map[Class[_], ArgumentExtractor[_]]()

  def getExtractors: Map[Class[_], ArgumentExtractor[_]] =
    DEFAULT_SCALA_PRIMITIVE_ARGUMENT_EXTRACTORS ++ CUSTOM_ARGUMENT_EXTRACTOR

  def register(runtime: Class[_], customExtractor: ArgumentExtractor[_]): Unit =
    CUSTOM_ARGUMENT_EXTRACTOR += runtime -> customExtractor

  def listOf[A](extractor: ArgumentExtractor[A]): ListArgumentExtractor[A]     = ListArgumentExtractor(extractor)
  def optionOf[A](extractor: ArgumentExtractor[A]): OptionArgumentExtractor[A] = OptionArgumentExtractor(extractor)
  def seqOf[A](extractor: ArgumentExtractor[A]): SeqArgumentExtractor[A]       = SeqArgumentExtractor(extractor)
  def setOf[A](extractor: ArgumentExtractor[A]): SetArgumentExtractor[A]       = SetArgumentExtractor(extractor)
  def vectorOf[A](extractor: ArgumentExtractor[A]): VectorArgumentExtractor[A] = VectorArgumentExtractor(extractor)

  lazy val DEFAULT_SCALA_PRIMITIVE_ARGUMENT_EXTRACTORS: Map[Class[_], ArgumentExtractor[_]] = List(
    classOf[Boolean] -> BooleanArgumentExtractor,
    classOf[Double]  -> DoubleArgumentExtractor,
    classOf[Float]   -> FloatArgumentExtractor,
    classOf[Int]     -> IntArgumentExtractor,
    classOf[Long]    -> LongArgumentExtractor,
    classOf[String]  -> StringArgumentExtractor,
    classOf[Unit]    -> UnitArgumentExtractor
  ).toMap

}
