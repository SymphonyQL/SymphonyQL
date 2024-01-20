package symphony
package parser
package adt

sealed trait OperationType extends Serializable

object OperationType {
  case object Query        extends OperationType
  case object Mutation     extends OperationType
  case object Subscription extends OperationType
}
