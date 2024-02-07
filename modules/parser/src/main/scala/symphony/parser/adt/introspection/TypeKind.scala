package symphony.parser.adt.introspection

sealed trait TypeKind

object TypeKind {
  case object SCALAR       extends TypeKind
  case object OBJECT       extends TypeKind
  case object INTERFACE    extends TypeKind
  case object UNION        extends TypeKind
  case object ENUM         extends TypeKind
  case object INPUT_OBJECT extends TypeKind
  case object LIST         extends TypeKind
  case object NON_NULL     extends TypeKind

  implicit val kindOrdering: Ordering[TypeKind] = Ordering.by[TypeKind, Int] {
    case TypeKind.SCALAR       => 1
    case TypeKind.NON_NULL     => 2
    case TypeKind.LIST         => 3
    case TypeKind.UNION        => 4
    case TypeKind.ENUM         => 5
    case TypeKind.INPUT_OBJECT => 6
    case TypeKind.INTERFACE    => 7
    case TypeKind.OBJECT       => 8
  }

  implicit val typeOrdering: Ordering[__Type] =
    Ordering.by(o => (o.kind, o.name.getOrElse("")))
}
