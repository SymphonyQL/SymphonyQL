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
import symphony.apt.SymphonyQLProcessor;
import symphony.apt.context.ProcessorContext;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.function.AddSuffix;
import symphony.apt.model.TypeCategory;
import symphony.apt.model.TypeInfo;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.ProcessorUtils;
import symphony.apt.util.SourceTextUtils;
import symphony.apt.util.TypeUtils;

import javax.annotation.processing.FilerException;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public abstract class GeneratedCodeGenerator implements CodeGenerator {

    protected static final String PARSER_PACKAGE = "symphony.parser";
    protected static final String SCHEMA_PACKAGE = "symphony.schema";
    protected static final String ADT_PACKAGE = "symphony.parser.adt.introspection";
    protected static final String BUILDER_PACKAGE = "symphony.schema.javadsl";
    protected static final String SCHEMA_SUFFIX = "Schema";
    protected static final String SCHEMA_METHOD_NAME = "schema";
    protected static final String EXTRACTOR_METHOD_NAME = "extractor";
    // symphonyql schema classes
    protected static final ClassName SYMPHONYQL_SCHEMA_CLASS = ClassName.get(SCHEMA_PACKAGE, "Schema");
    protected static final ClassName SYMPHONYQL_FIELD_BUILDER_CLASS = ClassName.get(BUILDER_PACKAGE, "FieldBuilder");
    protected static final ClassName SYMPHONYQL_FIELD_CLASS = ClassName.get(ADT_PACKAGE, "__Field");
    // symphonyql classes
    protected static final ClassName SYMPHONYQL_EXTRACTOR_CLASS = ClassName.get(SCHEMA_PACKAGE, "ArgumentExtractor");
    protected static final ClassName SYMPHONYQL_INPUTVALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLInputValue");
    protected static final ClassName SYMPHONYQL_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue");
    protected static final ClassName SYMPHONYQL_ERROR_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLError", "ArgumentError");
    protected static final ClassName SYMPHONYQL_OBJECT_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLInputValue", "ObjectValue");
    protected static final ClassName SYMPHONYQL_ENUM_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue", "EnumValue");
    protected static final ClassName SYMPHONYQL_STRING_VALUE_CLASS = ClassName.get(PARSER_PACKAGE, "SymphonyQLValue", "StringValue");

    private final Function<String, String> nameModifier = new AddSuffix(SCHEMA_SUFFIX);

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
        try (
                var writer = file.openWriter();
                var printWriter = new PrintWriter(writer)
        ) {
            var typeSpecBuilder = generateCommon(className);
            generateBody(context, typeSpecBuilder);

            var typeSpec = typeSpecBuilder.build();
            var javaFile = JavaFile.builder(packageName, typeSpec)
                    .indent(SourceTextUtils.INDENT)
                    .skipJavaLangImports(true)
                    .build();

            var sourceCode = javaFile.toString();
            printWriter.write(sourceCode);
            printWriter.flush();
        }
    }

    public Function<String, String> getNameModifier() {
        return nameModifier;
    }


    protected abstract void generateBody(
            CodeGeneratorContext context, TypeSpec.Builder builder
    ) throws Exception;


    private TypeSpec.Builder generateCommon(final String className) {
        final TypeSpec.Builder typeSpecBuilder = createTypeSpecBuilder(className);
        typeSpecBuilder.addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.FINAL)
                .addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addStatement("throw new $T()", UnsupportedOperationException.class)
                                .build()
                );

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
        typeSpecBuilder.addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "all")
                        .build());
    }

    private void addGeneratedAnnotation(
            final TypeSpec.Builder typeSpecBuilder, final ProcessorContext processorContext
    ) {
        final AnnotationSpec.Builder annotationBuilder =
                AnnotationSpec.builder(ClassName.get("javax.annotation", "Generated"))
                        .addMember("value", "$S", SymphonyQLProcessor.class.getName());

        if (processorContext.isAddGeneratedDate()) {
            final String currentTime =
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date());

            annotationBuilder.addMember("date", "$S", currentTime);
        }

        typeSpecBuilder.addAnnotation(annotationBuilder.build());
    }

    private static final String addFieldMethodTemplate = """
            newObject.field(new $T() {
                @Override
                public $T apply($T builder) {
                    return builder.name($S).schema(%s).build();
                }
            });
            """;

    private MethodSpec.Builder objectMethodBuilder(
            final ParameterizedTypeName returnType,
            final ClassName objectBuilderClass,
            final TypeElement typeElement
    ) {
        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        return MethodSpec
                .methodBuilder(SCHEMA_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(returnType)
                .addStatement("$T<$T> newObject = $T.newObject()", objectBuilderClass, parameterizedTypeName, objectBuilderClass)
                .addStatement("newObject.name($S)", TypeUtils.getName(typeElement));
    }

    // 集合类型都是 Schema.createXX、ArgumentExtractor.createXX 包装而成的
    // 根据 getSchemaWrappedString 和 getExtractorWrappedString 可以计算出所需的 Schema 或 ArgumentExtractor 的数量 
    protected List<?> getOneParameterizedTypeArgs(String fieldName, TypeName fieldTypeName, ClassName className, TypeInfo typeInfo, List<TypeCategory> typeCategories) {
        var args = new ArrayList<>();
        var eitherValueName = fieldName + "Either";
        var typeTuple = TypeUtils.getFinalParameterizedTypes(fieldTypeName);
        if (className.equals(SYMPHONYQL_EXTRACTOR_CLASS)) {
            args.add(eitherValueName);
        }

        MessageUtils.note(typeCategories.toString());
        var parentParameterizedType = false;
        for (var category : typeCategories) {
            switch (category) {
                case SYSTEM_TYPE -> {
                    if (parentParameterizedType && className.equals(SYMPHONYQL_EXTRACTOR_CLASS)) {
                        args.add(ParameterizedTypeName.get(className, typeTuple.getKey()));
                        parentParameterizedType = false;
                    }
                    args.add(className);
                    args.add(typeTuple.getKey());
                }
                case CUSTOM_OBJECT_TYPE -> {
                    ClassName expectedObjectType = ClassName.get("", getNameModifier().apply(typeTuple.getKey().toString()));
                    var methodName = className.equals(SYMPHONYQL_EXTRACTOR_CLASS) ? EXTRACTOR_METHOD_NAME : SCHEMA_METHOD_NAME;
                    args.addAll(List.of(expectedObjectType, methodName));
                }
                case ONE_PARAMETERIZED_TYPE -> {
                    parentParameterizedType = true;
                    args.add(className);
                }
                default -> {
                }
            }

        }

        MessageUtils.note(args.toString());
        return args;
    }


    protected void generateObject(
            final String builderName,
            final TypeSpec.Builder builder,
            final TypeElement typeElement
    ) {
        var variables = ModelUtils.getVariableTypes(typeElement, ModelUtils.createHasFieldPredicate(typeElement));

        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(SYMPHONYQL_SCHEMA_CLASS, parameterizedTypeName);
        var objectBuilderClass = ClassName.get(BUILDER_PACKAGE, builderName);
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), SYMPHONYQL_FIELD_BUILDER_CLASS, SYMPHONYQL_FIELD_CLASS);

        var builderSchema = objectMethodBuilder(returnType, objectBuilderClass, typeElement);

        // 主要按照几个大类分别处理，可能有漏
        // 1.预定义标量或基本类型
        // 2.用户定义的对象类型
        // 3.集合类型
        // 4.遇到不支持的自定义对象：A，可以在同包下创建一个类 ASchema，并编写一个静态的 schema 方法返回Schema<A>
        for (final var entry : variables.entrySet()) {
            var name = entry.getKey();
            var type = TypeUtils.getTypeName(entry.getValue());
            var list = new ArrayList<>(List.of(functionType, SYMPHONYQL_FIELD_CLASS, SYMPHONYQL_FIELD_BUILDER_CLASS, name));
            switch (TypeUtils.getTypeCategory(type)) {
                case SYSTEM_TYPE -> {
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(SYMPHONYQL_SCHEMA_CLASS,
                            ClassName.get("", type.toString())));
                    builderSchema.addCode(
                            SourceTextUtils.lines(String.format(addFieldMethodTemplate, "$T.getSchema($S)")),
                            args.toArray()
                    );
                }
                case CUSTOM_OBJECT_TYPE -> {
                    ClassName expectedObjectType = ClassName.get("", getNameModifier().apply(type.toString()));
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(expectedObjectType, SCHEMA_METHOD_NAME));
                    builderSchema.addCode(SourceTextUtils.lines(String.format(addFieldMethodTemplate, "$T.$N")), args.toArray());
                }
                case ONE_PARAMETERIZED_TYPE -> {
                    var args = new ArrayList<>(list);
                    var typeInfo = TypeUtils.getTypeInfo(type, 1);
                    var types = new ArrayList<TypeCategory>();
                    var buildSchemaString = TypeUtils.getSchemaWrappedString(typeInfo, types);
                    MessageUtils.note(buildSchemaString);
                    args.addAll(getOneParameterizedTypeArgs(name, type, SYMPHONYQL_SCHEMA_CLASS, typeInfo, types));
                    builderSchema.addCode(CodeBlock.builder().add(String.format(addFieldMethodTemplate, buildSchemaString), args.toArray()).build());
                }
                case TWO_PARAMETERIZED_TYPES -> {
                }
            }
        }

        builderSchema.addStatement("return newObject.build()");
        builder.addMethod(builderSchema.build());
        builder.addField(assignFieldSpec(returnType, SCHEMA_METHOD_NAME));
    }

    protected FieldSpec assignFieldSpec(TypeName returnType, String methodName) {
        return FieldSpec.builder(returnType, methodName, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).initializer(methodName + "()").build();
    }

}
