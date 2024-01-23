package symphony
package parser
package value

import scala.util.hashing.MurmurHash3

private[symphony] trait OutputValue extends Serializable

object OutputValue {

  final case class ListValue(values: List[OutputValue]) extends OutputValue {
    override def toString: String = ValueRenderer.outputListValueRenderer.renderCompact(this)
  }

  final case class ObjectValue(fields: List[(String, OutputValue)]) extends OutputValue {
    override def toString: String = ValueRenderer.outputObjectValueRenderer.renderCompact(this)

    @transient override lazy val hashCode: Int = MurmurHash3.unorderedHash(fields)

    override def equals(other: Any): Boolean =
      other match {
        case o: ObjectValue => o.hashCode == hashCode
        case _              => false
      }
  }
}
