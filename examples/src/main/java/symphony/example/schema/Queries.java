package symphony.example.schema;

import java.util.function.Function;

import org.apache.pekko.*;
import org.apache.pekko.stream.javadsl.*;
import symphony.apt.annotation.ObjectSchema;

@ObjectSchema(withArgs = true)
public record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
