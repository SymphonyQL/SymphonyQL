package symphony.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

public class ArgumentExtractorCodeGenerator extends GeneratedCodeGenerator {
    // scala classes
    protected static final ClassName EITHER_CLASS = ClassName.get(scala.util.Either.class);
    protected static final ClassName RIGHT_CLASS = ClassName.get(scala.util.Right.class);
    protected static final ClassName OPTION_CONVERTERS_CLASS = ClassName.get(scala.jdk.javaapi.OptionConverters.class);

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
            var $L = $T.toJava($L.flatMap(a -> (%s).extract(a).toOption()))
                    .flatMap(a -> a.isEmpty() ? $T.empty() : $T.of(a.get()));
            """;

    private final static String createObjectFieldTemplate = """
            var $L = obj.fields().get($S);
            if ($L.isEmpty()) {
                throw new RuntimeException($S);
            }
            %s
            if ($L.isLeft()) {
                throw new RuntimeException($S, $L.swap().toOption().get());
            }
            var $L = ($T) $L.toOption().get();
            """;

    private final static String createEnumValueTemplate = """
            if (obj instanceof $T value) {
                var $L = $T.stream($T.values()).filter(o -> o.name().equals(value.value())).findFirst();
                if ($L.isEmpty()) {
                    throw new RuntimeException($S);
                }
                return $L.get();
            }
            var $L = ($T)obj;
            var $L = $T.stream($T.values()).filter(o -> o.name().equals($L.value())).findFirst();
            if ($L.isEmpty()) {
                throw new RuntimeException($S);
            }
            return $L.get();
            """;

    private void generateEnum(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var typeName = TypeUtils.getTypeName(typeElement);
        var simpleName = TypeUtils.getSimpleName(typeElement);
        var fieldCodes = new LinkedHashMap<String, CodeBlock>();
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_VALUE_CLASS, typeName);
        var type = TypeUtils.getTypeName(typeElement);
        var fieldName = typeElement.getSimpleName().toString().toLowerCase();
        var optionalValueName = Constant.OPTIONAL_SUFFIX_FUNCTION.apply(fieldName);
        var stringValueName = Constant.STRING_SUFFIX_FUNCTION.apply(fieldName);
        var code = CodeBlock.builder().add(createEnumValueTemplate, SYMPHONYQL_ENUM_VALUE_CLASS, // line 1
                optionalValueName, ClassName.get(Arrays.class), type,// line 2
                optionalValueName, // line 3
                String.format(Constant.CREATE_ENUM_ERROR_MSG, simpleName), // line 4
                optionalValueName, // line 6
                stringValueName, SYMPHONYQL_STRING_VALUE_CLASS, // line 8
                optionalValueName, ClassName.get(Arrays.class), type, stringValueName,// line 9
                optionalValueName, // line 10
                String.format(Constant.CREATE_ENUM_ERROR_MSG, simpleName), // line 11
                optionalValueName // line 13

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
                        yield $T.apply($L.apply(obj));
                    }
                    case $T obj -> {
                        yield $T.apply($L.apply(obj));
                    }
                    default -> throw new RuntimeException("Expected EnumValue or StringValue");
                };
                """, List.of(SYMPHONYQL_ENUM_VALUE_CLASS, // 1
                RIGHT_CLASS, Constant.CREATE_OBJECT_FUNCTION, //2
                SYMPHONYQL_STRING_VALUE_CLASS, // 4
                RIGHT_CLASS, Constant.CREATE_OBJECT_FUNCTION // 5
        ).toArray()).build();
        generateObjectBody(builder, typeName, applyCode, fieldSpec);
    }

    protected void generateObject(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var variables = ModelUtils.getVariableTypes(typeElement, ModelUtils.createHasFieldPredicate(typeElement));
        var fieldCodes = new LinkedHashMap<String, CodeBlock>();
        var typeName = TypeUtils.getTypeName(typeElement);
        for (var element : variables.entrySet()) {
            var fieldTypeName = TypeUtils.getTypeName(element.getValue());
            var fieldName = element.getKey();
            var optionalValueName = Constant.OPTIONAL_SUFFIX_FUNCTION.apply(fieldName);
            var eitherValueName = Constant.EITHER_SUFFIX_FUNCTION.apply(fieldName);
            CodeBlock code = null;
            var exceptedObjectName = getNameModifier().apply(fieldTypeName.toString());
            var beforeExtractArgs = List.of(optionalValueName, fieldName, // line 1
                    optionalValueName, // line 2 
                    String.format(Constant.NOT_FOUND_ERROR_MSG, fieldName) // line 3
            );
            var afterExtractArgs = List.of(eitherValueName, // line 6
                    String.format(Constant.CREATE_ERROR_MSG, fieldName), eitherValueName, // 7
                    fieldName, fieldTypeName, eitherValueName // 9
            );

            var rawType = TypeUtils.getRawTypeName(fieldTypeName);
            switch (TypeUtils.classifyType(rawType)) {
                case DEFAULT_OR_PRIMITIVE_TYPE -> {
                    var args = new LinkedList<>();
                    args.addAll(beforeExtractArgs);
                    args.addAll(List.of(eitherValueName, EXTRACTOR_CLASS, fieldTypeName, optionalValueName));
                    args.addAll(afterExtractArgs);
                    code = CodeBlock.builder().add(String.format(createObjectFieldTemplate, "var $L = $T.getArgumentExtractor($S).extract($L.get());"), args.toArray()).build();
                }
                case CUSTOM_OBJECT_TYPE -> {
                    var args = new LinkedList<>();
                    var exceptedObjectType = ClassName.get("", exceptedObjectName);
                    args.addAll(beforeExtractArgs);
                    args.addAll(List.of(eitherValueName, exceptedObjectType, Constant.EXTRACTOR_METHOD_NAME, optionalValueName));
                    args.addAll(afterExtractArgs);
                    code = CodeBlock.builder().add(String.format(createObjectFieldTemplate, "var $L = $T.$N.extract($L.get());"), args.toArray()).build();
                }
                case COLLECTION_PARAMETERIZED_TYPE -> {
                    var wrappedArgs = new ArrayList<>();
                    if (rawType.toString().equals(Constant.JAVA_OPTIONAL_CLASS)) {
                        var buildExtractorString = TypeUtils.buildExtractorWrappedString(
                                new WrappedContext(fieldTypeName, EXTRACTOR_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs
                        );
                        var buildString = String.format(createObjectOptionalFieldTemplate, buildExtractorString);
                        var args = new ArrayList<Object>(List.of(optionalValueName, fieldName));
                        args.add(fieldName);
                        args.add(OPTION_CONVERTERS_CLASS);
                        args.add(optionalValueName);
                        args.addAll(wrappedArgs);
                        args.addAll(List.of(ClassName.get(Optional.class), ClassName.get(Optional.class)));
                        code = CodeBlock.builder().add(buildString, args.toArray()).build();
                    } else {
                        wrappedArgs.add(Constant.EITHER_SUFFIX_FUNCTION.apply(fieldName));
                        var buildExtractorString = TypeUtils.buildExtractorWrappedString(
                                new WrappedContext(fieldTypeName, EXTRACTOR_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs
                        );
                        var extractMethodString = String.format("var $L = (%s).extract($L.get());", buildExtractorString);
                        var args = new ArrayList<>();
                        args.addAll(beforeExtractArgs);
                        args.addAll(wrappedArgs);
                        args.add(optionalValueName);
                        args.addAll(afterExtractArgs);
                        code = CodeBlock.builder().add(String.format(createObjectFieldTemplate, extractMethodString), args.toArray()).build();
                    }
                }
                case MAP_PARAMETERIZED_TYPE -> {
                }
            }
            if (code != null) {
                fieldCodes.put(fieldName, code);
            }
        }
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_OBJECT_VALUE_CLASS, typeName);
        var applyCode = CodeBlock.builder().add("""
                return switch (input) {
                    case $T obj -> {
                        yield $T.apply($L.apply(obj));
                    }
                    default -> throw new RuntimeException("Expected ObjectValue");
                };
                """, List.of(SYMPHONYQL_OBJECT_VALUE_CLASS, RIGHT_CLASS, Constant.CREATE_OBJECT_FUNCTION).toArray()).build();
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
                .addParameter(SYMPHONYQL_OBJECT_VALUE_CLASS, "obj")
                .returns(typeName);

        var join = new StringJoiner(", ");
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
        var returnType = ParameterizedTypeName.get(EXTRACTOR_CLASS, typeName);
        // new ArgumentExtractor<T>
        var extractAnonymousObject = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(EXTRACTOR_CLASS, typeName))
                // public Either<SymphonyQLError.ArgumentError, InputObject> extract(SymphonyQLInputValue input)
                .addMethod(MethodSpec.methodBuilder("extract")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(SYMPHONYQL_INPUTVALUE_CLASS, "input")
                        .returns(ParameterizedTypeName.get(EITHER_CLASS, SYMPHONYQL_ERROR_CLASS, typeName))
                        .addCode(applyCode).build());

        // private final static Function<SymphonyQLInputValue, T> function = new Function<SymphonyQLInputValue, T>{ };  
        builder.addField(fieldSpec);

        // public static final ArgumentExtractor<T> extractor = extractor(); 
        builder.addField(assignFieldSpec(returnType, Constant.EXTRACTOR_METHOD_NAME));

        builder.addMethod(
                // private static ArgumentExtractor<T> extractor(){ };
                MethodSpec.methodBuilder(Constant.EXTRACTOR_METHOD_NAME.toLowerCase())
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addStatement("return $L", extractAnonymousObject.build()).returns(returnType)
                        .build());
    }
}
