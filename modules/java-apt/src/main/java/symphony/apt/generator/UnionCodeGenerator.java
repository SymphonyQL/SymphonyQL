package symphony.apt.generator;

import com.squareup.javapoet.TypeSpec;
import java.util.function.Function;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import symphony.apt.Constant;
import symphony.apt.annotation.UnionSchema;
import symphony.apt.util.MessageUtils;

public class UnionCodeGenerator extends GeneratedCodeGenerator {

  @Override
  public final Class<UnionSchema> getAnnotation() {
    return UnionSchema.class;
  }

  @Override
  public Function<String, String> getNameModifier() {
    return Constant.SCHEMA_SUFFIX;
  }

  @Override
  protected final void generateBody(
      final CodeGeneratorContext context, final TypeSpec.Builder builder) {
    var typeElement = context.getTypeElement();
    var annotation = typeElement.getAnnotation(UnionSchema.class);
    var modifiers = typeElement.getModifiers();
    var kind = typeElement.getKind();

    if (annotation != null
        && modifiers.contains(Modifier.SEALED)
        && (kind == ElementKind.INTERFACE)) {
      generateObject(UNION_BUILDER_CLASS, builder, typeElement);
    } else {
      MessageUtils.message(
          Diagnostic.Kind.WARNING, "@UnionSchema only support on sealed interface: " + typeElement);
    }
  }
}
