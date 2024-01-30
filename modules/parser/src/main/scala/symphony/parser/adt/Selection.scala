package symphony
package parser
package adt

import Type.*

sealed trait Selection extends Serializable {
  @transient final override lazy val hashCode: Int = super.hashCode()
}

object Selection {

  final case class Field(
    alias: Option[String],
    name: String,
    arguments: Map[String, InputValue],
    directives: List[Directive],
    selectionSet: List[Selection]
  ) extends Selection

  final case class FragmentSpread(name: String, directives: List[Directive]) extends Selection

  final case class InlineFragment(
    typeCondition: Option[NamedType],
    dirs: List[Directive],
    selectionSet: List[Selection]
  ) extends Selection

}
