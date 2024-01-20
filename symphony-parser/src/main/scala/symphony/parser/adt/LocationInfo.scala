package symphony
package parser
package adt

import value.ResponseValue
import value.ResponseValue.ObjectValue
import value.Value.*

final case class LocationInfo(column: Int, line: Int) {

  def toResponseValue: ResponseValue =
    ObjectValue(List("line" -> IntValue(line), "column" -> IntValue(column)))
}

object LocationInfo {
  val origin: LocationInfo = LocationInfo(0, 0)
}
