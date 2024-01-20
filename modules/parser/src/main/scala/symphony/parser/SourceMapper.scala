package symphony.parser

import adt.LocationInfo

/** Maps an index to the "friendly" version of an index based on the underlying source.
 */
trait SourceMapper extends Serializable {

  def getLocation(index: Int): LocationInfo

  def size: Option[Int] = None

}
