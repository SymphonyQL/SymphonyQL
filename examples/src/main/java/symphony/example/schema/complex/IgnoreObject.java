package symphony.example.schema.complex;

import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.apt.annotation.IgnoreSchema;

@IgnoreSchema
@GQLDeprecated(reason = "deprecated")
@GQLDescription("IgnoreObject")
record IgnoreObject(
        OriginEnum originEnum
) {
}