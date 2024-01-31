package symphony.schema

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

import symphony.parser.{ SymphonyQLInputValue, SymphonyQLOutputValue }
import symphony.parser.SymphonyQLError.{ ArgumentError, ExecutionError }
import symphony.parser.SymphonyQLOutputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.Directive
import symphony.parser.introspection.*
import symphony.schema.Stage.*

trait Schema[T] { self =>
  def optional: Boolean = false
  def toType: __Type
  def arguments: List[__InputValue] = Nil
  def resolve(value: T): Stage

  def contramap[A](f: A => T): Schema[A] = new Schema[A] {
    override def optional: Boolean             = self.optional
    override def arguments: List[__InputValue] = self.arguments
    override def toType: __Type                = self.toType
    override def resolve(value: A): Stage      = self.resolve(f(value))
  }
}

object Schema {

  def mkScalar[A](name: String, description: Option[String], toOutput: A => SymphonyQLOutputValue): Schema[A] =
    new Schema[A] {

      override def toType: __Type = Types.mkScalar(name, description)

      override def resolve(value: A): Stage = PureStage(toOutput(value))
    }

  def mkInputObject[A](
    name: String,
    description: Option[String],
    isOptional: Boolean = false,
    fields: List[__Field],
    directives: List[Directive] = List.empty
  ): Schema[A] =
    new Schema[A] {

      override def optional: Boolean = isOptional

      override def toType: __Type = {
        Types.mkInputObject(
          Some(if (name.endsWith("Input")) name else s"${name}Input"),
          description,
          fields.map(f => __InputValue(f.name, f.description, f.`type`, None, directives = f.directives)),
          directives = Some(directives)
        )
      }

      override def resolve(value: A): Stage = Stage.NullStage
    }

  def mkObject[A](
    name: String,
    description: Option[String],
    isOptional: Boolean = false,
    fields: List[(__Field, A => Stage)],
    directives: List[Directive] = List.empty
  ): Schema[A] =
    new Schema[A] {

      override def optional: Boolean = isOptional

      override def toType: __Type = {
        Types.mkObject(Some(name), description, fields.map(_._1), directives)
      }

      override def resolve(value: A): Stage =
        ObjectStage(name, fields.map { case (f, aToStage) => f.name -> aToStage(value) }.toMap)
    }

  def mkNullable[A](schema: Schema[A]): Schema[A] = new Schema[A] {

    override def optional: Boolean = true

    override def toType: __Type = schema.toType

    override def resolve(value: A): Stage = schema.resolve(value)
  }

  def mkOption[A](schema: Schema[A]): Schema[Option[A]] = new Schema[Option[A]] {

    override def optional: Boolean = true

    override def toType: __Type = schema.toType

    override def resolve(value: Option[A]): Stage =
      value match {
        case Some(value) => schema.resolve(value)
        case None        => NullStage
      }
  }

  def mkList[A](schema: Schema[A]): Schema[List[A]] = new Schema[List[A]] {

    override def toType: __Type = {
      val t = schema.toType
      (if (schema.optional) t else t.nonNull).list
    }

    override def resolve(value: List[A]): Stage = ListStage(value.map(schema.resolve))
  }

  def mkFuncSchema[A, B](
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[A => B] =
    new Schema[A => B] {
      private lazy val inputType = inputSchema.toType

      override def arguments: List[__InputValue] = {
        val input = inputType.allInputFields
        if (input.nonEmpty) input
        else
          handleInput(List.empty[__InputValue])(
            List(
              __InputValue(
                "value",
                None,
                () => if (inputSchema.optional) inputType else inputType.nonNull,
                None
              )
            )
          )
      }
      override def optional: Boolean = outputSchema.optional
      override def toType: __Type    = outputSchema.toType

      override def resolve(value: A => B): Stage =
        FunctionStage { args =>
          val builder = argumentExtractor.extract(SymphonyQLInputValue.ObjectValue(args))
          handleInput(builder)(
            builder.fold(
              error => args.get("value").fold[Either[ArgumentError, A]](Left(error))(argumentExtractor.extract),
              Right(_)
            )
          )
            .fold(error => SourceStage(Source.failed(error)), input => outputSchema.resolve(value(input)))
        }

      private def handleInput[T](wrapped: => T)(onUnwrapped: => T): T =
        inputType.kind match {
          case __TypeKind.SCALAR | __TypeKind.ENUM | __TypeKind.LIST => onUnwrapped
          case _                                                     => wrapped
        }
    }

  def mkSourceSchema[A](ev: Schema[A]): Schema[Source[A, NotUsed]] =
    new Schema[Source[A, NotUsed]] {
      override def optional: Boolean = ev.optional
      override def toType: __Type    = ev.toType

      override def resolve(value: Source[A, NotUsed]): Stage =
        SourceStage(value.map(ev.resolve).mapError {
          case e: ExecutionError => e
          case other =>
            ExecutionError("Caught error during execution of effect-ful field", innerThrowable = Some(other))
        })
    }

  val unit: Schema[Unit]       = mkScalar("Unit", None, _ => ObjectValue(Nil))
  val boolean: Schema[Boolean] = mkScalar("Boolean", None, BooleanValue.apply)
  val string: Schema[String]   = mkScalar("String", None, StringValue.apply)
  val int: Schema[Int]         = mkScalar("Int", None, IntValue(_))
  val long: Schema[Long]       = mkScalar("Long", None, IntValue(_))
  val double: Schema[Double]   = mkScalar("Float", None, FloatValue(_))
  val float: Schema[Float]     = mkScalar("Float", None, FloatValue(_))
}
