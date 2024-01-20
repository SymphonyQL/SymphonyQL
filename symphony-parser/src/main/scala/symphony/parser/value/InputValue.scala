package symphony
package parser
package value

private[symphony] trait InputValue extends Serializable { self =>
  def toInputString: String = ValueRenderer.inputValueRenderer.renderCompact(self)
}

object InputValue {

  final case class ListValue(values: List[InputValue]) extends InputValue {
    override def toString: String      = values.mkString("[", ",", "]")
    override def toInputString: String = ValueRenderer.inputListValueRenderer.render(this)
  }

  final case class ObjectValue(fields: Map[String, InputValue]) extends InputValue {

    override def toString: String =
      fields.map { case (name, value) => s""""$name:${value.toString}"""" }.mkString("{", ",", "}")

    override def toInputString: String = ValueRenderer.inputObjectValueRenderer.render(this)
  }

  final case class VariableValue(name: String) extends InputValue {
    override def toString: String = s"$$$name"
  }

}
