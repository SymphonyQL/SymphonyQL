package symphony.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import symphony.apt.annotation.ArgExtractor;
import symphony.apt.function.AddSuffix;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.TypeUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

public class ArgumentExtractorCodeGenerator extends GeneratedCodeGenerator {

    protected static final String PARSER_PACKAGE = "symphony.parser";
    private final static String EXTRACTOR_SUFFIX = "Extractor";
    private final static String CREATE_OBJECT_FUNCTION = "function";
    protected static final String EXTRACTOR_METHOD_NAME = EXTRACTOR_SUFFIX.toLowerCase();
    protected static final ClassName ARGUMENT_EXTRACTOR_CLASS = ClassName.get(SCHEMA_PACKAGE, "ArgumentExtractor");
    protected static final ClassName SYMPHONYQL_INPUTVALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLInputValue");
    protected static final ClassName SYMPHONYQL_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue");
    protected static final ClassName EITHER_CLASS = ClassName.get("scala.util", "Either");
    protected static final ClassName LEFT_CLASS = ClassName.get("scala.util", "Left");
    protected static final ClassName RIGHT_CLASS = ClassName.get("scala.util", "Right");
    protected static final ClassName ERROR_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLError", "ArgumentError");
    protected static final ClassName OBJECT_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLInputValue", "ObjectValue");
    protected static final ClassName ENUM_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue", "EnumValue");
    protected static final ClassName STRING_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue", "StringValue");

    @Override
    public final Class<ArgExtractor> getAnnotation() {
        return ArgExtractor.class;
    }

    @Override
    public Function<String, String> getNameModifier() {
        return new AddSuffix(EXTRACTOR_SUFFIX);
    }

    @Override
    protected final void generateBody(final CodeGeneratorContext context, final TypeSpec.Builder builder) {
        var typeElement = context.getTypeElement();
        var annotation = typeElement.getAnnotation(ArgExtractor.class);
        var kind = typeElement.getKind();

        if (annotation != null) {
            if (kind == (ElementKind.ENUM)) {
                generateEnum(builder, typeElement);
            }
            if (kind == ElementKind.RECORD) {
                generateObject(builder, typeElement);
            }
        } else {
            MessageUtils.message(Diagnostic.Kind.WARNING, "@ArgExtractor only support on record or enum class: " + typeElement);
        }
    }

    private final static String createFieldTemplate = """
            var $L = obj.fields().get($S);
            if ($L.isEmpty()) {
                throw new RuntimeException($S);
            }
            %s
            var $L = switch ($L) {
                case $T<$T, ?> right -> {
                    yield ($T) right.value();
                }
                case $T<$T, ?> left -> {
                    throw new RuntimeException($S, left.value());
                }
            };
            """;

    private void generateEnum(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var variables = typeElement.getEnclosedElements().stream().filter(t -> t.getKind().equals(ElementKind.ENUM_CONSTANT)).toList();
        var typeName = TypeUtils.getTypeName(typeElement);
        var fieldCodes = new LinkedHashMap<String, CodeBlock>();
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_VALUE_CLASS, typeName);
        var type = TypeUtils.getTypeName(typeElement);
        var fieldName = typeElement.getSimpleName().toString().toLowerCase();
        var eitherValueName = fieldName + "Either";
        var code = CodeBlock.builder().add("""
                        if (obj instanceof $T value) {
                            var $L = $T.StringArg().extract(value);
                            return switch ($L) {
                                case $T<$T, ?> right -> {
                                    yield ($T) $T.valueOf((String)right.value());
                                }
                                case $T<$T, ?> left -> {
                                    throw new RuntimeException($S, left.value());
                                }
                            };
                        }
                        var $L = $T.StringArg().extract(obj);
                        return switch ($L) {
                            case $T<$T, ?> right -> {
                                yield ($T) $T.valueOf((String)right.value());
                            }
                            case $T<$T, ?> left -> {
                                throw new RuntimeException($S, left.value());
                            }
                        };
                        """,
                ENUM_VALUE_CLASS, // line 1
                eitherValueName, ARGUMENT_EXTRACTOR_CLASS, // line 2
                eitherValueName, // line 3
                RIGHT_CLASS, ERROR_CLASS, // line 4
                type, type, // line 5
                LEFT_CLASS, ERROR_CLASS, // line 7
                "Cannot build enum " + typeName + " from input", // line 8
                eitherValueName, ARGUMENT_EXTRACTOR_CLASS, // line 12
                eitherValueName, // line 13
                RIGHT_CLASS, ERROR_CLASS, // line 14
                type, type, // line 15
                LEFT_CLASS, ERROR_CLASS, // line 17
                "Cannot build enum " + typeName + " from input" // line 18

        ).build();

        fieldCodes.put(fieldName, code);

        var fieldSpec = FieldSpec
                .builder(functionType, CREATE_OBJECT_FUNCTION, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .initializer("$L", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(functionType)
                        .addMethod(generateEnumApplyMethod(fieldCodes, typeName)).build())
                .build();
        generateObjectBody(builder, typeName, CodeBlock.builder().add("""
                                return switch (input) {
                                    case $T obj ->  {
                                        yield $T.apply($L.apply(obj));
                                    }
                                    case $T obj ->  {
                                        yield $T.apply($L.apply(obj));
                                    }
                                    default -> $T.apply(new $T($S));
                                };
                                """, List.of(
                                ENUM_VALUE_CLASS,
                                RIGHT_CLASS,
                                CREATE_OBJECT_FUNCTION,
                                STRING_VALUE_CLASS,
                                RIGHT_CLASS,
                                CREATE_OBJECT_FUNCTION,
                                LEFT_CLASS,
                                ERROR_CLASS,
                                "Expected EnumValue or StringValue"
                        ).toArray()
                ).build(),
                fieldSpec
        );
    }

    protected void generateObject(
            final TypeSpec.Builder builder,
            final TypeElement typeElement
    ) {
        var variables = ModelUtils.getVariableTypes(typeElement, ModelUtils.createHasFieldPredicate(typeElement));
        var fieldCodes = new LinkedHashMap<String, CodeBlock>();
        var typeName = TypeUtils.getTypeName(typeElement);
        for (var element : variables.entrySet()) {
            var fieldTypeName = TypeUtils.getTypeName(element.getValue());
            var fieldName = element.getKey();
            var optionalValueName = fieldName + "Optional";
            var eitherValueName = fieldName + "Either";
            switch (TypeUtils.getTypeCategory(fieldTypeName)) {
                case SYSTEM_TYPE -> {
                    var code = CodeBlock.builder().add(
                            String.format(createFieldTemplate, "var $L = $T.getArgumentExtractor($S).extract($L.get());"), // line 5
                            optionalValueName, fieldName, // line 1
                            optionalValueName, // line 2 
                            "Field " + fieldName + " is not present in input", // line 3
                            eitherValueName, ARGUMENT_EXTRACTOR_CLASS, fieldTypeName, optionalValueName, // line 5
                            fieldName, eitherValueName, // line 6
                            RIGHT_CLASS, ERROR_CLASS, // line 7
                            fieldTypeName, // line 8
                            LEFT_CLASS, ERROR_CLASS, // line 10
                            "Cannot build field " + fieldName + " from input, expected type: " + fieldTypeName.toString() // line 11
                    ).build();
                    fieldCodes.put(fieldName, code);
                }
                case CUSTOM_OBJECT_TYPE -> {
                }
                case ONE_PARAMETERIZED_TYPE -> {
                }
                case TWO_PARAMETERIZED_TYPES -> {
                }
            }
        }
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_INPUTVALUE_CLASS, typeName);
        var fieldSpec = FieldSpec
                .builder(functionType, CREATE_OBJECT_FUNCTION, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .initializer("$L", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(functionType)
                        .addMethod(generateObjectApplyMethod(fieldCodes, typeName)).build())
                .build();
        generateObjectBody(builder, typeName, CodeBlock.builder().add("""
                                return switch (input) {
                                    case $T obj ->  {
                                        yield $T.apply($L.apply(obj));
                                    }
                                    default -> $T.apply(new $T($S));
                                };
                                """, List.of(
                                OBJECT_VALUE_CLASS,
                                RIGHT_CLASS,
                                CREATE_OBJECT_FUNCTION,
                                LEFT_CLASS,
                                ERROR_CLASS,
                                "Expected ObjectValue"
                        ).toArray()
                ).build(),
                fieldSpec
        );
    }

    private MethodSpec generateEnumApplyMethod(
            final LinkedHashMap<String, CodeBlock> fieldCodes,
            final TypeName typeName
    ) {
        // new Function<SymphonyQLValue, T>
        var applyMethod = MethodSpec.methodBuilder("apply")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(SYMPHONYQL_VALUE_CLASS, "obj")
                .returns(typeName);

        fieldCodes.values().forEach(applyMethod::addCode);
        return applyMethod.build();

    }

    private MethodSpec generateObjectApplyMethod(
            final LinkedHashMap<String, CodeBlock> fieldCodes,
            final TypeName typeName
    ) {
        // new Function<SymphonyQLInputValue, T>
        var applyMethod = MethodSpec.methodBuilder("apply")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(OBJECT_VALUE_CLASS, "obj")
                .returns(typeName);

        var join = new StringJoiner(",");
        fieldCodes.keySet().forEach(join::add);
        fieldCodes.values().forEach(applyMethod::addCode);
        applyMethod.addStatement("return new $T($L)", typeName, join.toString());
        return applyMethod.build();

    }

    private void generateObjectBody(
            final TypeSpec.Builder builder,
            final TypeName typeName,
            final CodeBlock applyCode,
            final FieldSpec fieldSpec

    ) {
        var returnType = ParameterizedTypeName.get(ARGUMENT_EXTRACTOR_CLASS, typeName);
        // new ArgumentExtractor<T>
        var extractAnonymousObject = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(ARGUMENT_EXTRACTOR_CLASS, typeName))
                .addMethod(MethodSpec.methodBuilder("extract")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(SYMPHONYQL_INPUTVALUE_CLASS, "input")
                        .returns(ParameterizedTypeName.get(EITHER_CLASS, ERROR_CLASS, typeName))
                        .addCode(applyCode).build()
                );

        // private final static Function<SymphonyQLInputValue, T> function = new Function<SymphonyQLInputValue, T>{ };  
        builder.addField(fieldSpec);

        // public static final ArgumentExtractor<T> extractor = extractor(); 
        builder.addField(assignFieldSpec(returnType, EXTRACTOR_METHOD_NAME));

        // private static ArgumentExtractor<T> extractor(){ };
        builder.addMethod(
                MethodSpec.methodBuilder(EXTRACTOR_SUFFIX.toLowerCase())
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addStatement("return $L", extractAnonymousObject.build())
                        .returns(returnType)
                        .build()
        );
    }
}
