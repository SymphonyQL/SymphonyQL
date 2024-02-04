package symphony.schema

import scala.annotation.tailrec

import symphony.parser.adt.*
import symphony.parser.adt.introspection.*

object Types {

  def mkList(underlying: IntrospectionType): IntrospectionType =
    IntrospectionType(TypeKind.LIST, ofType = Some(underlying))

  def mkNonNull(underlying: IntrospectionType): IntrospectionType =
    IntrospectionType(TypeKind.NON_NULL, ofType = Some(underlying))

  def mkScalar(
    name: String,
    description: Option[String] = None,
    specifiedBy: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): IntrospectionType =
    IntrospectionType(TypeKind.SCALAR, Some(name), description, specifiedBy = specifiedBy, directives = directives)

  val boolean: IntrospectionType = mkScalar("Boolean")
  val string: IntrospectionType  = mkScalar("String")
  val int: IntrospectionType     = mkScalar("Int")
  val long: IntrospectionType    = mkScalar("Long")
  val float: IntrospectionType   = mkScalar("Float")
  val double: IntrospectionType  = mkScalar("Double")

  def mkEnum(
    name: Option[String],
    description: Option[String],
    values: List[IntrospectionEnumValue],
    origin: Option[String],
    directives: Option[List[Directive]] = None
  ): IntrospectionType =
    IntrospectionType(
      TypeKind.ENUM,
      name,
      description,
      enumValues =
        args => if (args.includeDeprecated.getOrElse(false)) Some(values) else Some(values.filter(!_.isDeprecated)),
      origin = origin,
      directives = directives
    )

  def mkObject(
    name: Option[String],
    description: Option[String],
    fields: List[IntrospectionField],
    directives: List[Directive],
    origin: Option[String] = None,
    interfaces: () => Option[List[IntrospectionType]] = () => Some(Nil)
  ): IntrospectionType =
    IntrospectionType(
      TypeKind.OBJECT,
      name,
      description,
      fields =
        args => if (args.includeDeprecated.getOrElse(false)) Some(fields) else Some(fields.filter(!_.isDeprecated)),
      interfaces = interfaces,
      directives = Some(directives),
      origin = origin
    )

  def mkField(
    name: String,
    description: Option[String],
    arguments: List[IntrospectionInputValue],
    `type`: () => IntrospectionType,
    isDeprecated: Boolean = false,
    deprecationReason: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): IntrospectionField =
    IntrospectionField(
      name,
      description,
      args =>
        if (args.includeDeprecated.getOrElse(false)) arguments
        else arguments.filter(!_.isDeprecated),
      `type`,
      isDeprecated,
      deprecationReason,
      directives
    )

  def mkInputObject(
    name: Option[String],
    description: Option[String],
    fields: List[IntrospectionInputValue],
    origin: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): IntrospectionType =
    IntrospectionType(
      TypeKind.INPUT_OBJECT,
      name,
      description,
      inputFields = args =>
        if (args.includeDeprecated.getOrElse(false)) Some(fields)
        else Some(fields.filter(!_.isDeprecated)),
      origin = origin,
      directives = directives
    )

  def mkUnion(
    name: Option[String],
    description: Option[String],
    subTypes: List[IntrospectionType],
    origin: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): IntrospectionType =
    IntrospectionType(
      TypeKind.UNION,
      name,
      description,
      possibleTypes = Some(subTypes),
      origin = origin,
      directives = directives
    )

  def mkInterface(
    name: Option[String],
    description: Option[String],
    fields: () => List[IntrospectionField],
    subTypes: List[IntrospectionType],
    origin: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): IntrospectionType =
    IntrospectionType(
      TypeKind.INTERFACE,
      name,
      description,
      fields =
        args => if (args.includeDeprecated.getOrElse(false)) Some(fields()) else Some(fields().filter(!_.isDeprecated)),
      possibleTypes = Some(subTypes),
      origin = origin,
      directives = directives
    )

  def collectTypes(t: IntrospectionType, existingTypes: List[IntrospectionType] = Nil): List[IntrospectionType] =
    t.kind match {
      case TypeKind.SCALAR | TypeKind.ENUM   =>
        t.name.fold(existingTypes)(_ => if (existingTypes.exists(same(t, _))) existingTypes else t :: existingTypes)
      case TypeKind.LIST | TypeKind.NON_NULL =>
        t.ofType.fold(existingTypes)(collectTypes(_, existingTypes))
      case _                                 =>
        val list1         =
          t.name.fold(existingTypes)(_ =>
            if (existingTypes.exists(same(t, _))) {
              existingTypes.map {
                case ex if same(ex, t) =>
                  ex.copy(interfaces =
                    () =>
                      (ex.interfaces(), t.interfaces()) match {
                        case (None, None)              => None
                        case (Some(interfaces), None)  => Some(interfaces)
                        case (None, Some(interfaces))  => Some(interfaces)
                        case (Some(left), Some(right)) =>
                          Some(left ++ right.filterNot(t => left.exists(_.name == t.name)))
                      }
                  )
                case other             => other
              }
            } else t :: existingTypes
          )
        val embeddedTypes =
          t.allFields.flatMap(f => f.tpe :: f.allArgs.map(_.tpe)) ++
            t.allInputFields.map(_.tpe) ++
            t.interfaces().getOrElse(Nil).map(() => _)
        val list2         = embeddedTypes.foldLeft(list1) { case (types, f) =>
          val t = innerType(f())
          t.name.fold(types)(_ => if (existingTypes.exists(same(t, _))) types else collectTypes(t, types))
        }
        t.possibleTypes.getOrElse(Nil).foldLeft(list2) { case (types, subtype) => collectTypes(subtype, types) }
    }

  @tailrec
  def same(t1: IntrospectionType, t2: IntrospectionType): Boolean =
    if (t1.kind == t2.kind && t1.ofType.nonEmpty)
      same(t1.ofType.getOrElse(t1), t2.ofType.getOrElse(t2))
    else
      t1.name == t2.name && t1.kind == t2.kind && (t1.origin.isEmpty || t2.origin.isEmpty || t1.origin == t2.origin)

  def innerType(t: IntrospectionType): IntrospectionType = t.ofType.fold(t)(innerType)

  def listOf(t: IntrospectionType): Option[IntrospectionType] =
    t.kind match {
      case TypeKind.LIST     => t.ofType
      case TypeKind.NON_NULL => t.ofType.flatMap(listOf)
      case _                 => None
    }

  def name(t: IntrospectionType): String =
    (t.kind match {
      case TypeKind.LIST     => t.ofType.map("ListOf" + name(_))
      case TypeKind.NON_NULL => t.ofType.map(name)
      case _                 => t.name
    }).getOrElse("")
}
