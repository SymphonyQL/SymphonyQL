package symphony.parser.adt.introspection

import symphony.parser.SymphonyQLValue.StringValue
import symphony.parser.adt.Definition.TypeSystemDefinition.TypeDefinition.*
import symphony.parser.adt.Directive

final case class IntrospectionField(
  name: String,
  description: Option[String],
  args: DeprecatedArgs => List[IntrospectionInputValue],
  tpe: () => IntrospectionType,
  isDeprecated: Boolean = false,
  deprecationReason: Option[String] = None,
  directives: Option[List[Directive]] = None
) {
  override lazy val hashCode: Int = super.hashCode()

  def toFieldDefinition: FieldDefinition = {
    val allDirectives = (if (isDeprecated)
                           List(
                             Directive(
                               "deprecated",
                               List(deprecationReason.map(reason => "reason" -> StringValue(reason))).flatten.toMap
                             )
                           )
                         else Nil) ++ directives.getOrElse(Nil)
    FieldDefinition(description, name, allArgs.map(_.toInputValueDefinition), _type.toType(), allDirectives)
  }

  def toInputValueDefinition: InputValueDefinition =
    InputValueDefinition(description, name, _type.toType(), None, directives.getOrElse(Nil))

  lazy val allArgs: List[IntrospectionInputValue] =
    args(DeprecatedArgs(Some(true)))

  private[symphony] lazy val _type: IntrospectionType = tpe()

}
