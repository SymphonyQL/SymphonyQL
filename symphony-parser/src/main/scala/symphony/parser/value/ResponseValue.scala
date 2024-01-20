package symphony
package parser
package value

import scala.util.hashing.MurmurHash3

private[symphony] trait ResponseValue extends Serializable

object ResponseValue {

  final case class ListValue(values: List[ResponseValue]) extends ResponseValue {
    override def toString: String = ValueRenderer.responseListValueRenderer.renderCompact(this)
  }

  final case class ObjectValue(fields: List[(String, ResponseValue)]) extends ResponseValue {
    override def toString: String = ValueRenderer.responseObjectValueRenderer.renderCompact(this)

    @transient override lazy val hashCode: Int = MurmurHash3.unorderedHash(fields)

    override def equals(other: Any): Boolean =
      other match {
        case o: ObjectValue => o.hashCode == hashCode
        case _              => false
      }
  }
}
