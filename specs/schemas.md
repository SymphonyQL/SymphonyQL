# Schemas

A SymphonyQL schema will be derived automatically at compile-time from the types present in your resolver.

The following table shows how to convert common Scala/Java types to SymphonyQL types.

| Scala Type (Java Type)                                                                                       | SymphonyQL Type                                 |
|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `Boolean` (`boolean`)                                                                                        | Boolean                                         |
| `Int` (`int`)                                                                                                | Int                                             |
| `Float` (`float`)                                                                                            | Float                                           |
| `Double` (`double`)                                                                                          | Float                                           |
| `String` (`String`)                                                                                          | String                                          |
| `Unit` (`void`)                                                                                              | Unit (custom scalar)                            |
| `Long` (`long`)                                                                                              | Long (custom scalar)                            |
| Case Class (Record Class)                                                                                    | Object                                          |
| `Option[A]` (`Optional[A]`)                                                                                  | Nullable A                                      |
| `List[A]` (`java.util.List[A]`)                                                                              | List of A                                       |
| `Set[A]` (`java.util.Set[A]`)                                                                                | List of A                                       |
| `Seq[A]` (not have)                                                                                          | List of A                                       |
| `Vector[A]` (`java.util.Vector[A]`)                                                                          | List of A                                       |
| `A => B` (`java.util.function.Function`)                                                                     | A and B                                         |
| `Future[A]` (`CompletionStage`)                                                                              | Nullable A                                      |
| `org.apache.pekko.stream.scaladsl.Source[A, NotUsed]` (`org.apache.pekko.stream.javadsl.Source[A, NotUsed]`) | A (subscription) or List of A (query, mutation) |

## Scala 3 Example

Defining API using Scala case classes:
```scala 3
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.*

enum Origin {
  case EARTH, MARS, BELT
}
case class Character(name: String, origin: Origin)
case class FilterArgs(origin: Option[Origin])
case class Queries(characters: FilterArgs => Source[Character, NotUsed])
```

### Auto-deriving Schema

For Scala3, we can use macros to automatically derive a schema by adding the following import:
```scala 3
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.*
import symphony.schema.scaladsl.*

enum Origin {
  case EARTH, MARS, BELT
}
case class Character(name: String, origin: Origin)
case class FilterArgs(origin: Option[Origin])
case class Queries(characters: FilterArgs => Source[Character, NotUsed])

val graphql: SymphonyQL = SymphonyQL
  .builder()
  .rootResolver(
    SymphonyQLResolver(
      Queries(args =>
        Source.single(Character("abc-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT)))
      ) -> Schema.derived[Queries]
    )
  )
  .build()

implicit val actorSystem: ActorSystem                        = ActorSystem("symphonyActorSystem")
val getRes: Future[SymphonyQLResponse[SymphonyQLError]]      = graphql.runWith(SymphonyQLRequest(Some(characters)))

println(Await.result(getRes, Duration.Inf).toOutputValue)

actorSystem.terminate()
```

## Java Example

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

### Creating Schema Manually

Now, we need to manually define it, which is an example of creating a query through DSL:
```java
import org.apache.pekko.actor.*;
import org.apache.pekko.stream.javadsl.*;

import scala.Option;
import scala.Tuple2;
import scala.util.Left;
import scala.util.Right;

import symphony.SymphonyQL;
import symphony.SymphonyQLRequest;
import symphony.SymphonyQLResolver;
import symphony.parser.SymphonyQLError;
import symphony.parser.SymphonyQLInputValue;
import symphony.parser.SymphonyQLValue;
import symphony.parser.adt.introspection.IntrospectionEnumValue;
import symphony.parser.adt.introspection.IntrospectionField;
import symphony.schema.ArgumentExtractor;
import symphony.schema.Schema;
import symphony.schema.Stage;
import symphony.schema.javadsl.EnumBuilder;
import symphony.schema.javadsl.FieldBuilder;
import symphony.schema.javadsl.InputObjectBuilder;
import symphony.schema.javadsl.ObjectBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class JavaCodeUserMain {

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
        return EnumBuilder.<Origin>newEnum().name("Origin").serialize((Function<Origin, String>) o -> o.name()).values(
                IntrospectionEnumValue.apply("EARTH", Option.empty(), false, Option.empty(), Option.empty()),
                IntrospectionEnumValue.apply("MARS", Option.empty(), false, Option.empty(), Option.empty()),
                IntrospectionEnumValue.apply("BELT", Option.empty(), false, Option.empty(), Option.empty())
        ).build();
    }

    @SuppressWarnings("unchecked")
    public static Schema<FilterArgs> inputSchema(Schema<Origin> enumSchema) {
        return InputObjectBuilder.<FilterArgs>newInputObject().name("FilterArgs").
                fields((Function<FieldBuilder, IntrospectionField>) builder -> builder.name("name").schema(Schema.createOptional(enumSchema)).build()).build();
    }

    @SuppressWarnings("unchecked")
    public static Schema<CharacterOutput> outputSchema(Schema<Origin> enumSchema) {
        return ObjectBuilder
                .<CharacterOutput>newObject()
                .name("CharacterOutput")
                .fields(
                        (Function<FieldBuilder, Tuple2<IntrospectionField, Function<CharacterOutput, Stage>>>) builder -> Tuple2.<IntrospectionField, Function<CharacterOutput, Stage>>apply(builder.name("name").schema(Schema.StringSchema()).build(),
                                character -> Stage.createNull()),
                        (Function<FieldBuilder, Tuple2<IntrospectionField, Function<CharacterOutput, Stage>>>) builder -> Tuple2.<IntrospectionField, Function<CharacterOutput, Stage>>apply(builder.name("origin").schema(enumSchema).build(),
                                character -> Stage.createNull())
                ).build();
    }

    @SuppressWarnings("unchecked")
    public static Schema<Queries> queriesSchema(ArgumentExtractor<FilterArgs> argumentExtractor, Schema<FilterArgs> inputSchema, Schema<CharacterOutput> outputSchema) {
        return ObjectBuilder.<Queries>newObject().name("Queries").fields(
                // TODO 类型必须指定
                (Function<FieldBuilder, Tuple2<IntrospectionField, Function<Queries, Stage>>>) builder -> Tuple2.apply(builder.name("characters").hasArgs(true)
                                .schema(Schema.createFunction(argumentExtractor, inputSchema, Schema.createSource(outputSchema)))
                                .build(),
                        (a -> Stage.createFunction(input -> {
                            var args = argumentExtractor.extract(SymphonyQLInputValue.ObjectValue.apply(input)).toOption().get();
                            return Stage.createJavaSource(a.characters().apply(args).map(character -> Stage.createObject(
                                    "Character", Map.of(
                                            "name", Stage.createPure(SymphonyQLValue.StringValue.apply(character.name())),
                                            "origin", Stage.createPure(SymphonyQLValue.EnumValue.apply(character.origin().name()))
                                    )
                            )));
                        })
                        )

                )
        ).build();
    }


    public static void main(String[] args) throws TimeoutException {
        var enumSchema = originSchema();
        var argumentExtractor = argumentExtractor();
        var inputSchema = inputSchema(enumSchema);
        var outputSchema = outputSchema(enumSchema);
        var queriesSchema = queriesSchema(argumentExtractor, inputSchema, outputSchema);

        var graphql = SymphonyQL
                .newSymphonyQL()
                .rootResolver(
                        SymphonyQLResolver.apply(
                                Tuple2.apply(
                                        new Queries(args1 -> Source.single(new CharacterOutput("abc-" + args1.origin().map(Enum::toString).get(), args1.origin().get()))), queriesSchema
                                )
                        )
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

        getRes.whenComplete((resp, throwable) -> {
            System.out.println(resp);
        });
        getRes.thenRun(() -> actorSystem.terminate());
    }
}
```