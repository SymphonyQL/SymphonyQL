package symphony.apt.generator;

import java.util.function.Function;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.Validate;
import symphony.apt.util.ProcessorUtils;
import symphony.apt.util.TypeUtils;

public final class CodeGeneratorContext {

  private final TypeElement typeElement;
  private final String packageName;

  private CodeGeneratorContext(final TypeElement typeElement, final String packageName) {
    this.typeElement = typeElement;
    this.packageName = packageName;
  }

  public static CodeGeneratorContext create(final TypeElement typeElement) {
    Validate.notNull(typeElement, "Type element must be defined");

    final String packageName = ProcessorUtils.packageName(typeElement);
    return new CodeGeneratorContext(typeElement, packageName);
  }

  public TypeElement getTypeElement() {
    return typeElement;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getClassName(final Function<String, String> nameModifier) {
    return nameModifier.apply(TypeUtils.getName(getTypeElement()));
  }
}
