package symphony.example;

import org.apache.pekko.actor.*;
import org.apache.pekko.stream.javadsl.*;
import symphony.SymphonyQL;
import symphony.SymphonyQLRequest;
import symphony.example.schema.CharacterOutput;
import symphony.example.schema.Queries;
import symphony.example.schema.QueriesSchema;

import java.util.Optional;

public class JavaAPIMain {

    public static void main(String[] args) {
        var graphql = SymphonyQL
                .newSymphonyQL()
                .addQuery(
                        new Queries(
                                args1 -> Source.single(new CharacterOutput("hello-" + args1.origin().map(Enum::toString).get(), args1.origin().get()))
                        ),
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

        final var actorSystem = ActorSystem.create("symphonyActorSystem");

        var getRes = graphql.run(
                SymphonyQLRequest.newRequest().query(characters).build(),
                actorSystem
        );

        getRes.whenComplete((resp, throwable) -> System.out.println(resp.toOutputValue()));
        getRes.thenRun(() -> actorSystem.terminate());
    }
}
