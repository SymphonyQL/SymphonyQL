package symphony.parser

import adt.LocationInfo

/** Maps an index to the "friendly" version of an index based on the underlying source.
 */
trait SourceMapper extends Serializable {

  def getLocation(index: Int): LocationInfo

  def size: Option[Int] = None

}

object SourceMapper {
  val empty: SourceMapper = EmptySourceMapper

  private case object EmptySourceMapper extends SourceMapper {
    def getLocation(index: Int): LocationInfo = LocationInfo.origin
  }
}
