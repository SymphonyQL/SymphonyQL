# SymphonyQL

SymphonyQL is a GraphQL implementation built with Apache Pekko

## Motivation

Apache Pekko native, Java/Scala support.

## POC

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
    Schema.derived[Queries]
  )
  .build()

implicit val actorSystem: ActorSystem                        = ActorSystem("symphonyActorSystem")
val getRes: Future[SymphonyQLResponse[SymphonyQLError]]      = graphql.runWith(SymphonyQLRequest(Some(characters)))

println(Await.result(getRes, Duration.Inf).toOutputValue)

actorSystem.terminate()
```

### Java 21 Example

Defining API using Java21 record classes:
```java
record FilterArgs(Optional<Origin> origin) {
}

enum Origin {
    EARTH, MARS, BELT;
}

record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
```

Now, we need to manually define it, which is an example of creating a query through DSL:
```java
public static ArgumentExtractor<FilterArgs> argumentExtractor() {
    return input -> switch (input) {
        case SymphonyQLInputValue.ObjectValue a -> {
            var org = Optional.of(Origin.valueOf(((SymphonyQLValue.StringValue) a.fields().get("origin").get()).value()));
            yield Right.apply(new FilterArgs(org));
        }
        default -> Left.apply(SymphonyQLError.ArgumentError.apply("error", Option.empty(), Option.empty()));
    };
}

public static Schema<Origin> originSchema() {
    return EnumBuilder.<Origin>newEnum()
            .name("Origin")
            .serialize((Function<Origin, String>) Enum::name)
            .values(
                    __EnumValue.apply("EARTH", Option.empty(), false, Option.empty(), Option.empty()),
                    __EnumValue.apply("MARS", Option.empty(), false, Option.empty(), Option.empty()),
                    __EnumValue.apply("BELT", Option.empty(), false, Option.empty(), Option.empty())
            ).build();
}

public static Schema<FilterArgs> inputSchema(Schema<Origin> enumSchema) {
    return InputObjectBuilder.<FilterArgs>newInputObject()
            .name("FilterArgs")
            .field((Function<FieldBuilder, __Field>) builder -> builder.name("name").schema(Schema.createOptional(enumSchema)).build())
            .build();
}

public static Schema<CharacterOutput> outputSchema(Schema<Origin> enumSchema) {
    return ObjectBuilder
            .<CharacterOutput>newObject()
            .name("CharacterOutput")
            .field((Function<FieldBuilder, __Field>) builder -> builder.name("name").schema(Schema.StringSchema()).build())
            .field((Function<FieldBuilder, __Field>) builder -> builder.name("origin").schema(enumSchema).build())
            .build();
}

public static Schema<Queries> queriesSchema(ArgumentExtractor<FilterArgs> argumentExtractor, Schema<FilterArgs> inputSchema, Schema<CharacterOutput> outputSchema) {
    return ObjectBuilder.<Queries>newObject()
            .name("Queries")
            .fieldWithArg(
                    (Function<FieldBuilder, __Field>) builder -> builder
                            .name("characters")
                            .hasArgs(true)
                            .schema(Schema.createFunction(argumentExtractor, inputSchema, Schema.createSource(outputSchema)))
                            .build()
                    ,
                    // for testing, use reflection to create stages
                    (Function<Queries, Stage>) queries -> Stage.derivesStageByReflection(argumentExtractor, args -> queries.characters().apply(args))

            ).build();
}

public static void main(String[] args) {
    var enumSchema = originSchema();
    var argumentExtractor = argumentExtractor();
    var inputSchema = inputSchema(enumSchema);
    var outputSchema = outputSchema(enumSchema);
    var queriesSchema = queriesSchema(argumentExtractor, inputSchema, outputSchema);

    var graphql = SymphonyQL
            .newSymphonyQL()
            .addQuery(
                    new Queries(
                            args1 -> Source.single(new CharacterOutput("abc-" + args1.origin().map(Enum::toString).get(), args1.origin().get()))
                    ),
                    queriesSchema
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

    final var actorSystem = ActorSystem.create("symphonyActorSystem");

    var getRes = graphql.run(
            SymphonyQLRequest.newRequest().query(Optional.of(characters)).build(),
            actorSystem
    );

    getRes.whenComplete((resp, throwable) -> System.out.println(resp));
    getRes.thenRun(() -> actorSystem.terminate());
}
```

## Inspire By 

1. [caliban](https://github.com/ghostdogpr/caliban)
2. [graphql-java](https://github.com/graphql-java/graphql-java)

The design of this library references caliban and graphql-java, and a large number of ADTs and Utils have been copied from caliban.
