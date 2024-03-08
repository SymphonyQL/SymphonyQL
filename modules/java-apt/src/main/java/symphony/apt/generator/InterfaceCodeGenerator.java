package symphony.apt.generator;

import com.squareup.javapoet.TypeSpec;
import java.util.function.Function;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import symphony.apt.Constant;
import symphony.apt.annotation.InterfaceSchema;
import symphony.apt.util.MessageUtils;

public class InterfaceCodeGenerator extends GeneratedCodeGenerator {

  @Override
  public final Class<InterfaceSchema> getAnnotation() {
    return InterfaceSchema.class;
  }

  @Override
  public Function<String, String> getNameModifier() {
    return Constant.SCHEMA_SUFFIX;
  }

  @Override
  protected final void generateBody(
      final CodeGeneratorContext context, final TypeSpec.Builder builder) {
    var typeElement = context.getTypeElement();
    var annotation = typeElement.getAnnotation(InterfaceSchema.class);
    var modifiers = typeElement.getModifiers();
    var kind = typeElement.getKind();

    if (annotation != null
        && modifiers.contains(Modifier.SEALED)
        && (kind == ElementKind.INTERFACE)) {
      generateObject(INTERFACE_BUILDER_CLASS, builder, typeElement);
    } else {
      MessageUtils.message(
          Diagnostic.Kind.WARNING,
          "@InterfaceSchema only support on sealed interface: " + typeElement);
    }
  }
}
