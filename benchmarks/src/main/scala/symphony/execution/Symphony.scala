package symphony.execution

import symphony.{ SymphonyQL, SymphonyQLResolver }
import symphony.execution.Data.*
import symphony.schema.javadsl.ObjectBuilder

import scala.concurrent.Future

object Symphony {
  import symphony.schema.*

  case class CharactersArgs(origin: Option[Origin])

  case class CharacterArgs(name: String)

  case class Query(
    characters: CharactersArgs => Future[List[Character]],
    character: CharacterArgs => Future[Option[Character]]
  )

  val graphql: SymphonyQL = SymphonyQL
    .newSymphonyQL()
    .addQuery(
      Query(
        args => Future.successful(Data.characters.filter(c => args.origin.forall(c.origin == _))),
        args => Future.successful(Data.characters.find(c => c.name == args.name))
      ),
      Schema.derived[Query]
    )
    .build()
}
