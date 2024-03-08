package symphony.apt.generator;

import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import symphony.apt.annotation.ObjectSchema;
import symphony.apt.util.MessageUtils;

public class ObjectCodeGenerator extends GeneratedCodeGenerator {

  @Override
  public Class<ObjectSchema> getAnnotation() {
    return ObjectSchema.class;
  }

  @Override
  protected void generateBody(final CodeGeneratorContext context, final TypeSpec.Builder builder) {
    var typeElement = context.getTypeElement();
    var annotation = typeElement.getAnnotation(ObjectSchema.class);
    var modifiers = typeElement.getModifiers();
    var kind = typeElement.getKind();

    if (annotation != null
        && !modifiers.contains(Modifier.ABSTRACT)
        && (kind == ElementKind.RECORD)) {
      generateObject(OBJECT_BUILDER_CLASS, builder, typeElement);
    } else {
      MessageUtils.message(
          Diagnostic.Kind.WARNING, "@ObjectSchema only support on record class: " + typeElement);
    }
  }
}
