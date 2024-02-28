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

import javax.annotation.processing.FilerException;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static symphony.apt.Constant.ADT_PACKAGE;
import static symphony.apt.Constant.BUILDER_PACKAGE;
import static symphony.apt.Constant.INPUT_OBJECT_BUILDER;
import static symphony.apt.Constant.PARSER_PACKAGE;
import static symphony.apt.Constant.SCHEMA_PACKAGE;

public abstract class GeneratedCodeGenerator implements CodeGenerator {
    // symphonyql schema classes
    protected static final ClassName SCHEMA_CLASS = ClassName.get(SCHEMA_PACKAGE, "Schema");
    protected static final ClassName FIELD_BUILDER_CLASS = ClassName.get(BUILDER_PACKAGE, "FieldBuilder");
    protected static final ClassName FIELD_CLASS = ClassName.get(ADT_PACKAGE, "__Field");
    // symphonyql classes
    public static final ClassName EXTRACTOR_CLASS = ClassName.get(SCHEMA_PACKAGE, "ArgumentExtractor");
    protected static final ClassName SYMPHONYQL_INPUTVALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLInputValue");
    protected static final ClassName SYMPHONYQL_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue");
    protected static final ClassName SYMPHONYQL_ERROR_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLError", "ArgumentError");
    protected static final ClassName SYMPHONYQL_OBJECT_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLInputValue", "ObjectValue");
    protected static final ClassName SYMPHONYQL_ENUM_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue", "EnumValue");
    protected static final ClassName SYMPHONYQL_STRING_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue", "StringValue");
    protected static final ClassName ENUM_BUILDER_CLASS = ClassName.get(BUILDER_PACKAGE, "EnumBuilder");
    protected static final ClassName ENUM_VALUE_BUILDER_CLASS = ClassName.get(BUILDER_PACKAGE, "EnumValueBuilder");
    protected static final ClassName ENUM_VALUE_CLASS = ClassName.get(ADT_PACKAGE, "__EnumValue");
    // function
    protected static final ParameterizedTypeName BUILD_FIELD_FUNCTION_TYPE = ParameterizedTypeName.get(ClassName.get(Function.class),
            FIELD_BUILDER_CLASS, FIELD_CLASS);
    protected static final ParameterizedTypeName BUILD_ENUM_VALUE_FUNCTION_TYPE = ParameterizedTypeName.get(ClassName.get(Function.class), ENUM_VALUE_BUILDER_CLASS, ENUM_VALUE_CLASS);

    private static final String addInputFieldMethodTemplate = """
            newObject.field(
                    new $T() {
                        @Override
                        public $T apply($T builder) {
                            return builder.name($S).schema(%s).build();
                        }
                    }
            );
            """;

    private static final String addFieldMethodTemplate = """
            newObject.field(
                new $T() {
                    @Override
                    public $T apply($T builder) {
                        return builder.name($S).schema(%s).build();
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

    private static final String addFieldWithArgMethodTemplate = """
            newObject.fieldWithArg(
                    new $T() {
                        @Override
                        public $T apply($T builder) {
                            return builder.name($S).schema(%s).build();
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

    public Function<String, String> getNameModifier() {
        return Constant.SCHEMA_SUFFIX;
    }


    protected abstract void generateBody(CodeGeneratorContext context, TypeSpec.Builder builder) throws Exception;


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

    protected void generateObjectWithArg(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var variables = ModelUtils.getVariableTypes(typeElement, ModelUtils.createHasFunctionalFieldPredicate(typeElement));
        var typeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(SCHEMA_CLASS, typeName);
        var objectBuilderClass = ClassName.get(BUILDER_PACKAGE, Constant.OBJECT_BUILDER);
        var builderSchema = objectMethodBuilder(returnType, objectBuilderClass, typeElement);
        var schemaArgs = new ArrayList<Object>(List.of(BUILD_FIELD_FUNCTION_TYPE, FIELD_CLASS, FIELD_BUILDER_CLASS));

        for (final var entry : variables.entrySet()) {
            var name = entry.getKey();
            var type = TypeUtils.getTypeName(entry.getValue());
            var rawType = TypeUtils.getRawTypeName(type);
            switch (TypeUtils.classifyType(rawType)) {
                case FUNCTION_PARAMETERIZED_TYPE, SUPPLIER_PARAMETERIZED_TYPE -> {
                    var functionSchemaArgs = new ArrayList<>();
                    var buildInputSchemaString =
                            TypeUtils.buildSchemaWrappedString(new WrappedContext(type, SCHEMA_CLASS, getNameModifier(), EXTRACTOR_CLASS), functionSchemaArgs);

                    var fieldValueType = entry.getValue().asType().getKind().isPrimitive() ? TypeUtils.getTypeName(entry.getValue(), true) : type;
                    var fieldFunctionType = ParameterizedTypeName.get(ClassName.get(Function.class), typeName, fieldValueType);
                    var args = new ArrayList<Object>(schemaArgs);
                    args.add(name);
                    args.addAll(functionSchemaArgs);
                    args.addAll(List.of(fieldFunctionType, type, typeName, name));
                    var string = String.format(addFieldWithArgMethodTemplate, buildInputSchemaString);
                    MessageUtils.note(name + " -> " + functionSchemaArgs);
                    MessageUtils.note(name + " -> " + buildInputSchemaString);
                    builderSchema.addCode(CodeBlock.builder().add(string, args.toArray()).build());
                }
            }
        }

        builderSchema.addStatement("return newObject.build()");
        builder.addMethod(builderSchema.build());
        builder.addField(assignFieldSpec(returnType, Constant.SCHEMA_METHOD_NAME));
    }

    private MethodSpec.Builder objectMethodBuilder(
            final ParameterizedTypeName returnType,
            final ClassName objectBuilderClass,
            final TypeElement typeElement
    ) {
        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        return MethodSpec.methodBuilder(Constant.SCHEMA_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(returnType)
                .addStatement("$T<$T> newObject = $T.newObject()", objectBuilderClass, parameterizedTypeName, objectBuilderClass)
                .addStatement("newObject.name($S)", TypeUtils.getSimpleName(typeElement));
    }

    private void generateObjectField(MethodSpec.Builder builderSchema, TypeElement typeElement, Map.Entry<String, VariableElement> entry, List<Object> list) {
        var name = entry.getKey();
        var fieldElement = entry.getValue();
        var typeName = TypeUtils.getTypeName(typeElement);
        var type = TypeUtils.getTypeName(fieldElement);
        var rawType = TypeUtils.getRawTypeName(type);
        var fieldValueType = fieldElement.asType().getKind().isPrimitive() ? TypeUtils.getTypeName(fieldElement, true) : type;
        var fieldFunctionType = ParameterizedTypeName.get(ClassName.get(Function.class), typeName, fieldValueType);
        var fieldValueArgs = List.of(fieldFunctionType, fieldValueType, typeName, name);
        switch (TypeUtils.classifyType(rawType)) {
            case DEFAULT_OR_PRIMITIVE_TYPE -> {
                var args = new ArrayList<>(list);
                args.addAll(List.of(SCHEMA_CLASS, ClassName.get("", type.toString())));
                args.addAll(fieldValueArgs);
                builderSchema.addCode(SourceTextUtils.lines(String.format(addFieldMethodTemplate, "$T.getSchema($S)")), args.toArray());
            }
            case CUSTOM_OBJECT_TYPE -> {
                ClassName expectedObjectType = ClassName.get("", getNameModifier().apply(type.toString()));
                var args = new ArrayList<>(list);
                args.addAll(List.of(expectedObjectType, Constant.SCHEMA_METHOD_NAME));
                args.addAll(fieldValueArgs);
                builderSchema.addCode(SourceTextUtils.lines(String.format(addFieldMethodTemplate, "$T.$N")), args.toArray());
            }
            case COLLECTION_PARAMETERIZED_TYPE, MAP_PARAMETERIZED_TYPE -> {
                var args = new ArrayList<>(list);
                var wrappedArgs = new ArrayList<>();
                var buildSchemaString = TypeUtils.buildSchemaWrappedString(
                        new WrappedContext(type, SCHEMA_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs
                );
                MessageUtils.note(name + " -> " + buildSchemaString);
                MessageUtils.note(name + " -> " + wrappedArgs);
                args.addAll(wrappedArgs);
                args.addAll(fieldValueArgs);
                builderSchema.addCode(CodeBlock.builder().add(
                        String.format(addFieldMethodTemplate, buildSchemaString), args.toArray()).build()
                );
            }
        }
    }

    private void generateInputObjectField(MethodSpec.Builder builderSchema, Map.Entry<String, VariableElement> entry, List<Object> list) {
        var name = entry.getKey();
        var fieldElement = entry.getValue();
        var type = TypeUtils.getTypeName(fieldElement);
        var rawType = TypeUtils.getRawTypeName(type);
        switch (TypeUtils.classifyType(rawType)) {
            case DEFAULT_OR_PRIMITIVE_TYPE -> {
                var args = new ArrayList<>(list);
                args.addAll(List.of(SCHEMA_CLASS, ClassName.get("", type.toString())));
                builderSchema.addCode(SourceTextUtils.lines(String.format(addInputFieldMethodTemplate, "$T.getSchema($S)")), args.toArray());
            }
            case CUSTOM_OBJECT_TYPE -> {
                ClassName expectedObjectType = ClassName.get("", (TypeUtils.isEnumType(type) ? Constant.SCHEMA_SUFFIX : getNameModifier()).apply(type.toString()));
                var args = new ArrayList<>(list);
                args.addAll(List.of(expectedObjectType, Constant.SCHEMA_METHOD_NAME));
                builderSchema.addCode(SourceTextUtils.lines(String.format(addInputFieldMethodTemplate, "$T.$N")), args.toArray());
            }
            case COLLECTION_PARAMETERIZED_TYPE, MAP_PARAMETERIZED_TYPE -> {
                var args = new ArrayList<>(list);
                var wrappedArgs = new ArrayList<>();
                var buildSchemaString = TypeUtils.buildSchemaWrappedString(new WrappedContext(type, SCHEMA_CLASS, getNameModifier(), EXTRACTOR_CLASS), wrappedArgs);
                MessageUtils.note(name + " -> " + buildSchemaString);
                MessageUtils.note(name + " -> " + wrappedArgs);
                args.addAll(wrappedArgs);
                builderSchema.addCode(CodeBlock.builder().add(
                        String.format(addInputFieldMethodTemplate, buildSchemaString), args.toArray()).build()
                );
            }
        }
    }

    protected void generateObject(final String builderName, final TypeSpec.Builder builder, final TypeElement typeElement) {
        var variables = ModelUtils.getVariableTypes(typeElement, ModelUtils.createHasFieldPredicate(typeElement));
        var typeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(SCHEMA_CLASS, typeName);
        var objectBuilderClass = ClassName.get(BUILDER_PACKAGE, builderName);
        var builderSchema = objectMethodBuilder(returnType, objectBuilderClass, typeElement);
        for (final var entry : variables.entrySet()) {
            var name = entry.getKey();
            var list = new ArrayList<>(List.of(BUILD_FIELD_FUNCTION_TYPE, FIELD_CLASS, FIELD_BUILDER_CLASS, name));
            if (INPUT_OBJECT_BUILDER.equals(builderName)) {
                generateInputObjectField(builderSchema, entry, list);
            } else {
                generateObjectField(builderSchema, typeElement, entry, list);
            }
        }

        builderSchema.addStatement("return newObject.build()");
        builder.addMethod(builderSchema.build());
        builder.addField(assignFieldSpec(returnType, Constant.SCHEMA_METHOD_NAME));
    }

    protected FieldSpec assignFieldSpec(TypeName returnType, String methodName) {
        return FieldSpec.builder(returnType, methodName, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer(methodName + "()").build();
    }

}
