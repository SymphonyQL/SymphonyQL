package symphony.apt.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.time.DateFormatUtils;
import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.annotations.java.GQLExcluded;
import symphony.annotations.java.GQLInputName;
import symphony.annotations.java.GQLName;
import symphony.apt.Constant;
import symphony.apt.SymphonyQLProcessor;
import symphony.apt.context.ProcessorContext;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.model.WrappedContext;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.ProcessorUtils;
import symphony.apt.util.SourceTextUtils;
import symphony.apt.util.TypeUtils;
import symphony.parser.SymphonyQLError;
import symphony.parser.SymphonyQLInputValue;
import symphony.parser.SymphonyQLValue;
import symphony.parser.adt.introspection.__EnumValue;
import symphony.parser.adt.introspection.__Field;
import symphony.schema.ArgumentExtractor;
import symphony.schema.Schema;
import symphony.schema.builder.EnumBuilder;
import symphony.schema.builder.EnumValueBuilder;
import symphony.schema.builder.FieldBuilder;
import symphony.schema.builder.InputObjectBuilder;
import symphony.schema.builder.ObjectBuilder;

import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class GeneratedCodeGenerator implements CodeGenerator {
    // symphonyql classes
    protected static final ClassName SCHEMA_CLASS = ClassName.get(Schema.class);
    protected static final ClassName FIELD_BUILDER_CLASS = ClassName.get(FieldBuilder.class);
    protected static final ClassName FIELD_CLASS = ClassName.get(__Field.class);
    protected static final ClassName EXTRACTOR_CLASS = ClassName.get(ArgumentExtractor.class);
    protected static final ClassName SYMPHONYQL_INPUTVALUE_CLASS = ClassName.get(SymphonyQLInputValue.class);
    protected static final ClassName SYMPHONYQL_VALUE_CLASS = ClassName.get(SymphonyQLValue.class);
    protected static final ClassName SYMPHONYQL_ERROR_CLASS = ClassName.get(SymphonyQLError.ArgumentError.class);
    protected static final ClassName SYMPHONYQL_OBJECT_VALUE_CLASS = ClassName.get(SymphonyQLInputValue.ObjectValue.class);
    protected static final ClassName SYMPHONYQL_ENUM_VALUE_CLASS = ClassName.get(SymphonyQLValue.EnumValue.class);
    protected static final ClassName SYMPHONYQL_STRING_VALUE_CLASS = ClassName.get(SymphonyQLValue.StringValue.class);
    protected static final ClassName ENUM_BUILDER_CLASS = ClassName.get(EnumBuilder.class);
    protected static final ClassName ENUM_VALUE_BUILDER_CLASS = ClassName.get(EnumValueBuilder.class);
    protected static final ClassName ENUM_VALUE_CLASS = ClassName.get(__EnumValue.class);
    protected static final ClassName OBJECT_BUILDER_CLASS = ClassName.get(ObjectBuilder.class);
    protected static final ClassName INPUT_OBJECT_BUILDER_CLASS = ClassName.get(InputObjectBuilder.class);

    // function
    protected static final ParameterizedTypeName BUILD_FIELD_FUNCTION_TYPE = ParameterizedTypeName.get(ClassName.get(Function.class),
            FIELD_BUILDER_CLASS, FIELD_CLASS);
    protected static final ParameterizedTypeName BUILD_ENUM_VALUE_FUNCTION_TYPE = ParameterizedTypeName.get(
            ClassName.get(Function.class), ENUM_VALUE_BUILDER_CLASS, ENUM_VALUE_CLASS
    );

    public Function<String, String> getNameModifier() {
        return Constant.SCHEMA_SUFFIX;
    }

    protected abstract void generateBody(CodeGeneratorContext context, TypeSpec.Builder builder) throws Exception;

    private static final String objectInputFieldMethodTemplate = """
            newObject.field(
                    new $T() {
                        @Override
                        public $T apply($T builder) {
                            return builder
                                    .name($S)
                                    .schema(%s)
                                    .description($L)
                                    .isDeprecated($L)
                                    .deprecationReason($L)
                                    .build();
                        }
                    }
            );
            """;

    private static final String objectFieldMethodTemplate = """
            newObject.field(
                    new $T() {
                        @Override
                        public $T apply($T builder) {
                            return builder
                                    .name($S)
                                    .schema(%s)
                                    .description($L)
                                    .isDeprecated($L)
                                    .deprecationReason($L)
                                    .build();
                        }
                    },
                    new $T() {
                        @Override
                        public $T apply($T obj) {
                            return obj.$N();
                        }
                    }
            );
            """;

    private static final String objectFieldWithArgMethodTemplate = """
            newObject.fieldWithArg(
                    new $T() {
                        @Override
                        public $T apply($T builder) {
                            return builder
                                    .name($S)
                                    .schema(%s)
                                    .description($L)
                                    .isDeprecated($L)
                                    .deprecationReason($L)
                                    .build();
                        }
                    },
                    new $T() {
                        @Override
                        public $T apply($T obj) {
                            return obj.$N();
                        }
                    }
            );
            """;

    @Override
    public final void generate(final CodeGeneratorContext context) throws Exception {
        var typeElement = context.getTypeElement();
        var packageName = context.getPackageName();
        var className = context.getClassName(getNameModifier());
        JavaFileObject file = null;
        try {
            file = ProcessorUtils.createSourceFile(typeElement, packageName, className);
        } catch (FilerException e) {
            MessageUtils.note("Attempt to recreate a file for type " + className);
        }
        if (file == null) return;
        try (var writer = file.openWriter(); var printWriter = new PrintWriter(writer)) {
            var typeSpecBuilder = generateCommon(className);
            generateBody(context, typeSpecBuilder);

            var typeSpec = typeSpecBuilder.build();
            var javaFile = JavaFile.builder(packageName, typeSpec).indent(SourceTextUtils.INDENT).skipJavaLangImports(true).build();

            var sourceCode = javaFile.toString();
            printWriter.write(sourceCode);
            printWriter.flush();
        }
    }

    protected void generateObject(
            final ClassName builderName,
            final TypeSpec.Builder builder,
            final TypeElement typeElement
    ) {
        var fieldElements = ModelUtils.getVariableTypes(typeElement, ModelUtils.createHasFieldPredicate(typeElement));
        var typeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(SCHEMA_CLASS, typeName);
        var builderSchema = objectMethodBuilder(returnType, builderName, typeElement);
        for (final var elementEntry : fieldElements.entrySet()) {
            var realName = getName(elementEntry.getValue()).orElse(elementEntry.getKey());
            var args = new ArrayList<>(List.of(BUILD_FIELD_FUNCTION_TYPE, FIELD_CLASS, FIELD_BUILDER_CLASS, realName));
            if (INPUT_OBJECT_BUILDER_CLASS.equals(builderName)) {
                builderSchema.addCode(inputObjectFieldCreator.buildField(typeElement, elementEntry, args));
            } else {
                if (!isExcludedField(elementEntry.getValue())) {
                    builderSchema.addCode(objectFieldCreator.buildField(typeElement, elementEntry, args));
                }
            }
        }

        builderSchema.addStatement("return newObject.build()");
        builder.addMethod(builderSchema.build());
        builder.addField(assignFieldSpec(returnType, Constant.SCHEMA_METHOD_NAME));
    }

    protected static List<Object> getAnnotationVarargs(Element fieldElement) {
        return List.of(
                getDescription(fieldElement),
                getIsDeprecated(fieldElement),
                getDeprecatedReason(fieldElement)
        );
    }

    private interface FieldCreator {
        CodeBlock buildField(
                final TypeElement typeElement,
                final Map.Entry<String, RecordComponentElement> fieldElements,
                final List<Object> list);
    }

    final FieldCreator objectFieldCreator = new ObjectFieldCreator();
    final FieldCreator inputObjectFieldCreator = new InputObjectFieldCreator();

    private class ObjectFieldCreator implements FieldCreator {
        @Override
        public CodeBlock buildField(
                final TypeElement typeElement,
                final Map.Entry<String, RecordComponentElement> fieldElements,
                final List<Object> list
        ) {
            var name = fieldElements.getKey();
            var fieldElement = fieldElements.getValue();
            var typeName = TypeUtils.getTypeName(typeElement);
            var type = TypeUtils.getTypeName(fieldElement);
            var rawType = TypeUtils.getRawTypeName(type);
            var fieldValueType = fieldElement.asType().getKind().isPrimitive() ? TypeUtils.getTypeName(fieldElement, true) : type;
            var fieldFunctionType = ParameterizedTypeName.get(ClassName.get(Function.class), typeName, fieldValueType);
            var fieldValueArgs = List.of(fieldFunctionType, fieldValueType, typeName, name);
            var annotationVarargs = getAnnotationVarargs(fieldElement);
            TypeUtils.classifyType(rawType);
            return switch (TypeUtils.classifyType(rawType)) {
                case DEFAULT_OR_PRIMITIVE_TYPE -> {
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(SCHEMA_CLASS, ClassName.get("", type.toString())));
                    args.addAll(annotationVarargs);
                    args.addAll(fieldValueArgs);
                    yield CodeBlock.builder().add(String.format(objectFieldMethodTemplate, "$T.getSchema($S)"), args.toArray()).build();
                }
                case CUSTOM_OBJECT_TYPE -> {
                    ClassName expectedObjectType = ClassName.get("", getNameModifier().apply(type.toString()));
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(expectedObjectType, Constant.SCHEMA_METHOD_NAME));
                    args.addAll(annotationVarargs);
                    args.addAll(fieldValueArgs);
                    yield CodeBlock.builder().add(String.format(objectFieldMethodTemplate, "$T.$N"), args.toArray()).build();
                }
                case COLLECTION_PARAMETERIZED_TYPE, MAP_PARAMETERIZED_TYPE -> {
                    var args = new ArrayList<>(list);
                    var wrappedArgs = new ArrayList<>();
                    var buildSchemaString = TypeUtils.buildSchemaWrappedString(new WrappedContext(type, SCHEMA_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs);
                    args.addAll(wrappedArgs);
                    args.addAll(annotationVarargs);
                    args.addAll(fieldValueArgs);
                    yield CodeBlock.builder().add(String.format(objectFieldMethodTemplate, buildSchemaString), args.toArray()).build();
                }
                case FUNCTION_PARAMETERIZED_TYPE, SUPPLIER_PARAMETERIZED_TYPE -> {
                    var functionSchemaArgs = new ArrayList<>();
                    var buildInputSchemaString = TypeUtils.buildSchemaWrappedString(new WrappedContext(type, SCHEMA_CLASS, getNameModifier(), EXTRACTOR_CLASS), functionSchemaArgs);
                    var args = new ArrayList<>(list);
                    args.addAll(functionSchemaArgs);
                    args.addAll(annotationVarargs);
                    args.addAll(List.of(fieldFunctionType, type, typeName, name));
                    var string = String.format(objectFieldWithArgMethodTemplate, buildInputSchemaString);
                    yield CodeBlock.builder().add(string, args.toArray()).build();
                }

            };
        }
    }

    private class InputObjectFieldCreator implements FieldCreator {

        @Override
        public CodeBlock buildField(
                final TypeElement typeElement,
                final Map.Entry<String, RecordComponentElement> fieldElements,
                final List<Object> list
        ) {
            var fieldElement = fieldElements.getValue();
            var type = TypeUtils.getTypeName(fieldElement);
            var rawType = TypeUtils.getRawTypeName(type);
            var annotationVarargs = getAnnotationVarargs(fieldElement);
            TypeUtils.classifyType(rawType);
            return switch (TypeUtils.classifyType(rawType)) {
                case DEFAULT_OR_PRIMITIVE_TYPE -> {
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(SCHEMA_CLASS, ClassName.get("", type.toString())));
                    args.addAll(annotationVarargs);
                    yield CodeBlock.builder().add(String.format(objectInputFieldMethodTemplate, "$T.getSchema($S)"), args.toArray()).build();
                }
                case CUSTOM_OBJECT_TYPE -> {
                    ClassName expectedObjectType = ClassName.get("", (TypeUtils.isEnumType(type) ? Constant.SCHEMA_SUFFIX : getNameModifier()).apply(type.toString()));
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(expectedObjectType, Constant.SCHEMA_METHOD_NAME));
                    args.addAll(annotationVarargs);
                    yield CodeBlock.builder().add(String.format(objectInputFieldMethodTemplate, "$T.$N"), args.toArray()).build();
                }
                case COLLECTION_PARAMETERIZED_TYPE, MAP_PARAMETERIZED_TYPE -> {
                    var args = new ArrayList<>(list);
                    var wrappedArgs = new ArrayList<>();
                    var buildSchemaString = TypeUtils.buildSchemaWrappedString(new WrappedContext(type, SCHEMA_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs);
                    args.addAll(wrappedArgs);
                    args.addAll(annotationVarargs);
                    yield CodeBlock.builder().add(String.format(objectInputFieldMethodTemplate, buildSchemaString), args.toArray()).build();
                }
                case FUNCTION_PARAMETERIZED_TYPE, SUPPLIER_PARAMETERIZED_TYPE -> CodeBlock.builder().build();
            };
        }
    }

    private MethodSpec.Builder objectMethodBuilder(
            final ParameterizedTypeName returnType,
            final ClassName objectBuilder,
            final TypeElement typeElement
    ) {
        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        var name = objectBuilder.equals(INPUT_OBJECT_BUILDER_CLASS)
                ? getInputName(typeElement).orElse(TypeUtils.getSimpleName(typeElement) + "Input")
                : getName(typeElement).orElse(TypeUtils.getSimpleName(typeElement));
        return MethodSpec.methodBuilder(Constant.SCHEMA_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(returnType)
                .addStatement("$T<$T> newObject = $T.newObject()", objectBuilder, parameterizedTypeName, objectBuilder)
                .addStatement("newObject.description($L)", getDescription(typeElement))
                .addStatement("newObject.name($S)", name);
    }

    protected static CodeBlock getIsDeprecated(Element element) {
        var deprecated = TypeUtils.getAnnotation(GQLDeprecated.class, element);
        return deprecated != null ? CodeBlock.of("$L", true) : CodeBlock.of("$L", false);
    }

    protected boolean isExcludedField(Element element) {
        var excluded = TypeUtils.getAnnotation(GQLExcluded.class, element);
        return excluded != null;
    }

    protected Optional<String> getInputName(Element element) {
        var inputName = TypeUtils.getAnnotation(GQLInputName.class, element);
        return Optional.ofNullable(inputName != null ? inputName.value() : null);
    }

    protected Optional<String> getName(Element element) {
        var name = TypeUtils.getAnnotation(GQLName.class, element);
        return Optional.ofNullable(name != null ? name.value() : null);
    }

    protected static CodeBlock getDescription(Element element) {
        var description = TypeUtils.getAnnotation(GQLDescription.class, element);
        return description != null ? CodeBlock.of("$T.ofNullable($S)", ClassName.get(Optional.class), description.value()) :
                CodeBlock.of("$T.empty()", ClassName.get(Optional.class));
    }

    protected static CodeBlock getDeprecatedReason(Element element) {
        var deprecated = TypeUtils.getAnnotation(GQLDeprecated.class, element);
        return deprecated != null ? CodeBlock.of("$T.ofNullable($S)", ClassName.get(Optional.class), deprecated.reason()) :
                CodeBlock.of("$T.empty()", ClassName.get(Optional.class));
    }

    protected FieldSpec assignFieldSpec(TypeName returnType, String methodName) {
        return FieldSpec.builder(returnType, methodName, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer(methodName + "()").build();
    }


    private TypeSpec.Builder generateCommon(final String className) {
        final TypeSpec.Builder typeSpecBuilder = createTypeSpecBuilder(className);
        typeSpecBuilder.addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addStatement("throw new $T()", UnsupportedOperationException.class).build());

        final ProcessorContext processorContext = ProcessorContextHolder.getContext();
        if (processorContext.isAddGeneratedAnnotation()) {
            addGeneratedAnnotation(typeSpecBuilder, processorContext);
        }
        if (processorContext.isAddSuppressWarningsAnnotation()) {
            addSuppressWarningsAnnotation(typeSpecBuilder);
        }

        return typeSpecBuilder;
    }

    private TypeSpec.Builder createTypeSpecBuilder(final String className) {
        return TypeSpec.classBuilder(className);
    }

    private void addSuppressWarningsAnnotation(final TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "all").build());
    }

    private void addGeneratedAnnotation(final TypeSpec.Builder typeSpecBuilder, final ProcessorContext processorContext) {
        final AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(
                ClassName.get("javax.annotation", "Generated")).addMember("value", "$S",
                SymphonyQLProcessor.class.getName());

        if (processorContext.isAddGeneratedDate()) {
            final String currentTime = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date());

            annotationBuilder.addMember("date", "$S", currentTime);
        }

        typeSpecBuilder.addAnnotation(annotationBuilder.build());
    }

}
