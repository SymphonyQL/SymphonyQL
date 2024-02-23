package symphony.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import symphony.apt.annotation.EnumSchema;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.SourceTextUtils;
import symphony.apt.util.TypeUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.function.Function;

public class EnumCodeGenerator extends GeneratedCodeGenerator {

    @Override
    public final Class<EnumSchema> getAnnotation() {
        return EnumSchema.class;
    }


    @Override
    protected final void generateBody(final CodeGeneratorContext context, final TypeSpec.Builder builder) {
        var typeElement = context.getTypeElement();
        var annotation = typeElement.getAnnotation(EnumSchema.class);
        var kind = typeElement.getKind();

        if (annotation != null && kind == ElementKind.ENUM) {
            generateMethod(builder, typeElement);
        } else {
            MessageUtils.message(Diagnostic.Kind.WARNING, "@EnumSchema only support on enum class: " + typeElement);
        }
    }

    private void generateMethod(final TypeSpec.Builder builder, final TypeElement typeElement) {
        var variables = ModelUtils.getEnumTypes(typeElement).values();
        var schemaClass = ClassName.get(SCHEMA_PACKAGE, "Schema");
        var parameterizedTypeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(schemaClass, parameterizedTypeName);
        var enumBuilderClass = ClassName.get(BUILDER_PACKAGE, "EnumBuilder");
        var enumValueBuilderClass = ClassName.get(BUILDER_PACKAGE, "EnumValueBuilder");
        var enumValueClass = ClassName.get(ADT_PACKAGE, "__EnumValue");
        var functionType = ParameterizedTypeName.get(ClassName.get(Function.class), enumValueBuilderClass, enumValueClass);
        var stringClass = ClassName.get(String.class);
        var serializeFunctionType = ParameterizedTypeName.get(ClassName.get(Function.class), parameterizedTypeName, stringClass);

        var builderSchema = MethodSpec.methodBuilder(SCHEMA_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(returnType)
                .addStatement("$T<$T> newEnum = $T.newEnum()", enumBuilderClass, parameterizedTypeName, enumBuilderClass)
                .addStatement("newEnum.name($S)", TypeUtils.getName(typeElement));

        builderSchema.addCode(SourceTextUtils.lines("""
                newEnum.serialize(new $T() {
                    @Override
                    public String apply($T en) {
                        return en.name();
                    }
                });
                """), serializeFunctionType, parameterizedTypeName);

        for (var entry : variables) {
            var name = entry.getSimpleName().toString();
            builderSchema.addCode(SourceTextUtils.lines("""
                    newEnum.value(new $T() {
                        @Override
                        public $T apply($T builder) {
                            return builder.name($S).build();
                        }
                    });
                    """), functionType, enumValueClass, enumValueBuilderClass, name);
        }

        builderSchema.addStatement("return newEnum.build()");

        builder.addMethod(builderSchema.build());
        builder.addField(assignFieldSpec(returnType, SCHEMA_METHOD_NAME));
    }

}
