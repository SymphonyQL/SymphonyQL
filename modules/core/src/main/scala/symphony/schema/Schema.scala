package symphony.schema

import symphony.parser.OutputValue
import symphony.parser.OutputValue.*
import symphony.parser.Value.*
import symphony.parser.adt.Directive
import symphony.parser.introspection.*
import symphony.schema.ExecutionPlan.*

trait Schema[T] { self =>
  def optional: Boolean = false
  def toType: __Type
  def arguments: List[__InputValue] = Nil
  def resolve(value: T): ExecutionPlan

  def contramap[A](f: A => T): Schema[A] = new Schema[A] {
    override def optional: Boolean                = self.optional
    override def arguments: List[__InputValue]    = self.arguments
    override def toType: __Type                   = self.toType
    override def resolve(value: A): ExecutionPlan = self.resolve(f(value))
  }
}

object Schema {

  def mkScalar[A](name: String, description: Option[String], toOutput: A => OutputValue): Schema[A] =
    new Schema[A] {

      override def toType: __Type = Types.mkScalar(name, description)

      override def resolve(value: A): ExecutionPlan = PureDataPlan(toOutput(value))
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

      override def resolve(value: A): ExecutionPlan = ExecutionPlan.NullPlan
    }

  def mkObject[A](
    name: String,
    description: Option[String],
    isOptional: Boolean = false,
    fields: List[(__Field, A => ExecutionPlan)],
    directives: List[Directive] = List.empty
  ): Schema[A] =
    new Schema[A] {

      override def optional: Boolean = isOptional

      override def toType: __Type = {
        Types.mkObject(Some(name), description, fields.map(_._1), directives)
      }

      override def resolve(value: A): ExecutionPlan =
        ObjectDataPlan(name, fields.map { case (f, plan) => f.name -> plan(value) }.toMap)
    }

  def mkNullable[A](schema: Schema[A]): Schema[A] = new Schema[A] {

    override def optional: Boolean = true

    override def toType: __Type = schema.toType

    override def resolve(value: A): ExecutionPlan = schema.resolve(value)
  }

  def mkOption[A](schema: Schema[A]): Schema[Option[A]] = new Schema[Option[A]] {

    override def optional: Boolean = true

    override def toType: __Type = schema.toType

    override def resolve(value: Option[A]): ExecutionPlan =
      value match {
        case Some(value) => schema.resolve(value)
        case None        => NullPlan
      }
  }

  def mkList[A](schema: Schema[A]): Schema[List[A]] = new Schema[List[A]] {

    override def toType: __Type = {
      val t = schema.toType
      (if (schema.optional) t else t.nonNull).list
    }

    override def resolve(value: List[A]): ExecutionPlan = ListDataPlan(value.map(schema.resolve))
  }

  val unit: Schema[Unit]       = mkScalar("Unit", None, _ => ObjectValue(Nil))
  val boolean: Schema[Boolean] = mkScalar("Boolean", None, BooleanValue.apply)
  val string: Schema[String]   = mkScalar("String", None, StringValue.apply)
  val int: Schema[Int]         = mkScalar("Int", None, IntValue(_))
  val long: Schema[Long]       = mkScalar("Long", None, IntValue(_))
  val double: Schema[Double]   = mkScalar("Float", None, FloatValue(_))
  val float: Schema[Float]     = mkScalar("Float", None, FloatValue(_))
}
