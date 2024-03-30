package symphony.parser

import adt.LocationInfo
import org.parboiled2.Position

trait SourceMapper extends Serializable {

  def getLocation(position: Position): LocationInfo

  def size: Option[Int] = None

}

object SourceMapper {
  val empty: SourceMapper = EmptySourceMapper

  def apply(source: String): SourceMapper = DefaultSourceMapper(source)

  private final case class DefaultSourceMapper(source: String) extends SourceMapper {
    def getLocation(position: Position): LocationInfo =
      LocationInfo(column = position.column, line = position.line)

    override def size: Option[Int] = Some(source.length)
  }

  private case object EmptySourceMapper extends SourceMapper {
    def getLocation(position: Position): LocationInfo = LocationInfo.origin
  }
}
