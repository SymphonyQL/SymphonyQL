package symphony.schema.scaladsl

import magnolia1.Macro
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.schema.Stage.*

import scala.compiletime.*
import scala.deriving.Mirror

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
              if t.fields.apply(DeprecatedArgs(Some(true))).forall(_.isEmpty) &&
                t.inputFields.apply(DeprecatedArgs(Some(true))).forall(_.isEmpty) =>
            true
          case _ => false
        }
        new Schema[A] {
          def toType(isInput: Boolean): IntrospectionType =
            if (isEnum && subTypes.nonEmpty) {
              Types.mkEnum(
                Some(getName(info)),
                None,
                subTypes.collect { case (name, IntrospectionType(_, _, _, _, _, _, _, _, _, _, _, _)) =>
                  IntrospectionEnumValue(name, None, false, None, None)
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
          def toType(isInput: Boolean): IntrospectionType =
            if (isInput)
              Types.mkInputObject(
                Some(customInputTypeName(getName(info))),
                None,
                fields.map { case (label, schema, _) =>
                  IntrospectionInputValue(
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
                  IntrospectionField(
                    label,
                    None,
                    (_: DeprecatedArgs) => schema.arguments,
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
