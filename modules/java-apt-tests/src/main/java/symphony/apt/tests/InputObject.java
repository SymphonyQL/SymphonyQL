package symphony.apt.tests;

import symphony.annotations.java.GQLDefault;
import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.annotations.java.GQLName;
import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@InputSchema
@ArgExtractor
@GQLDeprecated(reason = "deprecated")
@GQLDescription("InputObject")
record InputObject(
        @GQLDefault("EARTH") OriginEnum originEnum,
        @GQLDefault("EARTH") @GQLName("org") Optional<OriginEnum> optionalEnum,
        @GQLDeprecated(reason = "deprecated")
        @GQLDescription("Optional") Optional<String> optionalString,
        List<List<Optional<OriginEnum>>> ssOptionalEnum,
        List<String> sString,
        List<List<String>> ssString,
        String stringV,
        int intV,
        double doubleV,
        float floatV,
        short shortV,
        BigDecimal bigDecimalV,
        Optional<BigDecimal> optionalBigDecimal,
        Optional<NestedObject> optionalNestedObject,
        List<NestedObject> sNestedObject,
        Optional<List<NestedObject>> optionalNestedObjects
) {
}