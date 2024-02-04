package example.schema;

import java.util.function.Function;

import org.apache.pekko.*;
import org.apache.pekko.stream.javadsl.*;

record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
