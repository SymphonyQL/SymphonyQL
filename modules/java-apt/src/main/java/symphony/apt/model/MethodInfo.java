package symphony.apt.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.StringUtils;
import symphony.apt.util.TypeUtils;

public class MethodInfo {

  private final String name;
  private final TypeMirror returnType;
  private final List<TypeMirror> parameterTypes;
  private final ExecutableElement element;

  public MethodInfo(
      final String name,
      final TypeMirror returnType,
      final List<TypeMirror> parameterTypes,
      final ExecutableElement element) {
    this.name = name;
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
    this.element = element;
  }

  public static MethodInfo create(final ExecutableElement element) {
    return new MethodInfo(
        TypeUtils.getSimpleName(element),
        element.getReturnType(),
        TypeUtils.asTypes(element.getParameters()),
        element);
  }

  public static Set<ExecutableElement> convert(final Collection<MethodInfo> info) {
    final Set<ExecutableElement> methods = new HashSet<>();
    for (final MethodInfo method : info) {
      methods.add(method.getElement());
    }
    return methods;
  }

  public static MethodInfo find(
      final Collection<MethodInfo> info, final String name, final Collection<TypeMirror> types) {
    for (var method : info) {
      var  methodName = method.getName();
      if (StringUtils.equals(methodName, name)
          && Objects.equals(method.getParameterTypes(), types)) {
        return method;
      }
    }
    return null;
  }

  public final String getName() {
    return name;
  }

  public final TypeMirror getReturnType() {
    return returnType;
  }

  public final List<TypeMirror> getParameterTypes() {
    return parameterTypes;
  }

  public final ExecutableElement getElement() {
    return element;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(getName(), getParameterTypes(), getReturnType());
  }

  @Override
  public final boolean equals(final Object obj) {
    if (obj instanceof MethodInfo other) {
        return Objects.equals(other.getName(), getName())
          && Objects.equals(other.getParameterTypes(), getParameterTypes())
          && Objects.equals(other.getReturnType(), getReturnType());
    }
    return false;
  }
}
