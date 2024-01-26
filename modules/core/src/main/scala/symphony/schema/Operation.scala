package symphony.schema

import symphony.parser.introspection.__Type

final case class Operation(opType: __Type, executionPlan: ExecutionPlan)
