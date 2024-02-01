package symphony.parser

import scala.collection.mutable.ArrayBuffer

import adt.LocationInfo

/**
 * Maps an index to the "friendly" version of an index based on the underlying source.
 */
trait SourceMapper extends Serializable {

  def getLocation(index: Int): LocationInfo

  def size: Option[Int] = None

}

object SourceMapper {
  val empty: SourceMapper = EmptySourceMapper

  def apply(source: String): SourceMapper = DefaultSourceMapper(source)

  def lookupLineNumber(data: String): Array[Int] = {
    val lineStarts = ArrayBuffer[Int](0)
    var i          = 0
    var col        = 1
    // Stores the previous char we saw, or -1 if we just saw a \r\n or \n\r pair
    var state: Int = 0
    while (i < data.length) {
      val char = data(i)
      if (char == '\r' && state == '\n' || char == '\n' && state == '\r') {
        col += 1
        state = -1
      } else if (state == '\r' || state == '\n' || state == -1) {
        lineStarts.append(i)
        col = 1
        state = char
      } else {
        col += 1
        state = char
      }

      i += 1
    }

    if (state == '\r' || state == '\n' || state == -1) {
      lineStarts.append(i)
    }

    lineStarts.toArray
  }

  private final case class DefaultSourceMapper(source: String) extends SourceMapper {
    private lazy val lineNumberLookup = lookupLineNumber(source)

    def getLocation(index: Int): LocationInfo = {
      val line = lineNumberLookup.indexWhere(_ > index) match {
        case -1 => lineNumberLookup.length - 1
        case n  => 0 max (n - 1)
      }

      val col = index - lineNumberLookup(line)
      LocationInfo(column = col + 1, line = line + 1)
    }

    override def size: Option[Int] = Some(source.length)
  }

  private case object EmptySourceMapper extends SourceMapper {
    def getLocation(index: Int): LocationInfo = LocationInfo.origin
  }
}
