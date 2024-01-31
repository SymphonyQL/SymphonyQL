package symphony
package parser
package adt

import SymphonyQLOutputValue.ObjectValue
import SymphonyQLValue.*

final case class LocationInfo(column: Int, line: Int) {

  def toOutputValue: SymphonyQLOutputValue =
    ObjectValue(List("line" -> IntValue(line), "column" -> IntValue(column)))
}

object LocationInfo {
  val origin: LocationInfo = LocationInfo(0, 0)
}
