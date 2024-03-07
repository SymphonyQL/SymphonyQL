package symphony.apt.tests;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;
import symphony.apt.annotation.ObjectSchema;

@ArgExtractor
@InputSchema
@ObjectSchema
record SimpleNestedObject(
        OriginEnum originEnum
) {
}