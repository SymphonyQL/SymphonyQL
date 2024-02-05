package symphony.execution

import caliban.*
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Schema.auto.*
import symphony.execution.Data.*
import zio.{ Scope as _, * }

object Caliban {
  import caliban.schema.Schema

  private val runtime = Runtime.default

  case class CharactersArgs(origin: Option[Origin])
  case class CharacterArgs(name: String)

  case class Query(
    characters: CharactersArgs => UIO[List[Character]],
    character: CharacterArgs => UIO[Option[Character]]
  )

  implicit val originSchema: Schema[Any, Origin]       = Schema.gen
  implicit val characterSchema: Schema[Any, Character] = Schema.gen

  val resolver: RootResolver[Query, Unit, Unit] = RootResolver(
    Query(
      args => ZIO.succeed(Data.characters.filter(c => args.origin.forall(c.origin == _))),
      args => ZIO.succeed(Data.characters.find(c => c.name == args.name))
    )
  )

  val gql: GraphQL[Any]                                  = graphQL(resolver)
  val interpreter: GraphQLInterpreter[Any, CalibanError] = run(gql.interpreter)
  def run[A](zio: Task[A]): A                            = Unsafe.unsafe(implicit u => runtime.unsafe.run(zio).getOrThrow())
}
