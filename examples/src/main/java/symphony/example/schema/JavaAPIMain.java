package symphony.example.schema;

import scala.Option;
import scala.util.Left;
import scala.util.Right;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.javadsl.Source;

import symphony.SymphonyQL;
import symphony.SymphonyQLRequest;
import symphony.parser.SymphonyQLError;
import symphony.parser.SymphonyQLInputValue;
import symphony.parser.SymphonyQLValue;
import symphony.parser.adt.introspection.*;
import symphony.schema.ArgumentExtractor;
import symphony.schema.Schema;
import symphony.schema.Stage;
import symphony.schema.javadsl.EnumBuilder;
import symphony.schema.javadsl.FieldBuilder;
import symphony.schema.javadsl.InputObjectBuilder;
import symphony.schema.javadsl.ObjectBuilder;

import java.util.Optional;
import java.util.function.Function;

public class JavaAPIMain {

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
}
