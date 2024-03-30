package symphony
package parser
package adt

import SymphonyQLOutputValue.ObjectValue
import SymphonyQLValue.*

final case class LocationInfo(line: Int, column: Int) {

  def toOutputValue: SymphonyQLOutputValue =
    ObjectValue(List("line" -> IntValue(line), "column" -> IntValue(column)))
}

object LocationInfo {
  val origin: LocationInfo = LocationInfo(0, 0)
}
