package symphony.example.schema;

import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.apt.annotation.ObjectSchema;

@ObjectSchema
@GQLDeprecated(reason = "deprecated")
@GQLDescription("CharacterOutput")
public record CharacterOutput(
        String name,
        @GQLDeprecated(reason = "deprecated")
        @GQLDescription("Origin") Origin origin
) {
}
