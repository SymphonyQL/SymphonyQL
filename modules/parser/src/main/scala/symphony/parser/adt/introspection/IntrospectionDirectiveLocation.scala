package symphony.parser.adt.introspection

import symphony.parser.adt.*
import symphony.parser.adt.Definition.TypeSystemDefinition.*
import symphony.parser.adt.Definition.TypeSystemDefinition.DirectiveLocation.*

sealed trait IntrospectionDirectiveLocation { self =>

  def toDirectiveLocation: DirectiveLocation =
    self match {
      case IntrospectionDirectiveLocation.QUERY                  => ExecutableDirectiveLocation.QUERY
      case IntrospectionDirectiveLocation.MUTATION               => ExecutableDirectiveLocation.MUTATION
      case IntrospectionDirectiveLocation.SUBSCRIPTION           => ExecutableDirectiveLocation.SUBSCRIPTION
      case IntrospectionDirectiveLocation.FIELD                  => ExecutableDirectiveLocation.FIELD
      case IntrospectionDirectiveLocation.FRAGMENT_DEFINITION    => ExecutableDirectiveLocation.FRAGMENT_DEFINITION
      case IntrospectionDirectiveLocation.FRAGMENT_SPREAD        => ExecutableDirectiveLocation.FRAGMENT_SPREAD
      case IntrospectionDirectiveLocation.INLINE_FRAGMENT        => ExecutableDirectiveLocation.INLINE_FRAGMENT
      case IntrospectionDirectiveLocation.SCHEMA                 => TypeSystemDirectiveLocation.SCHEMA
      case IntrospectionDirectiveLocation.SCALAR                 => TypeSystemDirectiveLocation.SCALAR
      case IntrospectionDirectiveLocation.OBJECT                 => TypeSystemDirectiveLocation.OBJECT
      case IntrospectionDirectiveLocation.FIELD_DEFINITION       => TypeSystemDirectiveLocation.FIELD_DEFINITION
      case IntrospectionDirectiveLocation.ARGUMENT_DEFINITION    => TypeSystemDirectiveLocation.ARGUMENT_DEFINITION
      case IntrospectionDirectiveLocation.INTERFACE              => TypeSystemDirectiveLocation.INTERFACE
      case IntrospectionDirectiveLocation.UNION                  => TypeSystemDirectiveLocation.UNION
      case IntrospectionDirectiveLocation.ENUM                   => TypeSystemDirectiveLocation.ENUM
      case IntrospectionDirectiveLocation.ENUM_VALUE             => TypeSystemDirectiveLocation.ENUM_VALUE
      case IntrospectionDirectiveLocation.INPUT_OBJECT           => TypeSystemDirectiveLocation.INPUT_OBJECT
      case IntrospectionDirectiveLocation.INPUT_FIELD_DEFINITION => TypeSystemDirectiveLocation.INPUT_FIELD_DEFINITION
      case IntrospectionDirectiveLocation.VARIABLE_DEFINITION    => TypeSystemDirectiveLocation.VARIABLE_DEFINITION
    }
}

object IntrospectionDirectiveLocation {
  case object QUERY                  extends IntrospectionDirectiveLocation
  case object MUTATION               extends IntrospectionDirectiveLocation
  case object SUBSCRIPTION           extends IntrospectionDirectiveLocation
  case object FIELD                  extends IntrospectionDirectiveLocation
  case object FRAGMENT_DEFINITION    extends IntrospectionDirectiveLocation
  case object FRAGMENT_SPREAD        extends IntrospectionDirectiveLocation
  case object INLINE_FRAGMENT        extends IntrospectionDirectiveLocation
  case object SCHEMA                 extends IntrospectionDirectiveLocation
  case object SCALAR                 extends IntrospectionDirectiveLocation
  case object OBJECT                 extends IntrospectionDirectiveLocation
  case object FIELD_DEFINITION       extends IntrospectionDirectiveLocation
  case object ARGUMENT_DEFINITION    extends IntrospectionDirectiveLocation
  case object INTERFACE              extends IntrospectionDirectiveLocation
  case object UNION                  extends IntrospectionDirectiveLocation
  case object ENUM                   extends IntrospectionDirectiveLocation
  case object ENUM_VALUE             extends IntrospectionDirectiveLocation
  case object INPUT_OBJECT           extends IntrospectionDirectiveLocation
  case object INPUT_FIELD_DEFINITION extends IntrospectionDirectiveLocation
  case object VARIABLE_DEFINITION    extends IntrospectionDirectiveLocation
}
