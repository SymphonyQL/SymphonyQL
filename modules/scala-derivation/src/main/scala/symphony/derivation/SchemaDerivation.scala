package symphony.derivation

import magnolia1.*
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import symphony.parser.SymphonyQLValue.*
import symphony.parser.introspection.*
import symphony.schema.*
import symphony.schema.Stage.*

import scala.compiletime.*
import scala.concurrent.Future
import scala.deriving.Mirror
import scala.language.implicitConversions

object SchemaDerivation extends GenericSchema {
  def apply[T](implicit schema: Schema[T]): Schema[T] = schema
}

trait GenericSchema extends SchemaDerivation {

  implicit val unit: Schema[Unit]                                                        = Schema.unit
  implicit val boolean: Schema[Boolean]                                                  = Schema.boolean
  implicit val string: Schema[String]                                                    = Schema.string
  implicit val int: Schema[Int]                                                          = Schema.int
  implicit val long: Schema[Long]                                                        = Schema.long
  implicit val double: Schema[Double]                                                    = Schema.double
  implicit val float: Schema[Float]                                                      = Schema.float
  implicit def mkOption[A](implicit schema: Schema[A]): Schema[Option[A]]                = Schema.mkOption(schema)
  implicit def mkVector[A](implicit schema: Schema[A]): Schema[Vector[A]]                = Schema.mkVector(schema)
  implicit def mkSet[A](implicit schema: Schema[A]): Schema[Set[A]]                      = Schema.mkSet(schema)
  implicit def mkSeq[A](implicit schema: Schema[A]): Schema[Seq[A]]                      = Schema.mkSeq(schema)
  implicit def mkList[A](implicit schema: Schema[A]): Schema[List[A]]                    = Schema.mkList(schema)
  implicit def mkFuture[A](implicit schema: Schema[A]): Schema[Future[A]]                = Schema.mkFuture(schema)
  implicit def mkFuncSchema[A, B](implicit
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[A => B] =
    Schema.mkFuncSchema(argumentExtractor, inputSchema, outputSchema)
  implicit def mkSourceSchema[A](implicit schema: Schema[A]): Schema[Source[A, NotUsed]] =
    Schema.mkSourceSchema(schema)
}

trait SchemaDerivation extends BaseDerivation {

  implicit inline def gen[A]: Schema[A] = derived[A]

  inline def derived[A]: Schema[A] =
    inline summonInline[Mirror.Of[A]] match {
      case m: Mirror.SumOf[A]     =>
        lazy val members  = recurse[m.MirroredElemLabels, m.MirroredElemTypes]()
        lazy val info     = Macro.typeInfo[A]
        lazy val subTypes = members.map(m => m._1 -> m._2.toType()).sortBy(_._1)
        lazy val isEnum   = subTypes.forall {
          case (_, t)
              if t.fields.apply(__DeprecatedArgs(Some(true))).forall(_.isEmpty) &&
                t.inputFields.apply(__DeprecatedArgs(Some(true))).forall(_.isEmpty) =>
            true
          case _ => false
        }
        new Schema[A] {
          def toType(isInput: Boolean): __Type =
            if (isEnum && subTypes.nonEmpty) {
              Types.mkEnum(
                Some(getName(info)),
                None,
                subTypes.collect { case (name, __Type(_, _, _, _, _, _, _, _, _, _, _, _)) =>
                  __EnumValue(name, None, false, None, None)
                },
                Some(info.full)
              )
            } else {
              Types.mkUnion(
                Some(getName(info)),
                None,
                subTypes.map(t => fixEmptyUnionObject(t._2)),
                Some(info.full)
              )
            }

          def analyze(value: A): Stage = {
            val (label, schema, _) = members(m.ordinal(value))
            if (isEnum) PureStage(EnumValue(label)) else schema.analyze(value)
          }
        }
      case m: Mirror.ProductOf[A] =>
        lazy val fields = recurse[m.MirroredElemLabels, m.MirroredElemTypes]()
        lazy val info   = Macro.typeInfo[A]
        new Schema[A] {
          def toType(isInput: Boolean): __Type =
            if (isInput)
              Types.mkInputObject(
                Some(customInputTypeName(getName(info))),
                None,
                fields.map { case (label, schema, _) =>
                  __InputValue(
                    label,
                    None,
                    () =>
                      if (schema.optional) schema.toType(isInput)
                      else Types.mkNonNull(schema.toType(isInput)),
                    None,
                    false,
                    None,
                    None
                  )
                },
                Some(info.full)
              )
            else {
              Types.mkObject(
                Some(getName(info)),
                None,
                fields.map { case (label, schema, _) =>
                  __Field(
                    label,
                    None,
                    (_: __DeprecatedArgs) => schema.arguments,
                    () => if (schema.optional) schema.toType(isInput) else Types.mkNonNull(schema.toType(isInput))
                  )
                },
                List.empty,
                Some(info.full)
              )
            }

          def analyze(value: A): Stage =
            if (fields.isEmpty) PureStage(EnumValue(getName(info)))
            else {
              val fieldsBuilder = Map.newBuilder[String, Stage]
              fields.foreach { case (label, schema, index) =>
                fieldsBuilder += label -> schema.analyze(
                  value.asInstanceOf[Product].productElement(index)
                )
              }
              ObjectStage(getName(info), fieldsBuilder.result())
            }
        }
    }

  private inline def recurse[Label, A <: Tuple](index: Int = 0): List[(String, Schema[Any], Int)] =
    inline erasedValue[(Label, A)] match {
      case (_: (name *: names), _: (t *: ts)) =>
        val label   = constValue[name].toString
        val builder = summonInline[Schema[t]].asInstanceOf[Schema[Any]]
        (label, builder, index) :: recurse[names, ts](index + 1)
      case (_: EmptyTuple, _)                 => Nil
    }
}
