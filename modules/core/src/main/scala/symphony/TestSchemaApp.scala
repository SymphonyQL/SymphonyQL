package symphony

import symphony.parser.introspection.*
import symphony.parser.value.Value.*
import symphony.schema.*
import symphony.schema.builder.*

final case class UserUpdateInput(username: String, sex: Int)

final case class UserQueryInput(id: String)

final case class UserOutput(id: String, username: String)

final class UserMutationResolver {
  def updateUser(id: String, user: UserUpdateInput): UserOutput = ???
}

final class UserQueryResolver {
  def getUsers(user: UserQueryInput): UserOutput = ???
}

final class UserBatchQueryResolver {
  def batchGetUsers(users: List[UserQueryInput]): List[UserOutput] = ???
}

object TestSchemaApp extends App {
  import TestSchema.*

  val graphql: SymphonyQL = SymphonyQL
    .builder()
    .addQuery(querySchema, new UserQueryResolver)
    .addMutation(mutationSchema, new UserMutationResolver)
    .addQuery(batchQuerySchema, new UserBatchQueryResolver)
    .build()

  println(graphql.render)

}
