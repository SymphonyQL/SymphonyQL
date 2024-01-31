package symphony

import symphony.parser.Value.StringValue
import symphony.schema.*
import symphony.schema.builder.*

object TestSchema {

  // Schema DSL
  /** {{{
   *  input UserQueryInput {
   *      id: String!
   *   }
   *  }}}
   */
  val queryInputSchema: Schema[UserQueryInput] = InputObjectBuilder
    .builder[UserQueryInput]()
    .name("UserQueryInput")
    .fields(builder => builder.name("id").schema(Schema.string).build())
    .build()

  /** {{{
   *   input UserUpdateInput {
   *      username: String
   *      sex: Int!
   *   }
   *  }}}
   */
  val updateInputSchema: Schema[UserUpdateInput] = InputObjectBuilder
    .builder[UserUpdateInput]()
    .name("UserUpdateInput")
    .fields(
      builder => builder.name("username").isOptional(true).schema(Schema.string).build(),
      builder => builder.name("sex").schema(Schema.int).build()
    )
    .isOptional(true)
    .build()

  /** {{{
   *   type UserOutput {
   *      id: String!
   *      username: String
   *   }
   *  }}}
   */
  val outputSchema: Schema[UserOutput] = ObjectBuilder
    .builder[UserOutput]()
    .name("UserOutput")
    .fieldWithArgs(
      builder => builder.name("id").schema(Schema.string).build() -> ((a: UserOutput) => ExecutionPlan.NullPlan),
      builder =>
        builder.name("username").isOptional(true).schema(Schema.string).build() -> ((a: UserOutput) =>
          ExecutionPlan.NullPlan
        )
    )
    .build()

  // ===============================================Resolver=========================================================
  /** {{{
   *   type UserQueryResolver {
   *      batchGetUsers(users: [UserQueryInput!]!): [UserOutput!]!
   *      getUsers(user: UserQueryInput!): UserOutput!
   *   }
   *  }}}
   */
  val querySchema: Schema[UserQueryResolver] = ObjectBuilder
    .builder[UserQueryResolver]()
    .name("UserQueryResolver")
    .fieldWithArgs(builder =>
      builder
        .name("getUsers")
        .schema(outputSchema)
        .args(
          "user" -> queryInputSchema
        )
        .build() ->
        ((a: UserQueryResolver) =>
          ExecutionPlan.ObjectDataPlan( // TODO implement ExecutionPlan
            "UserQueryResolver",
            Map(
              "getUsers" -> ExecutionPlan.ObjectDataPlan(
                "UserOutput",
                Map(
                  "id"       -> ExecutionPlan.PureDataPlan(StringValue("id")),
                  "username" -> ExecutionPlan.PureDataPlan(StringValue("symphony"))
                )
              )
            )
          )
        )
    )
    .build()

  val batchQuerySchema: Schema[UserBatchQueryResolver] = ObjectBuilder
    .builder[UserBatchQueryResolver]()
    .name("UserBatchQueryResolver")
    .fieldWithArgs(builder =>
      builder
        .name("batchGetUsers")
        .schema(Schema.mkList(outputSchema))
        .args(
          "users" -> Schema.mkList(queryInputSchema)
        )
        .build() ->
        ((a: UserBatchQueryResolver) =>
          ExecutionPlan.ObjectDataPlan(
            "UserBatchQueryResolver",
            Map(
              "batchGetUsers" ->
                ExecutionPlan.ListDataPlan(
                  List(
                    ExecutionPlan.ObjectDataPlan(
                      "UserOutput",
                      Map(
                        "id"       -> ExecutionPlan.PureDataPlan(StringValue("id")),
                        "username" -> ExecutionPlan.PureDataPlan(StringValue("symphony"))
                      )
                    )
                  )
                )
            )
          )
        )
    )
    .build()

  /** {{{
   *     type UserMutationResolver {
   *        updateUser(id: String!, user: UserUpdateInput): UserOutput
   *     }
   *  }}}
   */
  val mutationSchema: Schema[UserMutationResolver] = ObjectBuilder
    .builder[UserMutationResolver]()
    .name("UserMutationResolver")
    .fieldWithArgs(builder =>
      builder
        .name("updateUser")
        .schema(outputSchema)
        .isOptional(true)
        .args(
          "id" -> Schema.string,
          // we should use `Option[T]`, this is a unsafe way, it should only be available to Java.
          "user" -> Schema.mkNullable(updateInputSchema)
        )
        .build() ->
        ((a: UserMutationResolver) =>
          ExecutionPlan.ObjectDataPlan(
            "UserMutationResolver",
            Map(
              "updateUser" -> ExecutionPlan.ObjectDataPlan(
                "UserOutput",
                Map(
                  "id"       -> ExecutionPlan.PureDataPlan(StringValue("id")),
                  "username" -> ExecutionPlan.PureDataPlan(StringValue("symphony"))
                )
              )
            )
          )
        )
    )
    .build()
}
