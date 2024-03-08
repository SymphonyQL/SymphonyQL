package symphony.schema

import scala.annotation.tailrec
import symphony.parser.adt.*
import symphony.parser.adt.introspection.*

object Types {

  def mkList(underlying: __Type): __Type =
    __Type(__TypeKind.LIST, ofType = Some(underlying))

  def mkNonNull(underlying: __Type): __Type =
    __Type(__TypeKind.NON_NULL, ofType = Some(underlying))

  def mkScalar(
    name: String,
    description: Option[String] = None,
    specifiedBy: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): __Type =
    __Type(__TypeKind.SCALAR, Some(name), description, specifiedBy = specifiedBy, directives = directives)

  val boolean: __Type = mkScalar("Boolean")
  val string: __Type  = mkScalar("String")
  val int: __Type     = mkScalar("Int")
  val long: __Type    = mkScalar("Long")
  val float: __Type   = mkScalar("Float")
  val double: __Type  = mkScalar("Double")

  def mkEnum(
    name: Option[String],
    description: Option[String],
    values: List[__EnumValue],
    origin: Option[String],
    directives: Option[List[Directive]] = None
  ): __Type =
    __Type(
      __TypeKind.ENUM,
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
    fields: List[__Field],
    directives: List[Directive],
    origin: Option[String] = None,
    interfaces: () => Option[List[__Type]] = () => Some(Nil)
  ): __Type =
    __Type(
      __TypeKind.OBJECT,
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
    arguments: List[__InputValue],
    `type`: () => __Type,
    isDeprecated: Boolean = false,
    deprecationReason: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): __Field =
    __Field(
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
    fields: List[__InputValue],
    origin: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): __Type =
    __Type(
      __TypeKind.INPUT_OBJECT,
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
    subTypes: List[__Type],
    origin: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): __Type =
    __Type(
      __TypeKind.UNION,
      name,
      description,
      possibleTypes = Some(subTypes),
      origin = origin,
      directives = directives
    )

  def mkInterface(
    name: Option[String],
    description: Option[String],
    fields: () => List[__Field],
    subTypes: List[__Type],
    origin: Option[String] = None,
    directives: Option[List[Directive]] = None
  ): __Type =
    __Type(
      __TypeKind.INTERFACE,
      name,
      description,
      fields =
        args => if (args.includeDeprecated.getOrElse(false)) Some(fields()) else Some(fields().filter(!_.isDeprecated)),
      possibleTypes = Some(subTypes),
      origin = origin,
      directives = directives
    )

  def collectTypes(t: __Type, existingTypes: List[__Type] = Nil): List[__Type] =
    t.kind match {
      case __TypeKind.SCALAR | __TypeKind.ENUM   =>
        t.name.fold(existingTypes)(_ => if (existingTypes.exists(same(t, _))) existingTypes else t :: existingTypes)
      case __TypeKind.LIST | __TypeKind.NON_NULL =>
        t.ofType.fold(existingTypes)(collectTypes(_, existingTypes))
      case _                                     =>
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
          t.allFields.flatMap(f => f.`type` :: f.allArgs.map(_.`type`)) ++
            t.allInputFields.map(_.`type`) ++
            t.interfaces().getOrElse(Nil).map(() => _)
        val list2         = embeddedTypes.foldLeft(list1) { case (types, f) =>
          val t = innerType(f())
          t.name.fold(types)(_ => if (existingTypes.exists(same(t, _))) types else collectTypes(t, types))
        }
        t.possibleTypes.getOrElse(Nil).foldLeft(list2) { case (types, subtype) => collectTypes(subtype, types) }
    }

  @tailrec
  def same(t1: __Type, t2: __Type): Boolean =
    if (t1.kind == t2.kind && t1.ofType.nonEmpty)
      same(t1.ofType.getOrElse(t1), t2.ofType.getOrElse(t2))
    else
      t1.name == t2.name && t1.kind == t2.kind && (t1.origin.isEmpty || t2.origin.isEmpty || t1.origin == t2.origin)

  def innerType(t: __Type): __Type = t.ofType.fold(t)(innerType)

  def listOf(t: __Type): Option[__Type] =
    t.kind match {
      case __TypeKind.LIST     => t.ofType
      case __TypeKind.NON_NULL => t.ofType.flatMap(listOf)
      case _                   => None
    }

  def name(t: __Type): String =
    (t.kind match {
      case __TypeKind.LIST     => t.ofType.map("ListOf" + name(_))
      case __TypeKind.NON_NULL => t.ofType.map(name)
      case _                   => t.name
    }).getOrElse("")

  def unify(l: List[__Type]): Option[__Type]        =
    l.headOption.flatMap(first => l.drop(1).foldLeft(Option(first))((acc, t) => acc.flatMap(unify(_, t))))

    /**
     * Tries to unify two types by widening them to a common supertype.
     */
  def unify(t1: __Type, t2: __Type): Option[__Type] =
    if (same(t1, t2)) Option(t1)
    else
      (t1.kind, t2.kind) match {
        case (__TypeKind.NON_NULL, _) => t1.ofType.flatMap(unify(_, t2))
        case (_, __TypeKind.NON_NULL) => t2.ofType.flatMap(unify(_, t1))
        case _                        => None
      }
}
