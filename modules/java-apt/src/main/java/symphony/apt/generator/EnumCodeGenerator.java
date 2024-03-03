package symphony.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import symphony.apt.Constant;
import symphony.apt.annotation.EnumSchema;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ModelUtils;
import symphony.apt.util.SourceTextUtils;
import symphony.apt.util.TypeUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
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
        var typeName = TypeUtils.getTypeName(typeElement);
        var returnType = ParameterizedTypeName.get(SCHEMA_CLASS, typeName);
        var stringClass = ClassName.get(String.class);
        var serializeFunctionType = ParameterizedTypeName.get(ClassName.get(Function.class), typeName, stringClass);
        var realName = getName(typeElement).orElse(TypeUtils.getSimpleName(typeElement));
        
        var builderSchema = MethodSpec.methodBuilder(Constant.SCHEMA_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(returnType)
                .addStatement("$T<$T> newEnum = $T.newEnum()", ENUM_BUILDER_CLASS, typeName, ENUM_BUILDER_CLASS)
                .addStatement("newEnum.description($L)", getDescription(typeElement))
                .addStatement("newEnum.name($S)", realName);

        builderSchema.addCode(SourceTextUtils.lines("""
                newEnum.serialize(new $T() {
                    @Override
                    public String apply($T en) {
                        return en.name();
                    }
                });
                """), serializeFunctionType, typeName);

        for (var element : variables) {
            var name = element.getSimpleName().toString();
            var annotationVarargs = getAnnotationVarargs(element);
            var args = new ArrayList<>();
            args.addAll(List.of(BUILD_ENUM_VALUE_FUNCTION_TYPE, ENUM_VALUE_CLASS, ENUM_VALUE_BUILDER_CLASS, name));
            args.addAll(annotationVarargs);
            builderSchema.addCode(
                    CodeBlock.builder().add("""
                            newEnum.value(new $T() {
                                @Override
                                public $T apply($T builder) {
                                    return builder
                                            .name($S)
                                            .description($L)
                                            .isDeprecated($L)
                                            .deprecationReason($L)
                                            .build();
                                }
                            });
                            """, args.toArray()).build());
        }

        builderSchema.addStatement("return newEnum.build()");

        builder.addMethod(builderSchema.build());
        builder.addField(assignFieldSpec(returnType, Constant.SCHEMA_METHOD_NAME));
    }

}
