package symphony.example.schema;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import symphony.apt.annotation.ObjectSchema;

import java.util.function.Function;

@ObjectSchema
public record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
