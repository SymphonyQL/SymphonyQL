package symphony

import symphony.parser.SymphonyQLValue.*

final case class UserUpdateInput(username: String, sex: Int)

final case class UserQueryInput(id: String)

final case class UserOutput(id: String, username: String)

final case class UserMutationResolver(
  updateUser: (String, UserUpdateInput) => UserOutput
)

final case class UserQueryResolver(
  getUsers: UserQueryInput => UserOutput,
  batchGetUsers: List[UserQueryInput] => List[UserOutput]
)

object TestSchemaApp extends App {
  import TestSchema.*

  // SchemaDerivationGen[UserQueryResolver].gen
  val graphql: SymphonyQL = SymphonyQL
    .builder()
    .rootResolver(
      SymphonyQLResolver(
        UserQueryResolver(_ => UserOutput("a1", "b1"), _ => List(UserOutput("a2", "b2"))) -> querySchema,
        UserMutationResolver((_, _) => UserOutput("aaaa", "bbbb"))                        -> mutationSchema
      )
    )
    .build()

  println(graphql.render)

}
