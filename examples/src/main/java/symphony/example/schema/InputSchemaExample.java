package symphony.example.schema;


import symphony.apt.annotation.EnumSchema;
import symphony.apt.annotation.InputSchema;
import symphony.apt.annotation.ObjectSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@EnumSchema
enum OriginEnum {
    EARTH, MARS, BELT;
}

@ObjectSchema
record OutputNestedObject(
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

@ObjectSchema
record OutputObject(
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
        Optional<OutputNestedObject> optionalNestedObject,
        List<OutputNestedObject> sNestedObject
) {
}

@InputSchema
record InputObject(
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
