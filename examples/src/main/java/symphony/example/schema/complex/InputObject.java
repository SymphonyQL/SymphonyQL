package symphony.example.schema.complex;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
@InputSchema
@ArgExtractor
record InputObject(
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
        Optional<BigDecimal> optionalBigDecimal,
        Optional<NestedObject> optionalNestedObject,
        List<NestedObject> sNestedObject,
        Optional<List<NestedObject>> optionalNestedObjects
) {
}