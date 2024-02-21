package symphony.apt.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.time.DateFormatUtils;
import symphony.apt.SymphonyQLProcessor;
import symphony.apt.context.ProcessorContext;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.function.AddSuffix;
import symphony.apt.model.TypeInfo;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.ProcessorUtils;
import symphony.apt.util.SourceTextUtils;
import symphony.apt.util.TypeUtils;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public abstract class GeneratedCodeGenerator implements CodeGenerator {

    protected static final String PACKAGE = "symphony.schema";
    protected static final String ADT_PACKAGE = "symphony.parser.adt.introspection";
    protected static final String BUILDER_PACKAGE = "symphony.schema.javadsl";
    protected static final String SUFFIX = "Schema";
    protected static final String methodName = SUFFIX.toLowerCase();
    protected static final ClassName schemaClass = ClassName.get(PACKAGE, "Schema");
    protected static final ClassName fieldBuilderClass = ClassName.get(BUILDER_PACKAGE, "FieldBuilder");
    protected static final ClassName fieldClass = ClassName.get(ADT_PACKAGE, "__Field");

    private final Function<String, String> nameModifier = new AddSuffix(SUFFIX);

    @Override
    public final void generate(final CodeGeneratorContext context) throws Exception {
        final TypeElement typeElement = context.getTypeElement();
        final String packageName = context.getPackageName();
        final String className = context.getClassName(getNameModifier());

        final JavaFileObject file = ProcessorUtils.createSourceFile(typeElement, packageName, className);
        try (
                final Writer writer = file.openWriter();
                final PrintWriter printWriter = new PrintWriter(writer)
        ) {
            final TypeSpec.Builder typeSpecBuilder = generateCommon(className);
            generateBody(context, typeSpecBuilder);

            final TypeSpec typeSpec = typeSpecBuilder.build();
            final JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                    .indent(SourceTextUtils.INDENT)
                    .skipJavaLangImports(true)
                    .build();

            final String sourceCode = javaFile.toString();
            printWriter.write(sourceCode);
            printWriter.flush();
        }
    }

    public final Function<String, String> getNameModifier() {
        return nameModifier;
    }


    protected abstract void generateBody(
            CodeGeneratorContext context, TypeSpec.Builder builder
    ) throws Exception;


    private TypeSpec.Builder generateCommon(final String className) {
        final TypeSpec.Builder typeSpecBuilder = createTypeSpecBuilder(className);
        typeSpecBuilder.addModifiers(Modifier.PUBLIC);
        typeSpecBuilder
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


    private MethodSpec.Builder generateObjectBuilder(
            final ParameterizedTypeName returnType,
            final ClassName objectBuilderClass,
            final TypeElement typeElement
    ) {
        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        return MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType)
                .addStatement("$T<$T> newObject = $T.newObject()", objectBuilderClass, parameterizedTypeName, objectBuilderClass)
                .addStatement("newObject.name($S)", TypeUtils.getName(typeElement));
    }

    protected void generateObject(
            final String builderName,
            final TypeSpec.Builder builder,
            final TypeElement typeElement
    ) {
        var variables = ModelUtils.getVariables(typeElement, ModelUtils.createHasFieldPredicate(typeElement));

        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(schemaClass, parameterizedTypeName);
        var objectBuilderClass = ClassName.get(BUILDER_PACKAGE, builderName);
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), fieldBuilderClass, fieldClass);

        var builderSchema = generateObjectBuilder(returnType, objectBuilderClass, typeElement);

        for (final var entry : variables.entrySet()) {
            var name = entry.getKey();
            var list = new ArrayList<>(List.of(functionType, fieldClass, fieldBuilderClass, name));
            // 不需要包装的类型: String
            if (TypeUtils.isPrimitiveType(entry.getValue())) {
                var args = new ArrayList<>(list);
                args.addAll(List.of(schemaClass,
                        ClassName.get("", entry.getValue().toString())));
                builderSchema.addCode(
                        SourceTextUtils.lines(String.format(addFieldMethodTemplate, "$T.getSchema($S)")),
                        args.toArray()
                );
            } else {
                // 不需要包装的类型，自动生成的对象: CustomObject
                if (TypeUtils.isCustomType(entry.getValue())) {
                    ClassName objectValueType = ClassName.get("", getNameModifier().apply(entry.getValue().toString()));
                    var args = new ArrayList<>(list);
                    args.addAll(List.of(objectValueType, methodName));
                    builderSchema.addCode(SourceTextUtils.lines(String.format(addFieldMethodTemplate, "$T.$N()")), args.toArray()
                    );
                } else if (TypeUtils.isWrappedType(entry.getValue())) {
                    // 需要包装的类型: List<A>, List<List<A>>, List<List<Optional<A>>>, e.g., 
                    var typeInfo = TypeUtils.getTypeInfo(entry.getValue(), 1);
                    var buildSchemaString = TypeUtils.getWrappedCallString(typeInfo);
                    var firstName = TypeUtils.getFirstNestedParameterizedTypeName(entry.getValue());
                    var maxDepth = TypeInfo.calculateMaxDepth(typeInfo);
                    var args = new ArrayList<>(list);
                    for (int i = 0; i < maxDepth - 1; i++) {
                        args.add(schemaClass);
                    }
                    if (TypeUtils.isPrimitiveType(firstName)) {
                        args.addAll(List.of(schemaClass, firstName));
                    } else {
                        ClassName objectValueType = ClassName.get("", getNameModifier().apply(firstName.toString()));
                        args.addAll(List.of(objectValueType, methodName));
                    }

                    builderSchema.addCode(SourceTextUtils.lines(String.format(addFieldMethodTemplate, buildSchemaString)), args.toArray());
                }
            }
        }

        builderSchema.addStatement("return newObject.build()");

        builder.addMethod(builderSchema.build());
    }

}
