package example.schema

import symphony.parser.*
import symphony.parser.SymphonyQLValue.StringValue
import symphony.schema.*
import symphony.schema.builder.*

val singleArgumentExtractor: ArgumentExtractor[UserQueryInput] = { case SymphonyQLInputValue.ObjectValue(fields) =>
  Right(UserQueryInput(fields("id").asInstanceOf[StringValue].value))
}

// Schema DSL
/**
 * {{{
 *  input UserQueryInput {
 *      id: String!
 *   }
 *  }}}
 */
val queryInputSchema: Schema[UserQueryInput] = InputObjectBuilder
  .builder[UserQueryInput]()
  .name("UserQueryInput")
  .fields(builder =>
    builder
      .name("value")
      .hasArgs(true)
      .isInput(true)
      .schema(
        InputObjectBuilder
          .builder[UserQueryInput => String]()
          .name("UserQueryInput")
          .fields(builder =>
            builder
              .hasArgs(false)
              .name("id")
              .schema(
                Schema.mkFuncSchema(
                  singleArgumentExtractor,
                  InputObjectBuilder
                    .builder[UserQueryInput]()
                    .name("UserQueryInput")
                    .build(),
                  Schema.string
                )
              )
              .build()
          )
          .build()
      )
      .build()
  )
  .build()

/**
 * {{{
 *   type UserOutput {
 *      id: String!
 *      username: String
 *   }
 *  }}}
 */
val outputSchema: Schema[UserOutput] = ObjectBuilder
  .builder[UserOutput]()
  .name("UserOutput")
  .fields(
    builder => builder.name("id").schema(Schema.string).build() -> ((_: UserOutput) => Stage.NullStage),
    builder =>
      builder.name("username").schema(Schema.mkNullable(Schema.string)).build() -> ((_: UserOutput) => Stage.NullStage)
  )
  .build()

// ===============================================Resolver=========================================================
/**
 * {{{
 *   type UserQueryResolver {
 *      getUsers(user: UserQueryInput!): UserOutput!
 *   }
 *  }}}
 */
val querySchema: Schema[UserQueryResolver] = ObjectBuilder
  .builder[UserQueryResolver]()
  .name("UserQueryResolver")
  .fields(builder =>
    builder
      .name("getUsers")
      .hasArgs(true)
      .schema(
        Schema.mkFuncSchema(
          singleArgumentExtractor,
          queryInputSchema,
          outputSchema
        )
      )
      .build() -> (a =>
      Stage.FunctionStage { args =>
        val user =
          a.getUsers(singleArgumentExtractor.extract(SymphonyQLInputValue.ObjectValue(args)).toOption.orNull)
        Stage.ObjectStage(
          "UserOutput",
          Map(
            "id"       -> PureStage(StringValue(user.id)),
            "username" -> PureStage(StringValue(user.username))
          )
        )
      }
    ),
  )
  .build()
