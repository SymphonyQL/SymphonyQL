package symphony.schema

import scala.concurrent.Future

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

import symphony.parser.*
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLOutputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.Directive
import symphony.parser.introspection.*
import symphony.schema.Stage.*

trait Schema[T] { self =>
  def optional: Boolean             = false
  def toType(isInput: Boolean = false): __Type
  def arguments: List[__InputValue] = Nil
  def analyze(value: T): Stage

  def contramap[A](f: A => T): Schema[A] = new Schema[A] {
    override def optional: Boolean                = self.optional
    override def arguments: List[__InputValue]    = self.arguments
    override def toType(isInput: Boolean): __Type = self.toType(isInput)
    override def analyze(value: A): Stage         = self.analyze(f(value))
  }
}

object Schema {
//
//  implicit lazy val unit: Schema[Unit]       = mkScalar("Unit", None, _ => ObjectValue(Nil))
//  implicit lazy val boolean: Schema[Boolean] = mkScalar("Boolean", None, BooleanValue.apply)
//  implicit lazy val string: Schema[String]   = mkScalar("String", None, StringValue.apply)
//  implicit lazy val int: Schema[Int]         = mkScalar("Int", None, IntValue(_))
//  implicit lazy val long: Schema[Long]       = mkScalar("Long", None, IntValue(_))
//  implicit lazy val double: Schema[Double]   = mkScalar("Float", None, FloatValue(_))
//  implicit lazy val float: Schema[Float]     = mkScalar("Float", None, FloatValue(_))
//
  def mkScalar[A](name: String, description: Option[String], toOutput: A => SymphonyQLOutputValue): Schema[A] =
    new Schema[A] {
      override def toType(isInput: Boolean): __Type = Types.mkScalar(name, description)
      override def analyze(value: A): Stage         = PureStage(toOutput(value))
    }

  def mkObject[A](
    name: String,
    description: Option[String],
    fields: Boolean => List[(__Field, A => Stage)],
    directives: List[Directive] = List.empty
  ): Schema[A] =
    new Schema[A] {
      override def toType(isInput: Boolean): __Type =
        if (isInput)
          Types.mkInputObject(
            Some(if (name.endsWith("Input")) name else s"${name}Input"),
            description,
            fields(isInput)
              .map(_._1)
              .map(f => __InputValue(f.name, f.description, f.`type`, None, directives = f.directives)),
            directives = Some(directives)
          )
        else
          Types.mkObject(Some(name), description, fields(isInput).map(_._1), directives)

      override def analyze(value: A): Stage =
        ObjectStage(name, fields(false).map { case (f, aToStage) => f.name -> aToStage(value) }.toMap)
    }

  def mkNullable[A](schema: Schema[A]): Schema[A] = new Schema[A] {
    override def optional: Boolean                = true
    override def toType(isInput: Boolean): __Type = schema.toType(isInput)
    override def analyze(value: A): Stage         = schema.analyze(value)
  }

  implicit def mkOption[A](implicit schema: Schema[A]): Schema[Option[A]] = new Schema[Option[A]] {
    override def optional: Boolean                = true
    override def toType(isInput: Boolean): __Type = schema.toType(isInput)
    override def analyze(value: Option[A]): Stage =
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
    override def toType(isInput: Boolean): __Type = {
      val t = schema.toType(isInput)
      (if (schema.optional) t else t.nonNull).list
    }
    override def analyze(value: List[A]): Stage   = ListStage(value.map(schema.analyze))
  }

  implicit def future[A](implicit schema: Schema[A]): Schema[Future[A]] =
    mkSourceSchema[A](schema).contramap[Future[A]](Source.future)

  implicit def mkFuncSchema[A, B](implicit
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[A => B] =
    new Schema[A => B] {
      private lazy val inputType                    = inputSchema.toType(true)
      override def arguments: List[__InputValue]    = {
        val input = inputType.allInputFields
        if (input.nonEmpty) input
        else
          inputType.kind match {
            case __TypeKind.SCALAR | __TypeKind.ENUM | __TypeKind.LIST =>
              List(
                __InputValue(
                  "value",
                  None,
                  () => if (inputSchema.optional) inputType else inputType.nonNull,
                  None
                )
              )
            case _                                                     => List.empty[__InputValue]
          }
      }
      override def optional: Boolean                = outputSchema.optional
      override def toType(isInput: Boolean = false): __Type = outputSchema.toType(isInput)
      override def analyze(value: A => B): Stage    =
        FunctionStage { args =>
          val builder    = argumentExtractor.extract(SymphonyQLInputValue.ObjectValue(args))
          val fixBuilder = inputType.kind match {
            case __TypeKind.SCALAR | __TypeKind.ENUM | __TypeKind.LIST =>
              builder.fold(
                error => args.get("value").fold[Either[ArgumentError, A]](Left(error))(argumentExtractor.extract),
                Right(_)
              )
            case _                                                     => builder
          }
          fixBuilder.fold(error => StreamStage(Source.failed(error)), input => outputSchema.analyze(value(input)))
        }
    }

  implicit def mkSourceSchema[A](implicit schema: Schema[A]): Schema[Source[A, NotUsed]] =
    new Schema[Source[A, NotUsed]] {
      override def optional: Boolean                         = true
      override def toType(isInput: Boolean): __Type          = schema.toType(isInput)
      override def analyze(value: Source[A, NotUsed]): Stage =
        StreamStage(value.map(schema.analyze).mapError {
          case e: ExecutionError => e
          case other             =>
            ExecutionError("Caught error during execution of source field", innerThrowable = Some(other))
        })
    }
}
