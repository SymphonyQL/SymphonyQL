package example.schema;

import org.apache.pekko.actor.*;
import org.apache.pekko.stream.javadsl.*;

import scala.Option;
import scala.util.Left;
import scala.util.Right;

import symphony.SymphonyQL;
import symphony.SymphonyQLRequest;
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
                        IntrospectionEnumValue.apply("EARTH", Option.empty(), false, Option.empty(), Option.empty()),
                        IntrospectionEnumValue.apply("MARS", Option.empty(), false, Option.empty(), Option.empty()),
                        IntrospectionEnumValue.apply("BELT", Option.empty(), false, Option.empty(), Option.empty())
                ).build();
    }

    public static Schema<FilterArgs> inputSchema(Schema<Origin> enumSchema) {
        return InputObjectBuilder.<FilterArgs>newInputObject()
                .name("FilterArgs")
                .field((Function<FieldBuilder, IntrospectionField>) builder -> builder.name("name").schema(Schema.createOptional(enumSchema)).build())
                .build();
    }

    public static Schema<CharacterOutput> outputSchema(Schema<Origin> enumSchema) {
        return ObjectBuilder
                .<CharacterOutput>newObject()
                .name("CharacterOutput")
                .field(
                        (Function<FieldBuilder, IntrospectionField>) builder -> builder.name("name").schema(Schema.StringSchema()).build(),
                        (Function<CharacterOutput, Stage>) character -> Stage.createNull()
                ).field(
                        (Function<FieldBuilder, IntrospectionField>) builder -> builder.name("origin").schema(enumSchema).build(),
                        (Function<CharacterOutput, Stage>) character -> Stage.createNull()
                ).build();
    }

    public static Schema<Queries> queriesSchema(ArgumentExtractor<FilterArgs> argumentExtractor, Schema<FilterArgs> inputSchema, Schema<CharacterOutput> outputSchema) {
        return ObjectBuilder.<Queries>newObject()
                .name("Queries")
                .field(
                        (Function<FieldBuilder, IntrospectionField>) builder -> builder
                                .name("characters")
                                .hasArgs(true)
                                .schema(Schema.createFunction(argumentExtractor, inputSchema, Schema.createSource(outputSchema)))
                                .build()
                        ,
                        (Function<Queries, Stage>) queries -> Stage.createFunction(input -> {
                            var args = argumentExtractor.extract(SymphonyQLInputValue.ObjectValue.apply(input)).toOption().get();
                            var source = queries.characters().apply(args).map(character -> Stage.createObject(
                                    "CharacterOutput", Map.of(
                                            "name", Stage.createPure(SymphonyQLValue.StringValue.apply(character.name())),
                                            "origin", Stage.createPure(SymphonyQLValue.EnumValue.apply(character.origin().name()))
                                    )
                            ));
                            return Stage.createJavaSource(source);
                        })

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
