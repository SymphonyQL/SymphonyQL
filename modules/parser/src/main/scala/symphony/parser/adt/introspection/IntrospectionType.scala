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

final case class IntrospectionType(
  kind: TypeKind,
  name: Option[String] = None,
  description: Option[String] = None,
  fields: DeprecatedArgs => Option[List[IntrospectionField]] = _ => None,
  interfaces: () => Option[List[IntrospectionType]] = () => None,
  possibleTypes: Option[List[IntrospectionType]] = None,
  enumValues: DeprecatedArgs => Option[List[IntrospectionEnumValue]] = _ => None,
  inputFields: DeprecatedArgs => Option[List[IntrospectionInputValue]] = _ => None,
  ofType: Option[IntrospectionType] = None,
  specifiedBy: Option[String] = None,
  directives: Option[List[Directive]] = None,
  origin: Option[String] = None
) { self =>
  override lazy val hashCode: Int = super.hashCode()

  @targetName("add")
  def ++(that: IntrospectionType): IntrospectionType = IntrospectionType(
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
          case TypeKind.LIST     => ListType(of.toType(), nonNull)
          case TypeKind.NON_NULL => of.toType(true)
          case _                 => NamedType(name.getOrElse(""), nonNull)
        }
      case None     => NamedType(name.getOrElse(""), nonNull)
    }

  def toTypeDefinition: Option[TypeDefinition] =
    kind match {
      case TypeKind.SCALAR       =>
        Some(
          ScalarTypeDefinition(
            description,
            name.getOrElse(""),
            directives
              .getOrElse(Nil) ++
              specifiedBy
                .map(url => Directive("specifiedBy", Map("url" -> StringValue(url)), directives.size))
                .toList
          )
        )
      case TypeKind.OBJECT       =>
        Some(
          ObjectTypeDefinition(
            description,
            name.getOrElse(""),
            interfaces().getOrElse(Nil).map(t => NamedType(t.name.getOrElse(""), nonNull = false)),
            directives.getOrElse(Nil),
            allFields.map(_.toFieldDefinition)
          )
        )
      case TypeKind.INTERFACE    =>
        Some(
          InterfaceTypeDefinition(
            description,
            name.getOrElse(""),
            interfaces().getOrElse(Nil).map(t => NamedType(t.name.getOrElse(""), nonNull = false)),
            directives.getOrElse(Nil),
            allFields.map(_.toFieldDefinition)
          )
        )
      case TypeKind.UNION        =>
        Some(
          UnionTypeDefinition(
            description,
            name.getOrElse(""),
            directives.getOrElse(Nil),
            possibleTypes.getOrElse(Nil).flatMap(_.name)
          )
        )
      case TypeKind.ENUM         =>
        Some(
          EnumTypeDefinition(
            description,
            name.getOrElse(""),
            directives.getOrElse(Nil),
            enumValues(DeprecatedArgs(Some(true))).getOrElse(Nil).map(_.toEnumValueDefinition)
          )
        )
      case TypeKind.INPUT_OBJECT =>
        Some(
          InputObjectTypeDefinition(
            description,
            name.getOrElse(""),
            directives.getOrElse(Nil),
            allInputFields.map(_.toInputValueDefinition)
          )
        )
      case _                     => None
    }

  lazy val list: IntrospectionType = IntrospectionType(TypeKind.LIST, ofType = Some(self))

  lazy val nonNull: IntrospectionType = IntrospectionType(TypeKind.NON_NULL, ofType = Some(self))

  lazy val allFields: List[IntrospectionField] =
    fields(DeprecatedArgs(Some(true))).getOrElse(Nil)

  lazy val allInputFields: List[IntrospectionInputValue] =
    inputFields(DeprecatedArgs(Some(true))).getOrElse(Nil)
}
