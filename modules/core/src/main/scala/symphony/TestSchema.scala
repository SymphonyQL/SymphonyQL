package symphony

import symphony.parser.SymphonyQLValue.StringValue
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
      builder => builder.name("id").schema(Schema.string).build() -> ((a: UserOutput) => Stage.NullStage),
      builder =>
        builder.name("username").isOptional(true).schema(Schema.string).build() -> ((a: UserOutput) => Stage.NullStage)
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
          Stage.ObjectStage( // TODO implement ExecutionStage
            "UserQueryResolver",
            Map(
              "getUsers" -> Stage.ObjectStage(
                "UserOutput",
                Map(
                  "id"       -> Stage.PureStage(StringValue("id")),
                  "username" -> Stage.PureStage(StringValue("symphony"))
                )
              )
            )
          )
        )
    )
    .build()

  val batchQuerySchema: Schema[UserQueryResolver] = ObjectBuilder
    .builder[UserQueryResolver]()
    .name("UserQueryResolver")
    .fieldWithArgs(builder =>
      builder
        .name("batchGetUsers")
        .schema(Schema.mkList(outputSchema))
        .args(
          "users" -> Schema.mkList(queryInputSchema)
        )
        .build() ->
        ((a: UserQueryResolver) =>
          Stage.ObjectStage(
            "UserBatchQueryResolver",
            Map(
              "batchGetUsers" ->
                Stage.ListStage(
                  List(
                    Stage.ObjectStage(
                      "UserOutput",
                      Map(
                        "id"       -> Stage.PureStage(StringValue("id")),
                        "username" -> Stage.PureStage(StringValue("symphony"))
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
          Stage.ObjectStage(
            "UserMutationResolver",
            Map(
              "updateUser" -> Stage.ObjectStage(
                "UserOutput",
                Map(
                  "id"       -> Stage.PureStage(StringValue("id")),
                  "username" -> Stage.PureStage(StringValue("symphony"))
                )
              )
            )
          )
        )
    )
    .build()
}
