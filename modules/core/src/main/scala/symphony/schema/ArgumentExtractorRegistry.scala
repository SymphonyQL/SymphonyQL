package symphony
package schema

import scala.collection.immutable

import symphony.schema.ArgumentExtractor.*

object ArgumentExtractorRegistry {

  private var CUSTOM_ARGUMENT_EXTRACTOR = Map[String, ArgumentExtractor[_]]()

  def getExtractors: Map[String, ArgumentExtractor[_]] =
    DEFAULT_SCALA_PRIMITIVE_ARGUMENT_EXTRACTORS ++ CUSTOM_ARGUMENT_EXTRACTOR

  def register(name: String, customExtractor: ArgumentExtractor[_]): Unit =
    CUSTOM_ARGUMENT_EXTRACTOR += name -> customExtractor

  def listOf[A](extractor: ArgumentExtractor[A]): ListArgumentExtractor[A]     = ListArgumentExtractor(extractor)
  def optionOf[A](extractor: ArgumentExtractor[A]): OptionArgumentExtractor[A] = OptionArgumentExtractor(extractor)
  def seqOf[A](extractor: ArgumentExtractor[A]): SeqArgumentExtractor[A]       = SeqArgumentExtractor(extractor)
  def setOf[A](extractor: ArgumentExtractor[A]): SetArgumentExtractor[A]       = SetArgumentExtractor(extractor)
  def vectorOf[A](extractor: ArgumentExtractor[A]): VectorArgumentExtractor[A] = VectorArgumentExtractor(extractor)

  lazy val DEFAULT_SCALA_PRIMITIVE_ARGUMENT_EXTRACTORS: Map[String, ArgumentExtractor[_]] = List(
    classOf[Boolean].getSimpleName -> BooleanArgumentExtractor,
    classOf[Double].getSimpleName  -> DoubleArgumentExtractor,
    classOf[Float].getSimpleName   -> FloatArgumentExtractor,
    classOf[Int].getSimpleName     -> IntArgumentExtractor,
    classOf[Long].getSimpleName    -> LongArgumentExtractor,
    classOf[String].getSimpleName  -> StringArgumentExtractor,
    classOf[Unit].getSimpleName    -> UnitArgumentExtractor
  ).toMap

}
