package symphony.example.schema;


import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.EnumSchema;
import symphony.apt.annotation.InputSchema;
import symphony.apt.annotation.ObjectSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EnumSchema
@ArgExtractor
enum OriginEnum {
    EARTH, MARS, BELT
}

@ObjectSchema
@ArgExtractor
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

// 不支持Map嵌套Map
@ObjectSchema
record OutputObject(
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
        List<Map<String, String>> sStringMap,
        List<Map<Optional<String>, NestedObject>> sNestedObjectMap,
        List<Map<Optional<String>, List<NestedObject>>> sOptionalStringListNestedObjectMap,
        Map<String, String> stringMap,
        Map<String, NestedObject> nestedObjectMap,
        Map<String, Optional<NestedObject>> optionalNestedObjectMap,
        Map<String, List<NestedObject>> listNestedObjectMap,
        Map<String, List<Optional<NestedObject>>> listOptionalNestedObjectMap,
        Map<String, List<Optional<String>>> listOptionalStringMap,
        Map<String, Optional<List<String>>> optionalListStringMap,
        Map<String, Optional<List<NestedObject>>> optionalListOutputNestedObjectMap,
        Map<Optional<String>, List<Optional<String>>> optionalListOptionalStringMap,
        Map<List<String>, List<Optional<String>>> listListOptionalStringMap,
        Map<NestedObject, String> nestedObjectStringMap,
        Map<NestedObject, List<String>> nestedObjectListStringMap,
        Map<Optional<NestedObject>, List<Optional<NestedObject>>> optionalNestedObjectListOptionalNestedObjectMap,
        Map<List<NestedObject>, List<Optional<String>>> listNestedObjectMapStringListOptionalMap

) {
}

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
