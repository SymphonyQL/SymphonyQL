package symphony.apt.generator;

import com.squareup.javapoet.TypeSpec;
import symphony.apt.annotation.InputSchema;
import symphony.apt.util.MessageUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

public class InputObjectCodeGenerator extends GeneratedCodeGenerator {

    @Override
    public final Class<InputSchema> getAnnotation() {
        return InputSchema.class;
    }


    @Override
    protected final void generateBody(final CodeGeneratorContext context, final TypeSpec.Builder builder) {
        var typeElement = context.getTypeElement();
        var annotation = typeElement.getAnnotation(InputSchema.class);
        var modifiers = typeElement.getModifiers();
        var kind = typeElement.getKind();

        if (annotation != null && !modifiers.contains(Modifier.ABSTRACT) && (kind == ElementKind.RECORD)) {
            generateObject("InputObjectBuilder", builder, typeElement);
        } else {
            MessageUtils.message(Diagnostic.Kind.WARNING, "@InputSchema only support on record class: " + typeElement);
        }
    }
}
