package symphony.example.schema.complex;

import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.annotations.java.GQLInputName;
import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;
import symphony.apt.annotation.ObjectSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ArgExtractor
@InputSchema
@ObjectSchema
@GQLDeprecated(reason = "deprecated")
@GQLDescription("NestedObject")
@GQLInputName("NObject")
record NestedObject(
        @GQLDeprecated(reason = "deprecated")
        @GQLDescription("OriginEnum") OriginEnum originEnum,
        Optional<OriginEnum> optionalEnum,
        Optional<String> optionalString,
        List<List<Optional<OriginEnum>>> ssOptionalEnum,
        List<String> sString,
        List<List<String>> ssString,
        String stringV,
        int intV,
        double doubleV,
        float floatV,
        short shortV,
        BigDecimal bigDecimalV,
        Optional<BigDecimal> optionalBigDecimal
) {
}