package symphony.apt.tests;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.annotations.java.GQLName;
import symphony.apt.annotation.ObjectSchema;

import java.util.function.Function;
import java.util.function.Supplier;

@ObjectSchema
@GQLDeprecated(reason = "deprecated")
@GQLDescription("QueriesObject")
@GQLName("GQLQueriesObject")
record QueriesObject(
        @GQLDeprecated(reason = "deprecated")
        @GQLDescription("Function") Function<InputObject, Source<OutputObject, NotUsed>> characters,
        Function<InputObject, OutputObject> character,
        Function<Integer, OutputObject> intCharacter,
        Function<String, String> intString,
        @GQLName("supplierArgCharacter") Supplier<OutputObject> noArgCharacter,
        String scalarField,
        SimpleNestedObject simpleNestedObject
) {
}
