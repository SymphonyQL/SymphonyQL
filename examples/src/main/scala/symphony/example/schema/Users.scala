package symphony.example.schema

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.*

object Users {
  enum Origin {
    case EARTH, MARS, BELT
  }

  case class Character(name: String, origin: Origin)

  case class FilterArgs(origin: Option[Origin])

  case class Queries(characters: FilterArgs => Source[Character, NotUsed])
}
