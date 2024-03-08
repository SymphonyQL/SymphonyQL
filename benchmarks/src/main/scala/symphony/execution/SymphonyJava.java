package symphony.execution;

import org.apache.pekko.actor.ActorSystem;
import symphony.SymphonyQL;
import symphony.SymphonyQLRequest;
import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.EnumSchema;
import symphony.apt.annotation.InputSchema;
import symphony.apt.annotation.ObjectSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnumSchema
@ArgExtractor
enum Origin {
    MARS, EARTH, BELT
}

@EnumSchema
enum Role {
    Captain,
    Pilot,
    Engineer,
    Mechanic
}

@InputSchema
@ArgExtractor
record CharactersArgs(Optional<Origin> origin) {
}

@InputSchema
@ArgExtractor
record CharacterArgs(String name) {
}

@ObjectSchema
record Character(String name, List<String> nicknames, Origin origin, Optional<Role> role) {
}

@ObjectSchema
record Query(Function<CharactersArgs, CompletionStage<List<Character>>> characters,
             Function<CharacterArgs, CompletionStage<Optional<Character>>> character) {
}

public class SymphonyJava {

    final static List<Character> characters = List.of(
            new Character("James Holden", List.of("Jim", "Hoss"), Origin.EARTH, Optional.of(Role.Captain)),
            new Character("Naomi Nagata", new ArrayList<>(), Origin.BELT, Optional.of(Role.Engineer)),
            new Character("Amos Burton", new ArrayList<>(), Origin.EARTH, Optional.of(Role.Mechanic)),
            new Character("Alex Kamal", new ArrayList<>(), Origin.MARS, Optional.of(Role.Pilot)),
            new Character("Chrisjen Avasarala", new ArrayList<>(), Origin.EARTH, Optional.empty()),
            new Character("Josephus Miller", List.of("Joe"), Origin.BELT, Optional.empty()),
            new Character("Roberta Draper", List.of("Bobbie", "Gunny"), Origin.MARS, Optional.empty())
    );

    final static SymphonyQL graphql = SymphonyQL
            .newSymphonyQL()
            .addQuery(new Query(
                    args -> CompletableFuture.completedFuture(
                            characters.stream().filter(c -> args.origin().stream().allMatch(a -> c.origin().equals(a))).collect(Collectors.toList())
                    ),
                    args -> CompletableFuture.completedFuture(
                            characters.stream().filter(c -> c.name().equals(args.name())).toList().stream().findFirst()
                    )
            ), QuerySchema.schema)
            .build();

    static ActorSystem actorSystem = ActorSystem.apply("symphonyActorSystem");

    public static void main(String[] args) {
        var getRes = SymphonyJava.graphql.run(
                SymphonyQLRequest.newRequest().query(Optional.of("""
                        {
                           characters {
                             name
                             origin
                           }
                        }""")).build(),
                actorSystem);
        getRes.whenComplete((resp, throwable) -> System.out.println(resp.toOutputValue()));
        getRes.thenRun(() -> actorSystem.terminate());
    }
}
