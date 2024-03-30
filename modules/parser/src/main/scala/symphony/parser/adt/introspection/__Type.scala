package symphony
package parser
package adt
package introspection

import scala.annotation.targetName

import symphony.parser.SymphonyQLValue.StringValue
import symphony.parser.adt.*
import symphony.parser.adt.Definition.TypeSystemDefinition.TypeDefinition
import symphony.parser.adt.Definition.TypeSystemDefinition.TypeDefinition.*
import symphony.parser.adt.Type.*
import symphony.annotations.scala.*

final case class __Type(
  kind: __TypeKind,
  name: Option[String] = None,
  description: Option[String] = None,
  fields: __DeprecatedArgs => Option[List[__Field]] = _ => None,
  interfaces: () => Option[List[__Type]] = () => None,
  possibleTypes: Option[List[__Type]] = None,
  enumValues: __DeprecatedArgs => Option[List[__EnumValue]] = _ => None,
  inputFields: __DeprecatedArgs => Option[List[__InputValue]] = _ => None,
  ofType: Option[__Type] = None,
  specifiedBy: Option[String] = None,
  @GQLExcluded directives: Option[List[Directive]] = None,
  @GQLExcluded origin: Option[String] = None
) { self =>
  override lazy val hashCode: Int = super.hashCode()

  @targetName("add")
  def ++(that: __Type): __Type = __Type(
    kind,
    (name ++ that.name).reduceOption((_, b) => b),
    (description ++ that.description).reduceOption((_, b) => b),
    args => (fields(args) ++ that.fields(args)).reduceOption(_ ++ _),
    () => (interfaces() ++ that.interfaces()).reduceOption(_ ++ _),
    (possibleTypes ++ that.possibleTypes).reduceOption(_ ++ _),
    args => (enumValues(args) ++ that.enumValues(args)).reduceOption(_ ++ _),
    args => (inputFields(args) ++ that.inputFields(args)).reduceOption(_ ++ _),
    (ofType ++ that.ofType).reduceOption(_ ++ _),
    (specifiedBy ++ that.specifiedBy).reduceOption((_, b) => b),
    (directives ++ that.directives).reduceOption(_ ++ _),
    (origin ++ that.origin).reduceOption((_, b) => b)
  )

  def toType(nonNull: Boolean = false): Type =
    ofType match {
      case Some(of) =>
        kind match {
          case __TypeKind.LIST     => ListType(of.toType(), nonNull)
          case __TypeKind.NON_NULL => of.toType(true)
          case _                   => NamedType(name.getOrElse(""), nonNull)
        }
      case None     => NamedType(name.getOrElse(""), nonNull)
    }

  def toTypeDefinition: Option[TypeDefinition] =
    kind match {
      case __TypeKind.SCALAR       =>
        Some(
          ScalarTypeDefinition(
            description,
            name.getOrElse(""),
            directives
              .getOrElse(Nil) ++
              specifiedBy
                .map(url => Directive("specifiedBy", Map("url" -> StringValue(url))))
                .toList
          )
        )
      case __TypeKind.OBJECT       =>
        Some(
          ObjectTypeDefinition(
            description,
            name.getOrElse(""),
            interfaces().getOrElse(Nil).map(t => NamedType(t.name.getOrElse(""), nonNull = false)),
            directives.getOrElse(Nil),
            allFields.map(_.toFieldDefinition)
          )
        )
      case __TypeKind.INTERFACE    =>
        Some(
          InterfaceTypeDefinition(
            description,
            name.getOrElse(""),
            interfaces().getOrElse(Nil).map(t => NamedType(t.name.getOrElse(""), nonNull = false)),
            directives.getOrElse(Nil),
            allFields.map(_.toFieldDefinition)
          )
        )
      case __TypeKind.UNION        =>
        Some(
          UnionTypeDefinition(
            description,
            name.getOrElse(""),
            directives.getOrElse(Nil),
            possibleTypes.getOrElse(Nil).flatMap(_.name)
          )
        )
      case __TypeKind.ENUM         =>
        Some(
          EnumTypeDefinition(
            description,
            name.getOrElse(""),
            directives.getOrElse(Nil),
            enumValues(__DeprecatedArgs(Some(true))).getOrElse(Nil).map(_.toEnumValueDefinition)
          )
        )
      case __TypeKind.INPUT_OBJECT =>
        Some(
          InputObjectTypeDefinition(
            description,
            name.getOrElse(""),
            directives.getOrElse(Nil),
            allInputFields.map(_.toInputValueDefinition)
          )
        )
      case _                       => None
    }

  lazy val list: __Type = __Type(__TypeKind.LIST, ofType = Some(self))

  lazy val nonNull: __Type = __Type(__TypeKind.NON_NULL, ofType = Some(self))

  lazy val allFields: List[__Field] =
    fields(__DeprecatedArgs(Some(true))).getOrElse(Nil)

  lazy val allInputFields: List[__InputValue] =
    inputFields(__DeprecatedArgs(Some(true))).getOrElse(Nil)
}
