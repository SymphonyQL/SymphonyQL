package symphony.example.schema.complex;

import org.apache.pekko.*;
import org.apache.pekko.stream.javadsl.*;
import symphony.apt.annotation.ObjectSchema;

import java.util.function.Function;
import java.util.function.Supplier;

@ObjectSchema(withArgs = true)
record QueriesObject(
        Function<InputObject, Source<OutputObject, NotUsed>> characters,
        Function<InputObject, OutputObject> character,
        Function<Integer, OutputObject> intCharacter,
        Function<String, String> intString,
        Supplier<OutputObject> noArgCharacter,
        String ignoreField
) {
}
