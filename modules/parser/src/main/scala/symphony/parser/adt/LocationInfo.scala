package symphony
package parser
package adt

import OutputValue.ObjectValue
import Value.*

final case class LocationInfo(column: Int, line: Int) {

  def toOutputValue: OutputValue =
    ObjectValue(List("line" -> IntValue(line), "column" -> IntValue(column)))
}

object LocationInfo {
  val origin: LocationInfo = LocationInfo(0, 0)
}
