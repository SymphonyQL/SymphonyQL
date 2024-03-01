---
title: Quick Start - Scala
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/quickstart-scala.md
---

This guide gets you started with SymphonyQL with a simple working example on Scala.

Assuming we want to develop an application for the GraphQL Schema below:
```graphql
schema {
  query: Queries
}

enum Origin {
  EARTH
  MARS
  BELT
}

input NestedArgInput {
  id: String!
  name: String
}

type CharacterOutput {
  name: String!
  origin: Origin!
}

type Queries {
  characters(origin: Origin, nestedArg: NestedArgInput): [CharacterOutput!]
}
```

Similarly, in Scala, you only need to use **case class** to define the schema and no longer need annotations.
```scala
enum Origin {
  case EARTH, MARS, BELT
}

case class Character(name: String, origin: Origin)
case class FilterArgs(origin: Option[Origin])
case class NestedArg(id: String, name: Optional[String])
case class Queries(characters: FilterArgs => Source[Character, NotUsed])
```

SymphonyQL automatically generates schemas during compilation:
```scala
def main(args: Array[String]): Unit = {
    val graphql: SymphonyQL = SymphonyQL
    .newSymphonyQL()
    .addQuery(
      Queries(args =>
        Source.single(
          Character("hello-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT))
        )
      ),
      Schema.derived[Queries]
    )
    .build()
    
    val characters =
    """{
      |  characters(origin: "MARS") {
      |    name
      |    origin
      |  }
      |}""".stripMargin
      
    implicit val actorSystem: ActorSystem                   = ActorSystem("symphonyActorSystem")
    val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(characters)))
}
```

`Schema.derived[Queries]` is an inline call by metaprogramming.