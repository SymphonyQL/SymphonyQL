package symphony.example.schema.complex;

import symphony.apt.annotation.IgnoreSchema;

@IgnoreSchema
record IgnoreObject(
        OriginEnum originEnum
) {
}