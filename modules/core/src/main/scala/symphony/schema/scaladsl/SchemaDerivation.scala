package symphony.schema.scaladsl

import magnolia1.Macro
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.annotations.scala.*
import symphony.schema.Stage.*

import scala.compiletime.*
import scala.deriving.Mirror

trait SchemaDerivation extends BaseDerivation {

  implicit inline def gen[A]: Schema[A] = derived[A]

  inline def derived[A]: Schema[A] =
    inline summonInline[Mirror.Of[A]] match {
      case m: Mirror.SumOf[A]     =>
        lazy val members     = recurse[m.MirroredElemLabels, m.MirroredElemTypes]()
        lazy val annotations = Macro.anns[A]
        lazy val info        = Macro.typeInfo[A]
        lazy val subTypes    = members.map { case (label, subTypeAnnotations, schema, _) =>
          (label, schema.lazyType(), subTypeAnnotations)
        }
          .sortBy(_._1)
        lazy val isEnum      = subTypes.forall {
          case (_, t, _)
              if t.fields.apply(__DeprecatedArgs(Some(true))).forall(_.isEmpty) &&
                t.inputFields.apply(__DeprecatedArgs(Some(true))).forall(_.isEmpty) =>
            true
          case _ => false
        }
        lazy val isInterface = annotations.exists {
          case GQLInterface() => true
          case _              => false
        }
        lazy val isUnion     = annotations.exists {
          case GQLUnion() => true
          case _          => false
        }
        new Schema[A] {
          def tpe(isInput: Boolean): __Type =
            val typeName = Some(getName(annotations, info))
            val typeDesc = getDescription(annotations)
            if (isEnum && subTypes.nonEmpty && !isInterface && !isUnion) {
              val enumValuesTypes = subTypes.collect {
                case (name, __Type(_, _, description, _, _, _, _, _, _, _, _, _), annotations) =>
                  __EnumValue(
                    name,
                    description,
                    annotations.collectFirst { case GQLDeprecated(_) => () }.isDefined,
                    annotations.collectFirst { case GQLDeprecated(reason) => reason }
                  )
              }
              Types.mkEnum(typeName, typeDesc, enumValuesTypes, Some(info.full))
            } else if (!isInterface) {
              Types.mkUnion(
                typeName,
                typeDesc,
                subTypes.map { case (_, t, _) => fixEmptyUnionObject(t) },
                Some(info.full)
              )
            } else {
              val impl         = subTypes.map(_._2.copy(interfaces = () => Some(List(tpe(isInput)))))
              val commonFields = () =>
                impl
                  .flatMap(_.fields(__DeprecatedArgs(Some(true))))
                  .flatten
                  .groupBy(_.name)
                  .filter { case (name, list) => list.lengthCompare(impl.size) == 0 }
                  .collect { case (name, list) =>
                    Types
                      .unify(list.map(_.`type`()))
                      .flatMap(t => list.headOption.map(_.copy(`type` = () => t)))
                  }
                  .flatten
                  .toList

              Types.mkInterface(typeName, typeDesc, commonFields, impl, Some(info.full))
            }

          def analyze(value: A): Stage = {
            val (label, _, schema, _) = members(m.ordinal(value))
            if (isEnum) PureStage(EnumValue(label)) else schema.analyze(value)
          }
        }
      case m: Mirror.ProductOf[A] =>
        lazy val fields           = recurse[m.MirroredElemLabels, m.MirroredElemTypes]()
        lazy val annotations      = Macro.anns[A]
        lazy val paramAnnotations = Macro.paramAnns[A].toMap
        lazy val info             = Macro.typeInfo[A]
        new Schema[A] {
          def tpe(isInput: Boolean): __Type =
            if (isInput) {
              val name      = annotations.collectFirst { case GQLInputName(suffix) => suffix }
                .getOrElse(customInputTypeName(getName(annotations, info)))
              val fieldList = fields.map { case (label, _, schema, _) =>
                val fieldAnnotations = paramAnnotations.getOrElse(label, Nil)
                __InputValue(
                  getName(paramAnnotations.getOrElse(label, Nil), label),
                  getDescription(fieldAnnotations),
                  () =>
                    if (schema.optional) schema.lazyType(isInput)
                    else Types.mkNonNull(schema.lazyType(isInput)),
                  None
                )
              }
              Types.mkInputObject(
                Some(name),
                getDescription(annotations),
                fieldList,
                Some(info.full)
              )
            } else {
              val fieldList = fields.filterNot { case (label, _, _, _) =>
                paramAnnotations.getOrElse(label, Nil).exists(_ == GQLExcluded())
              }.map { case (label, _, schema, _) =>
                val fieldAnnotations = paramAnnotations.getOrElse(label, Nil)
                __Field(
                  getName(fieldAnnotations, label),
                  getDescription(fieldAnnotations),
                  (_: __DeprecatedArgs) => schema.arguments,
                  () => if (schema.optional) schema.lazyType(isInput) else Types.mkNonNull(schema.lazyType(isInput)),
                  fieldAnnotations.collectFirst { case GQLDeprecated(_) => () }.isDefined,
                  fieldAnnotations.collectFirst { case GQLDeprecated(reason) => reason }
                )
              }
              Types.mkObject(
                Some(getName(annotations, info)),
                getDescription(annotations),
                fieldList,
                List.empty,
                Some(info.full)
              )
            }

          def analyze(value: A): Stage =
            if (fields.isEmpty) PureStage(EnumValue(getName(annotations, info)))
            else {
              val fieldsBuilder = Map.newBuilder[String, Stage]
              fields.foreach { case (label, _, schema, index) =>
                val fieldAnnotations = paramAnnotations.getOrElse(label, Nil)
                fieldsBuilder += getName(fieldAnnotations, label) -> schema.analyze(
                  value.asInstanceOf[Product].productElement(index)
                )
              }
              ObjectStage(getName(annotations, info), fieldsBuilder.result())
            }
        }
    }

  private inline def recurse[Label, A <: Tuple](index: Int = 0): List[(String, List[Any], Schema[Any], Int)] =
    inline erasedValue[(Label, A)] match {
      case (_: (name *: names), _: (t *: ts)) =>
        val label       = constValue[name].toString
        val builder     = summonInline[Schema[t]].asInstanceOf[Schema[Any]]
        val annotations = Macro.anns[t]
        (label, annotations, builder, index) :: recurse[names, ts](index + 1)
      case (_: EmptyTuple, _)                 => Nil
    }
}
