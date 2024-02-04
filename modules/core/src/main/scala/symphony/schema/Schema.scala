package symphony.schema

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.{ javadsl, scaladsl }
import symphony.parser.*
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLOutputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.Directive
import symphony.parser.adt.introspection.*
import symphony.schema.Stage.*
import symphony.schema.scaladsl.*

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.FunctionConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.OptionConverters.*

trait Schema[T] { self =>
  def optional: Boolean                        = false
  def toType(isInput: Boolean = false): IntrospectionType
  def arguments: List[IntrospectionInputValue] = Nil
  def analyze(value: T): Stage

  def contramap[A](f: A => T): Schema[A] = new Schema[A] {
    override def optional: Boolean                           = self.optional
    override def arguments: List[IntrospectionInputValue]    = self.arguments
    override def toType(isInput: Boolean): IntrospectionType = self.toType(isInput)
    override def analyze(value: A): Stage                    = self.analyze(f(value))
  }
}

object Schema extends GenericSchema with SchemaJavaAPI {
  def apply[T](implicit schema: Schema[T]): Schema[T] = schema
}

trait SchemaJavaAPI {
  self: GenericSchema =>

  /**
   * Java API
   */
  def createScalar[A](
    name: String,
    description: java.util.Optional[String],
    toOutput: java.util.function.Function[A, SymphonyQLOutputValue]
  ): Schema[A] =
    mkScalar(name, description.toScala, toOutput.asScala)

  /**
   * Java API
   */
  def createFunction[A, B](
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[java.util.function.Function[A, B]] =
    mkFunction(argumentExtractor, inputSchema, outputSchema).contramap(_.asScala)

  def createSource[A](schema: Schema[A]): Schema[javadsl.Source[A, NotUsed]] =
    mkSource(schema).contramap(_.asScala)

  /**
   * Java API
   */
  def createOptional[A](schema: Schema[A]): Schema[java.util.Optional[A]] = mkOption[A](schema).contramap(_.toScala)

  /**
   * Java API
   * Unsafe Nullable
   */
  def createNullable[A](schema: Schema[A]): Schema[A] = new Schema[A] {
    override def optional: Boolean                           = true
    override def toType(isInput: Boolean): IntrospectionType = schema.toType(isInput)
    override def analyze(value: A): Stage                    = schema.analyze(value)
  }

  /**
   * Java API
   */
  def createVector[A](schema: Schema[A]): Schema[java.util.Vector[A]] =
    mkVector[A](schema).contramap(_.asScala.toVector)

  /**
   * Java API
   */
  def createSet[A](schema: Schema[A]): Schema[java.util.Set[A]] =
    mkSet[A](schema).contramap(_.asScala.toSet)

  /**
   * Java API
   */
  def createList[A](schema: Schema[A]): Schema[java.util.List[A]]                                  =
    mkList[A](schema).contramap(_.asScala.toList)

    /**
     * Java API
     */
  def createCompletionStage[A](schema: Schema[A]): Schema[java.util.concurrent.CompletionStage[A]] =
    mkFuture[A](schema).contramap(_.asScala)

}
trait GenericSchema extends SchemaDerivation {

  implicit val UnitSchema: Schema[Unit]       = mkScalar("Unit", None, _ => ObjectValue(Nil))
  implicit val BooleanSchema: Schema[Boolean] = mkScalar("Boolean", None, BooleanValue.apply)
  implicit val StringSchema: Schema[String]   = mkScalar("String", None, StringValue.apply)
  implicit val IntSchema: Schema[Int]         = mkScalar("Int", None, IntValue(_))
  implicit val LongSchema: Schema[Long]       = mkScalar("Long", None, IntValue(_))
  implicit val DoubleSchema: Schema[Double]   = mkScalar("Float", None, FloatValue(_))
  implicit val FloatSchema: Schema[Float]     = mkScalar("Float", None, FloatValue(_))

  def mkEnum[A](
    name: String,
    description: Option[String] = None,
    values: List[IntrospectionEnumValue],
    repr: A => String,
    directives: List[Directive] = List.empty
  ): Schema[A] = new Schema[A] {
    private val validEnumValues                              = values.map(_.name).toSet
    override def toType(isInput: Boolean): IntrospectionType =
      Types.mkEnum(Some(name), description, values, None, if (directives.nonEmpty) Some(directives) else None)
    override def analyze(value: A): Stage                    = {
      val asString = repr(value)
      if (validEnumValues.contains(asString)) PureStage(EnumValue(asString))
      else Stage.FutureStage(Future.failed(SymphonyQLError.ExecutionError(s"Invalid enum value '$asString'")))
    }
  }

  def mkScalar[A](name: String, description: Option[String], toOutput: A => SymphonyQLOutputValue): Schema[A] =
    new Schema[A] {
      override def toType(isInput: Boolean): IntrospectionType = Types.mkScalar(name, description)
      override def analyze(value: A): Stage                    = PureStage(toOutput(value))
    }

  def mkObject[A](
    name: String,
    description: Option[String],
    fields: Boolean => List[(IntrospectionField, A => Stage)],
    directives: List[Directive] = List.empty
  ): Schema[A] =
    new Schema[A] {
      override def toType(isInput: Boolean): IntrospectionType =
        if (isInput)
          Types.mkInputObject(
            Some(if (name.endsWith("Input")) name else s"${name}Input"),
            description,
            fields(isInput)
              .map(_._1)
              .map(f => IntrospectionInputValue(f.name, f.description, f.tpe, None, directives = f.directives)),
            directives = Some(directives)
          )
        else
          Types.mkObject(Some(name), description, fields(isInput).map(_._1), directives)

      override def analyze(value: A): Stage =
        ObjectStage(name, fields(false).map { case (f, aToStage) => f.name -> aToStage(value) }.toMap)
    }

  implicit def mkOption[A](implicit schema: Schema[A]): Schema[Option[A]] = new Schema[Option[A]] {
    override def optional: Boolean                           = true
    override def toType(isInput: Boolean): IntrospectionType = schema.toType(isInput)
    override def analyze(value: Option[A]): Stage            =
      value match {
        case Some(value) => schema.analyze(value)
        case None        => NullStage
      }
  }

  implicit def mkVector[A](implicit schema: Schema[A]): Schema[Vector[A]] =
    mkList[A](schema).contramap(_.toList)

  implicit def mkSet[A](implicit schema: Schema[A]): Schema[Set[A]] = mkList[A](schema).contramap(_.toList)

  implicit def mkSeq[A](implicit schema: Schema[A]): Schema[Seq[A]] = mkList[A](schema).contramap(_.toList)

  implicit def mkList[A](implicit schema: Schema[A]): Schema[List[A]] = new Schema[List[A]] {
    override def toType(isInput: Boolean): IntrospectionType = {
      val t = schema.toType(isInput)
      (if (schema.optional) t else t.nonNull).list
    }
    override def analyze(value: List[A]): Stage              = ListStage(value.map(schema.analyze))
  }

  implicit def mkFuture[A](implicit schema: Schema[A]): Schema[Future[A]] =
    mkSource[A](schema).contramap[Future[A]](scaladsl.Source.future)

  implicit def mkFunction[A, B](implicit
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[A => B] =
    new Schema[A => B] {
      private lazy val inputType                                       = inputSchema.toType(true)
      override def arguments: List[IntrospectionInputValue]            = {
        val input = inputType.allInputFields
        if (input.nonEmpty) input
        else
          inputType.kind match {
            case TypeKind.SCALAR | TypeKind.ENUM | TypeKind.LIST =>
              List(
                IntrospectionInputValue(
                  "value",
                  None,
                  () => if (inputSchema.optional) inputType else inputType.nonNull,
                  None
                )
              )
            case _                                               => List.empty[IntrospectionInputValue]
          }
      }
      override def optional: Boolean                                   = outputSchema.optional
      override def toType(isInput: Boolean = false): IntrospectionType = outputSchema.toType(isInput)
      override def analyze(value: A => B): Stage                       =
        FunctionStage { args =>
          val builder    = argumentExtractor.extract(SymphonyQLInputValue.ObjectValue(args))
          val fixBuilder = inputType.kind match {
            case TypeKind.SCALAR | TypeKind.ENUM | TypeKind.LIST =>
              builder.fold(
                error => args.get("value").fold[Either[ArgumentError, A]](Left(error))(argumentExtractor.extract),
                Right(_)
              )
            case _                                               => builder
          }
          fixBuilder.fold(
            error => ScalaSourceStage(scaladsl.Source.failed(error)),
            input => outputSchema.analyze(value(input))
          )
        }
    }

  implicit def mkSource[A](implicit schema: Schema[A]): Schema[scaladsl.Source[A, NotUsed]] =
    new Schema[scaladsl.Source[A, NotUsed]] {
      override def optional: Boolean                                  = true
      override def toType(isInput: Boolean): IntrospectionType        =
        val t = schema.toType(isInput)
        (if (schema.optional) t else t.nonNull).list
      override def analyze(value: scaladsl.Source[A, NotUsed]): Stage = ScalaSourceStage(value.map(schema.analyze))
    }
}
