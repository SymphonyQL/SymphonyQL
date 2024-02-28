package symphony.example.schema.complex;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;
import symphony.apt.annotation.ObjectSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ArgExtractor
@InputSchema
@ObjectSchema
record NestedObject(
        OriginEnum originEnum,
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