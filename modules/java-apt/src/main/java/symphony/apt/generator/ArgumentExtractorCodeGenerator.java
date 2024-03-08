package symphony.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import scala.jdk.javaapi.OptionConverters;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import symphony.annotations.java.GQLDefault;
import symphony.apt.Constant;
import symphony.apt.annotation.ArgExtractor;
import symphony.apt.model.WrappedContext;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.TypeUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

public class ArgumentExtractorCodeGenerator extends GeneratedCodeGenerator {
    // scala classes
    protected static final ClassName EITHER_CLASS = ClassName.get(Either.class);
    protected static final ClassName RIGHT_CLASS = ClassName.get(Right.class);
    protected static final ClassName LEFT_CLASS = ClassName.get(Left.class);
    protected static final ClassName OPTION_CONVERTERS_CLASS = ClassName.get(OptionConverters.class);

    @Override
    public final Class<ArgExtractor> getAnnotation() {
        return ArgExtractor.class;
    }

    @Override
    public Function<String, String> getNameModifier() {
        return Constant.EXTRACTOR_SUFFIX_FUNCTION;
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

    private final static String createObjectOptionalFieldTemplate = """
            var $L = obj.fields().get($S);
            var $L = %s;
            var $L = $L.isEmpty() ? %s : $L.extract($L.get());
            if ($L.isLeft()) {
                return $T.apply(new $T($S, $L.swap().toOption().get()));
            }
            var $L = $T.toJava($L.flatMap(a -> $L.toOption()))
                    .flatMap(a -> a.isEmpty() ? $T.empty() : $T.of(a.get()));
                    
            """;

    private final static String createObjectFieldTemplate = """
            var $L = obj.fields().get($S);
            var $L = %s;
            var $L = $L.isEmpty() ? %s : $L.extract($L.get());
            if ($L.isLeft()) {
                return $T.apply(new $T($S, $L.swap().toOption().get()));
            }
            var $L = ($T) $L.toOption().get();
                        
            """;

    private final static String createEnumValueTemplate = """
            if (obj instanceof $T value) {
                var $L = $T.stream($T.values()).filter(o -> o.name().equals(value.value())).findFirst();
                if ($L.isEmpty()) {
                    return $T.apply(new $T($S));
                }
                return $T.apply($L.get());
            }
                        
            var $L = ($T) obj;
            var $L = $T.stream($T.values()).filter(o -> o.name().equals($L.value())).findFirst();
            if ($L.isEmpty()) {
                return $T.apply(new $T($S));
            }
            return $T.apply($L.get());
            """;

    private void generateEnum(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var typeName = TypeUtils.getTypeName(typeElement);
        var realName = getName(typeElement).orElse(TypeUtils.getSimpleName(typeElement));
        var fieldCodes = new LinkedHashMap<String, CodeBlock>();
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_VALUE_CLASS, ParameterizedTypeName.get(EITHER_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, typeName));
        var type = TypeUtils.getTypeName(typeElement);
        var fieldName = typeElement.getSimpleName().toString().toLowerCase();
        var optionalValueName = Constant.OPTIONAL_SUFFIX_FUNCTION.apply(fieldName);
        var stringValueName = Constant.STRING_SUFFIX_FUNCTION.apply(fieldName);
        var code = CodeBlock.builder().add(createEnumValueTemplate, SYMPHONYQL_ENUM_VALUE_CLASS, // line 1
                optionalValueName, ClassName.get(Arrays.class), type,// line 2
                optionalValueName, // line 3
                LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_ENUM_ERROR_MSG, realName), // line 4
                RIGHT_CLASS, optionalValueName, // line 6
                stringValueName, SYMPHONYQL_STRING_VALUE_CLASS, // line 9
                optionalValueName, ClassName.get(Arrays.class), type, stringValueName,// line 10
                optionalValueName, // line 11
                LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_ENUM_ERROR_MSG, realName), // line 12
                RIGHT_CLASS, optionalValueName // line 14

        ).build();

        fieldCodes.put(fieldName, code);

        var fieldSpec = FieldSpec.builder(functionType, Constant.CREATE_OBJECT_FUNCTION, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .initializer("$L",
                        TypeSpec.anonymousClassBuilder("")
                                .addSuperinterface(functionType)
                                .addMethod(generateEnumApplyMethod(fieldCodes, typeName))
                                .build())
                .build();
        var applyCode = CodeBlock.builder().add("""
                return switch (input) {
                    case $T obj ->  {
                        yield $L.apply(obj);
                    }
                    case $T obj -> {
                        yield $L.apply(obj);
                    }
                    default -> $T.apply(new $T($S));
                };
                """, List.of(
                SYMPHONYQL_ENUM_VALUE_CLASS, // 2
                Constant.CREATE_OBJECT_FUNCTION, // 3
                SYMPHONYQL_STRING_VALUE_CLASS, // 5
                Constant.CREATE_OBJECT_FUNCTION, // 6
                LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, "Expected EnumValue or StringValue" // 8
        ).toArray()).build();
        generateObjectBody(builder, typeName, applyCode, fieldSpec);
    }

    // TODO support interface
    protected void generateObject(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var fieldElements = ModelUtils.getRecordComponents(typeElement);
        var fieldCodes = new LinkedHashMap<String, CodeBlock>();
        var typeName = TypeUtils.getTypeName(typeElement);
        for (var elementEntry : fieldElements.entrySet()) {
            var fieldTypeName = TypeUtils.getTypeName(elementEntry.getValue());
            var fieldName = elementEntry.getKey();
            var fieldSchemaName = Constant.SCHEMA_SUFFIX.apply(fieldName);
            var inputFieldName = getName(elementEntry.getValue()).orElse(fieldName);
            var optionalValueName = Constant.OPTIONAL_SUFFIX_FUNCTION.apply(fieldName);
            var eitherValueName = Constant.EITHER_SUFFIX_FUNCTION.apply(fieldName);
            var exceptedObjectName = getNameModifier().apply(fieldTypeName.toString());
            var rawType = TypeUtils.getRawTypeName(fieldTypeName);

            var defaultAnnotation = TypeUtils.getAnnotation(GQLDefault.class, elementEntry.getValue());
            var defaultString = defaultAnnotation == null ? "$L.defaultValue($T.$N())" : "$L.defaultValue($T.of($S))";
            List<Object> defaultValue = defaultAnnotation == null || defaultAnnotation.value() == null ?
                    List.of(fieldSchemaName, ClassName.get(Optional.class), "empty") : List.of(fieldSchemaName, ClassName.get(Optional.class), defaultAnnotation.value());

            var fieldWrappedTypeName = elementEntry.getValue().asType().getKind().isPrimitive() ? TypeUtils.getTypeName(elementEntry.getValue(), true) : fieldTypeName;

            defaultString = TypeUtils.noDefaultValue(fieldTypeName) ? "$T.<$T, $T>apply(new $T($S))" : defaultString;
            defaultValue = TypeUtils.noDefaultValue(fieldTypeName) ?
                    List.of(LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, fieldWrappedTypeName, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_NOT_FOUND_ERROR_MSG, inputFieldName)) : defaultValue;

            if (rawType.toString().equals(Constant.JAVA_OPTIONAL_CLASS)) {
                defaultString = "$T.<$T, $T>apply($T.empty())";
                defaultValue = List.of(RIGHT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, fieldWrappedTypeName, ClassName.get(Optional.class));
            }

            var getFieldVargs = List.of(optionalValueName, inputFieldName);
            ArrayList<Object> args = new ArrayList<>();
            String codeString = null;

            switch (TypeUtils.classifyType(rawType)) {
                case DEFAULT_OR_PRIMITIVE_TYPE -> {
                    args = new ArrayList<>();
                    args.addAll(getFieldVargs);
                    args.addAll(List.of(fieldSchemaName, EXTRACTOR_CLASS, fieldTypeName));
                    args.addAll(List.of(eitherValueName, optionalValueName));
                    args.addAll(defaultValue);
                    args.addAll(List.of(fieldSchemaName, optionalValueName));
                    args.addAll(List.of(
                            eitherValueName, LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_ERROR_MSG, inputFieldName), eitherValueName,
                            fieldName, fieldTypeName, eitherValueName)
                    );
                    codeString = String.format(createObjectFieldTemplate, "$T.getArgumentExtractor($S)", defaultString);
                }
                case CUSTOM_OBJECT_TYPE -> {
                    args = new ArrayList<>();
                    args.addAll(getFieldVargs);
                    args.addAll(List.of(fieldSchemaName, ClassName.get("", exceptedObjectName), Constant.EXTRACTOR_METHOD_NAME));
                    args.addAll(List.of(eitherValueName, optionalValueName));
                    args.addAll(defaultValue);
                    args.addAll(List.of(fieldSchemaName, optionalValueName));
                    args.addAll(List.of(
                            eitherValueName, LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_ERROR_MSG, inputFieldName), eitherValueName,
                            fieldName, fieldTypeName, eitherValueName)
                    );
                    codeString = String.format(createObjectFieldTemplate, "$T.$N", defaultString);
                }
                case COLLECTION_PARAMETERIZED_TYPE -> {
                    var wrappedArgs = new ArrayList<>();
                    if (rawType.toString().equals(Constant.JAVA_OPTIONAL_CLASS)) {
                        var buildExtractorString = TypeUtils.buildExtractorWrappedString(new WrappedContext(fieldTypeName, EXTRACTOR_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs);
                        codeString = String.format(createObjectOptionalFieldTemplate, buildExtractorString, defaultString);
                        args = new ArrayList<>();
                        args.addAll(getFieldVargs);
                        args.add(fieldSchemaName);
                        args.addAll(wrappedArgs);
                        args.addAll(List.of(eitherValueName, optionalValueName));
                        args.addAll(defaultValue);
                        args.addAll(List.of(fieldSchemaName, optionalValueName));
                        args.addAll(List.of(
                                eitherValueName,
                                LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_ERROR_MSG, inputFieldName), eitherValueName,
                                fieldName,
                                OPTION_CONVERTERS_CLASS,
                                optionalValueName,
                                eitherValueName,
                                ClassName.get(Optional.class),
                                ClassName.get(Optional.class)
                        ));
                    } else {
                        var buildExtractorString = TypeUtils.buildExtractorWrappedString(new WrappedContext(fieldTypeName, EXTRACTOR_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs);
                        args = new ArrayList<>();
                        args.addAll(getFieldVargs);
                        args.add(fieldSchemaName);
                        args.addAll(wrappedArgs);
                        args.addAll(List.of(eitherValueName, optionalValueName));
                        args.addAll(defaultValue);
                        args.addAll(List.of(fieldSchemaName, optionalValueName));
                        args.addAll(List.of(
                                        eitherValueName, LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, String.format(Constant.CREATE_ERROR_MSG, inputFieldName), eitherValueName,
                                        fieldName, fieldTypeName, eitherValueName
                                )
                        );
                        codeString = String.format(createObjectFieldTemplate, buildExtractorString, defaultString);
                    }
                }
                case MAP_PARAMETERIZED_TYPE -> {
                }
            }
            if (codeString != null) {
                var code = CodeBlock.builder().add(codeString, args.toArray()).build();
                fieldCodes.put(fieldName, code);
            }
        }
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_OBJECT_VALUE_CLASS,
                ParameterizedTypeName.get(EITHER_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, typeName)
        );
        var applyCode = CodeBlock.builder().add("""
                return switch (input) {
                    case $T obj -> {
                        yield $L.apply(obj);
                    }
                    default -> $T.apply(new $T($S));
                };
                """, List.of(
                SYMPHONYQL_OBJECT_VALUE_CLASS, Constant.CREATE_OBJECT_FUNCTION,
                LEFT_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, "Expected ObjectValue"
        ).toArray()).build();
        var fieldSpec = FieldSpec.builder(functionType, Constant.CREATE_OBJECT_FUNCTION, Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .initializer("$L", TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(functionType)
                        .addMethod(generateObjectApplyMethod(fieldCodes, typeName)).build())
                .build();
        generateObjectBody(builder, typeName, applyCode, fieldSpec);
    }

    private MethodSpec generateEnumApplyMethod(
            final LinkedHashMap<String, CodeBlock> fieldCodes,
            final TypeName typeName
    ) {
        // new Function<SymphonyQLValue, Either<SymphonyQLError.ArgumentError, T>>
        var applyMethod = MethodSpec.methodBuilder("apply")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(SYMPHONYQL_VALUE_CLASS, "obj")
                .returns(ParameterizedTypeName.get(EITHER_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, typeName));

        fieldCodes.values().forEach(applyMethod::addCode);
        return applyMethod.build();

    }

    private MethodSpec generateObjectApplyMethod(
            final LinkedHashMap<String, CodeBlock> fieldCodes,
            final TypeName typeName
    ) {
        // new Function<SymphonyQLInputValue, Either<SymphonyQLError.ArgumentError, T>>
        var applyMethod = MethodSpec.methodBuilder("apply")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(SYMPHONYQL_OBJECT_VALUE_CLASS, "obj")
                .returns(ParameterizedTypeName.get(EITHER_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, typeName));

        var join = new StringJoiner(", ");
        fieldCodes.keySet().forEach(join::add);
        fieldCodes.values().forEach(applyMethod::addCode);
        applyMethod.addStatement("return $T.apply(new $T($L))", RIGHT_CLASS, typeName, join.toString());
        return applyMethod.build();

    }

    private void generateObjectBody(
            final TypeSpec.Builder builder,
            final TypeName typeName,
            final CodeBlock applyCode,
            final FieldSpec fieldSpec
    ) {
        var returnType = ParameterizedTypeName.get(EXTRACTOR_CLASS, typeName);
        // new ArgumentExtractor<T>
        var extractAnonymousObject = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(EXTRACTOR_CLASS, typeName))
                // public Either<SymphonyQLError.ArgumentError, InputObject> extract(SymphonyQLInputValue input)
                .addMethod(MethodSpec.methodBuilder("extract")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(SYMPHONYQL_INPUTVALUE_CLASS, "input")
                        .returns(ParameterizedTypeName.get(EITHER_CLASS, SYMPHONYQL_ARG_ERROR_CLASS, typeName))
                        .addCode(applyCode).build());

        // public static final ArgumentExtractor<T> extractor = extractor(); 
        builder.addField(assignFieldSpec(returnType, Constant.EXTRACTOR_METHOD_NAME));

        builder.addMethod(
                // private static ArgumentExtractor<T> extractor(){ };
                MethodSpec.methodBuilder(Constant.EXTRACTOR_METHOD_NAME.toLowerCase())
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addStatement("return $L", extractAnonymousObject.build()).returns(returnType)
                        .build());

        // private final static Function<SymphonyQLInputValue, Either<SymphonyQLError.ArgumentError, T>> function = 
        // new Function<SymphonyQLInputValue, Either<SymphonyQLError.ArgumentError, T>>{ };  
        builder.addField(fieldSpec);
    }
}
