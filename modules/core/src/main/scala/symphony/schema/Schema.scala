package symphony.schema

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.{ javadsl, scaladsl }
import symphony.parser.*
import symphony.parser.SymphonyQLError.*
import symphony.parser.SymphonyQLOutputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.introspection.*
import symphony.schema.Stage.*
import symphony.schema.scaladsl.*

import scala.annotation.{ nowarn, unused }
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.FunctionConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.OptionConverters.*

trait Schema[T] { self =>
  private lazy val tpe: __Type      = tpe()
  private lazy val inputTpe: __Type = tpe(true)

  def optional: Boolean                          = false
  def tpe(isInput: Boolean = false): __Type
  def lazyType(isInput: Boolean = false): __Type = if (isInput) inputTpe else tpe
  def arguments: List[__InputValue]              = Nil
  def analyze(value: T): Stage

  def contramap[A](f: A => T): Schema[A] = new Schema[A] {
    override def optional: Boolean             = self.optional
    override def arguments: List[__InputValue] = self.arguments
    override def tpe(isInput: Boolean): __Type = self.lazyType(isInput)
    override def analyze(value: A): Stage      = self.analyze(f(value))
  }
}

object Schema extends GenericSchema with SchemaJavaAPI {
  def apply[T](implicit schema: Schema[T]): Schema[T] = schema
}

trait IntrospectionSchemaDerivation {

  implicit lazy val __inputValueSchema: Schema[SymphonyQLInputValue] = Schema.gen
  implicit lazy val __introspectionEnumValue: Schema[__EnumValue]    = Schema.gen
  implicit lazy val __introspectionField: Schema[__Field]            = Schema.gen
  implicit lazy val __introspectionType: Schema[__Type]              = Schema.gen
  implicit lazy val __introspectionSchema: Schema[__Schema]          = Schema.gen
  val introspection: Schema[__Introspection]                         = Schema.gen
}

trait SchemaJavaAPI {
  self: GenericSchema =>

  /**
   * Java API
   */
  @unused
  def createScalar[A](
    name: String,
    description: java.util.Optional[String],
    toOutput: java.util.function.Function[A, SymphonyQLOutputValue]
  ): Schema[A] =
    mkScalar(name, description.toScala, toOutput.asScala)

  /**
   * Java API
   */
  @unused
  def createFunctionUnit[A](
    schema: Schema[A]
  ): Schema[java.util.function.Supplier[A]] =
    mkFunctionUnitSchema(schema).contramap(_.asScala)

  /**
   * Java API
   */
  @unused
  def createFunction[A, B](
    inputSchema: Schema[A],
    // When we recursively process in APT, the first thing we get is inputSchema
    argumentExtractor: ArgumentExtractor[A],
    outputSchema: Schema[B]
  ): Schema[java.util.function.Function[A, B]] =
    mkFunction(argumentExtractor, inputSchema, outputSchema).contramap(_.asScala)

  @unused
  def createSource[A](schema: Schema[A]): Schema[javadsl.Source[A, NotUsed]] =
    mkSource(schema).contramap(_.asScala)

  /**
   * Java API
   */
  @unused
  def createOptional[A](schema: Schema[A]): Schema[java.util.Optional[A]] = mkOption[A](schema).contramap(_.toScala)

  /**
   * Java API
   */
  @unused
  def createMap[A, B](keySchema: Schema[A], valueSchema: Schema[B]): Schema[java.util.Map[A, B]] =
    mkMapSchema[A, B](keySchema, valueSchema).contramap(kv => kv.asScala.toMap)

    /**
     * Java API
     */
  @unused
  def createTuple2[A, B](keySchema: Schema[A], valueSchema: Schema[B]): Schema[(A, B)]           =
    mkTuple2Schema[A, B](keySchema, valueSchema)

  /**
   * Java API
   * Unsafe Nullable
   */
  @unused
  def createNullable[A](schema: Schema[A]): Schema[A] = new Schema[A] {
    override def optional: Boolean             = true
    override def tpe(isInput: Boolean): __Type = schema.lazyType(isInput)
    override def analyze(value: A): Stage      = schema.analyze(value)
  }

  /**
   * Java API
   */
  @unused
  def createVector[A](schema: Schema[A]): Schema[java.util.Vector[A]] =
    mkVector[A](schema).contramap(_.asScala.toVector)

  /**
   * Java API
   */
  @unused
  def createSet[A](schema: Schema[A]): Schema[java.util.Set[A]] =
    mkSet[A](schema).contramap(_.asScala.toSet)

  /**
   * Java API
   */
  @unused
  def createList[A](schema: Schema[A]): Schema[java.util.List[A]]                                  =
    mkList[A](schema).contramap(_.asScala.toList)

    /**
     * Java API
     */
  @unused
  def createCompletionStage[A](schema: Schema[A]): Schema[java.util.concurrent.CompletionStage[A]] =
    mkFuture[A](schema).contramap(_.asScala)

  /**
   * Using in APT
   */
  @unused
  def getSchema(typeName: String): Schema[?] =
    typeName match
      case "java.lang.Boolean"    => Schema.createNullable(Schema.BooleanSchema)
      case "java.lang.String"     => Schema.createNullable(Schema.StringSchema)
      case "java.lang.Integer"    => Schema.createNullable(Schema.IntSchema)
      case "java.lang.Long"       => Schema.createNullable(Schema.LongSchema)
      case "java.lang.Double"     => Schema.createNullable(Schema.DoubleSchema)
      case "java.lang.Float"      => Schema.createNullable(Schema.FloatSchema)
      case "java.lang.Short"      => Schema.createNullable(Schema.ShortSchema)
      case "java.math.BigInteger" => Schema.createNullable(Schema.BigIntegerSchema)
      case "java.math.BigDecimal" => Schema.createNullable(Schema.JavaBigDecimalSchema)
      case "java.lang.Void"       => Schema.UnitSchema
      case "boolean"              => Schema.BooleanSchema
      case "int"                  => Schema.IntSchema
      case "long"                 => Schema.LongSchema
      case "double"               => Schema.DoubleSchema
      case "float"                => Schema.FloatSchema
      case "short"                => Schema.ShortSchema
      case "void"                 => Schema.UnitSchema
      case _                      => throw new IllegalArgumentException(s"Method 'Schema.getSchema' is not support for $typeName")

}
trait GenericSchema extends SchemaDerivation {

  implicit val UnitSchema: Schema[Unit]                           = mkScalar("Unit", None, _ => ObjectValue(Nil))
  implicit val BooleanSchema: Schema[Boolean]                     = mkScalar("Boolean", None, BooleanValue.apply)
  implicit val StringSchema: Schema[String]                       = mkScalar("String", None, StringValue.apply)
  implicit val IntSchema: Schema[Int]                             = mkScalar("Int", None, IntValue(_))
  implicit val LongSchema: Schema[Long]                           = mkScalar("Long", None, IntValue(_))
  implicit val DoubleSchema: Schema[Double]                       = mkScalar("Float", None, FloatValue(_))
  implicit val FloatSchema: Schema[Float]                         = mkScalar("Float", None, FloatValue(_))
  implicit val ShortSchema: Schema[Short]                         = mkScalar("Short", None, IntValue(_))
  implicit val BigIntSchema: Schema[BigInt]                       = mkScalar("BigInt", None, IntValue(_))
  implicit val BigIntegerSchema: Schema[java.math.BigInteger]     = mkScalar("BigInt", None, IntValue(_))
  implicit val BigDecimalSchema: Schema[BigDecimal]               = mkScalar("BigDecimal", None, FloatValue(_))
  implicit val JavaBigDecimalSchema: Schema[java.math.BigDecimal] = mkScalar("BigDecimal", None, FloatValue(_))

  def mkEnum[A](
    name: String,
    description: Option[String] = None,
    values: List[__EnumValue],
    repr: A => String,
    directives: List[Directive] = List.empty
  ): Schema[A] = new Schema[A] {
    private val validEnumValues                = values.map(_.name).toSet
    override def tpe(isInput: Boolean): __Type =
      Types.mkEnum(Some(name), description, values, None, if (directives.nonEmpty) Some(directives) else None)
    override def analyze(value: A): Stage      = {
      val asString = repr(value)
      if (validEnumValues.contains(asString)) PureStage(EnumValue(asString))
      else Stage.FutureStage(Future.failed(SymphonyQLError.ExecutionError(s"Invalid enum value '$asString'")))
    }
  }

  def mkScalar[A](name: String, description: Option[String], toOutput: A => SymphonyQLOutputValue): Schema[A] =
    new Schema[A] {
      override def tpe(isInput: Boolean): __Type = Types.mkScalar(name, description)
      override def analyze(value: A): Stage      = PureStage(toOutput(value))
    }

  def mkObject[A](
    name: String,
    description: Option[String],
    fields: Boolean => List[(__Field, A => Stage)],
    directives: List[Directive] = List.empty
  ): Schema[A] =
    new Schema[A] {
      override def tpe(isInput: Boolean): __Type =
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
        ObjectStage(
          name,
          fields(false).map { case (f, aToStage) =>
            f.name -> aToStage(value)
          }.toMap
        )
    }

  implicit def mkOption[A](implicit schema: Schema[A]): Schema[Option[A]] = new Schema[Option[A]] {
    override def optional: Boolean                = true
    override def tpe(isInput: Boolean): __Type    = schema.lazyType(isInput)
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
    override def tpe(isInput: Boolean): __Type  = {
      val t = schema.lazyType(isInput)
      (if (schema.optional) t else t.nonNull).list
    }
    override def analyze(value: List[A]): Stage = ListStage(value.map(schema.analyze))
  }

  implicit def mkFuture[A](implicit schema: Schema[A]): Schema[Future[A]] =
    mkSource[A](schema).contramap[Future[A]](scaladsl.Source.future)

  implicit def mkFunction[A, B](implicit
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[A => B] =
    new Schema[A => B] {
      private lazy val inputType                         = inputSchema.lazyType(true)
      override def arguments: List[__InputValue]         = {
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
      override def optional: Boolean                     = outputSchema.optional
      override def tpe(isInput: Boolean = false): __Type = outputSchema.lazyType(isInput)
      override def analyze(value: A => B): Stage         =
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
          fixBuilder.fold(
            error => ScalaSourceStage(scaladsl.Source.failed(error)),
            input => outputSchema.analyze(value(input))
          )
        }
    }

  implicit def mkSource[A](implicit schema: Schema[A]): Schema[scaladsl.Source[A, NotUsed]] =
    new Schema[scaladsl.Source[A, NotUsed]] {
      override def optional: Boolean                                  = true
      override def tpe(isInput: Boolean): __Type                      =
        val t = schema.lazyType(isInput)
        (if (schema.optional) t else t.nonNull).list
      override def analyze(value: scaladsl.Source[A, NotUsed]): Stage = ScalaSourceStage(value.map(schema.analyze))
    }

  implicit def mkMapSchema[A, B](implicit keySchema: Schema[A], valueSchema: Schema[B]): Schema[Map[A, B]] =
    new Schema[Map[A, B]] {
      private lazy val typeAName: String   = Types.name(keySchema.lazyType())
      private lazy val typeBName: String   = Types.name(valueSchema.lazyType())
      private lazy val name: String        = s"KV$typeAName$typeBName"
      private lazy val description: String = s"A key-value pair of $typeAName and $typeBName"

      private lazy val kvSchema: Schema[(A, B)] =
        Schema.mkObject[(A, B)](
          name,
          Some(description),
          isInput =>
            List(
              __Field(
                "key",
                Some("Key"),
                _ => List.empty,
                () => if (keySchema.optional) keySchema.lazyType(isInput) else keySchema.lazyType(isInput).nonNull
              ) -> (kv => keySchema.analyze(kv._1)),
              __Field(
                "value",
                Some("Value"),
                _ => List.empty,
                () => if (valueSchema.optional) valueSchema.lazyType(isInput) else valueSchema.lazyType(isInput).nonNull
              ) -> (kv => valueSchema.analyze(kv._2))
            )
        )

      override def tpe(isInput: Boolean): __Type =
        kvSchema.lazyType(isInput).nonNull.list

      override def analyze(value: Map[A, B]): Stage = ListStage(value.toList.map(kvSchema.analyze))
    }

  implicit def mkTuple2Schema[A, B](implicit keySchema: Schema[A], valueSchema: Schema[B]): Schema[(A, B)] = {
    val typeAName: String   = Types.name(keySchema.lazyType())
    val typeBName: String   = Types.name(valueSchema.lazyType())
    val name: String        = s"Tuple${typeAName}And$typeBName"
    val description: String = s"A tuple of $typeAName and $typeBName"
    Schema.mkObject[(A, B)](
      name,
      Some(description),
      isInput =>
        List(
          __Field(
            "_1",
            Some("First element of the tuple"),
            _ => List.empty,
            () => if (keySchema.optional) keySchema.lazyType(isInput) else keySchema.lazyType(isInput).nonNull
          ) -> (kv => keySchema.analyze(kv._1)),
          __Field(
            "_2",
            Some("Second element of the tuple"),
            _ => List.empty,
            () => if (valueSchema.optional) valueSchema.lazyType(isInput) else valueSchema.lazyType(isInput).nonNull
          ) -> (kv => valueSchema.analyze(kv._2))
        )
    )
  }

  implicit def mkFunctionUnitSchema[A](implicit schema: Schema[A]): Schema[() => A] =
    new Schema[() => A] {
      override def optional: Boolean              = schema.optional
      override def tpe(isInput: Boolean): __Type  = schema.lazyType(isInput)
      override def analyze(value: () => A): Stage = FunctionStage(_ => schema.analyze(value()))
    }

  implicit def mkEitherSchema[A, B](implicit
    leftSchema: Schema[A],
    rightSchema: Schema[B]
  ): Schema[Either[A, B]] = {
    val typeAName: String   = Types.name(leftSchema.lazyType())
    val typeBName: String   = Types.name(rightSchema.lazyType())
    val name: String        = s"Either${typeAName}Or$typeBName"
    val description: String = s"Either $typeAName or $typeBName"

    implicit val _leftSchema: Schema[A]  = new Schema[A] {
      override def optional: Boolean             = true
      override def tpe(isInput: Boolean): __Type = leftSchema.lazyType(isInput)
      override def analyze(value: A): Stage      = leftSchema.analyze(value)
    }
    implicit val _rightSchema: Schema[B] = new Schema[B] {
      override def optional: Boolean             = true
      override def tpe(isInput: Boolean): __Type = rightSchema.lazyType(isInput)
      override def analyze(value: B): Stage      = rightSchema.analyze(value)
    }

    Schema.mkObject[Either[A, B]](
      name,
      Some(description),
      isInput =>
        List(
          __Field(
            "left",
            Some("Left element of the Either"),
            _ => List.empty,
            () => _leftSchema.lazyType(isInput)
          ) -> (either => either.map(_ => NullStage).fold(_leftSchema.analyze, identity)),
          __Field(
            "right",
            Some("Right element of the Either"),
            _ => List.empty,
            () => _rightSchema.lazyType(isInput)
          ) -> (either => either.swap.map(_ => NullStage).fold(_rightSchema.analyze, identity))
        )
    )
  }
}
