# SymphonyQL

[![CI][Badge-CI]][Link-CI]

[Badge-CI]: https://github.com/SymphonyQL/SymphonyQL/actions/workflows/ScalaCI.yml/badge.svg
[Link-CI]: https://github.com/SymphonyQL/SymphonyQL/actions

SymphonyQL is a GraphQL implementation built with Apache Pekko.

[SymphonyQL Document](https://SymphonyQL.github.io/SymphonyQL)

## Motivation

Native support for Apache Pekko, including Java/Scala.

- Java 21
- Scala 3.3.1

## Examples

These are several POC examples for SymphonyQL, for reference only.

GraphQL Schema:
```graphql
schema {
  query: Queries
}

enum Origin {
  BELT
  EARTH
  MARS
}

# renamed to CharacterOutput in Java Code!
type Character {
  name: String!
  origin: Origin!
}

type Queries {
  characters(origin: Origin): [Character!]
}
```

### Scala 3 Example

Defining API using Scala case classes:
```scala 3
enum Origin {
  case EARTH, MARS, BELT
}
case class Character(name: String, origin: Origin)
case class FilterArgs(origin: Option[Origin])
case class Queries(characters: FilterArgs => Source[Character, NotUsed])
```

For Scala3, we can automatically derive schemas by macros:
```scala 3
// run query
val graphql: SymphonyQL = SymphonyQL
  .newSymphonyQL()
  .addQuery(
      Queries(args =>
        Source.single(Character("abc-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT)))
      ),
    // Automatically generated through scala3 metaprogramming.
    Schema.derived[Queries]
  )
  .build()

implicit val actorSystem: ActorSystem                        = ActorSystem("symphonyActorSystem")
val getRes: Future[SymphonyQLResponse[SymphonyQLError]]      = graphql.runWith(SymphonyQLRequest(Some(characters)))

println(Await.result(getRes, Duration.Inf).toOutputValue)
```

### Java 21 Example

Defining API using Java21 record classes:
```java
@ObjectSchema(withArgs = true)
record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}

@ObjectSchema
record CharacterOutput(String name, Origin origin) {
}

@InputSchema
@ArgExtractor
record FilterArgs(Optional<Origin> origin) {
}

@EnumSchema
@ArgExtractor
enum Origin {
    EARTH,
    MARS,
    BELT
}
```

After compilation, we can directly use `QueriesSchema.schema` (`QueriesSchema` is a default generated class name):
> Also, we can manually define it through DSL.
```java
var graphql = SymphonyQL
        .newSymphonyQL()
        .addQuery(
                new Queries(
                        args1 -> Source.single(new CharacterOutput("abc-" + args1.origin().map(Enum::toString).get(), args1.origin().get()))
                ),
                // Automatically generated through APT.
                QueriesSchema.schema
        )
        .build();
System.out.println(graphql.render());

var characters = """
          {
          characters(origin: "BELT") {
            name
            origin
          }
        }""";

var actorSystem = ActorSystem.create("symphonyActorSystem");

var getRes = graphql.run(
        SymphonyQLRequest.newRequest().query(Optional.of(characters)).build(),
        actorSystem
);

getRes.whenComplete((resp, throwable) -> System.out.println(resp.toOutputValue()));
```

## Inspire By 

1. [caliban](https://github.com/ghostdogpr/caliban)
2. [graphql-java](https://github.com/graphql-java/graphql-java)

The design of this library references caliban and graphql-java, and a large number of ADTs and Utils have been copied from caliban.
