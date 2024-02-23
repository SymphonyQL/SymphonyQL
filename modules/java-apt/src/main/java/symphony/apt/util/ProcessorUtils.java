package symphony.apt.util;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import symphony.apt.context.ProcessorContextHolder;

public final class ProcessorUtils {

  private ProcessorUtils() {
    throw new UnsupportedOperationException();
  }

  public static JavaFileObject createSourceFile(
      final TypeElement baseElement, final String packageName, final String className)
      throws Exception {
    final var env = ProcessorContextHolder.getProcessingEnvironment();
    final var filer = env.getFiler();

    return filer.createSourceFile(
        packageName + SourceTextUtils.PACKAGE_SEPARATOR + className, baseElement);
  }

  public static String packageName(final TypeElement element) {
    final var env = ProcessorContextHolder.getProcessingEnvironment();
    final var elementUtils = env.getElementUtils();
    final var packageElement = elementUtils.getPackageOf(element);
    return packageElement.getQualifiedName().toString();
  }

  public static TypeElement getWrappedType(final TypeMirror mirror) {
    final var env = ProcessorContextHolder.getProcessingEnvironment();
    final var typeUtils = env.getTypeUtils();

    final var kind = mirror.getKind();
    final var primitive = kind.isPrimitive();

    if (primitive) {
      return typeUtils.boxedClass((PrimitiveType) mirror);
    }
    return (TypeElement) typeUtils.asElement(mirror);
  }
}
