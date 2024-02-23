package symphony.apt.util;

import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import symphony.apt.model.MethodInfo;

public final class ModelUtils {

  private ModelUtils() {
    throw new UnsupportedOperationException();
  }

  public static String getName(final AnnotationMirror annotation) {
    return annotation.getAnnotationType().toString();
  }

  public static Predicate<Element> createHasFieldPredicate(final TypeElement typeElement) {
    final Collection<MethodInfo> methods = findImplementedMethods(typeElement);
    return element -> {
      final String name = TypeUtils.getName(element);
      final MethodInfo m = MethodInfo.find(methods, name, Collections.emptyList());

      if (m != null) {
        final ExecutableElement mElement = m.getElement();
        return TypeUtils.hasAnyModifier(mElement, Modifier.PUBLIC);
      }
      return false;
    };
  }

  public static Map<String, Element> getEnumTypes(final TypeElement typeElement) {
    var variables =
        typeElement.getEnclosedElements().stream()
            .filter(t -> t.getKind().equals(ElementKind.ENUM_CONSTANT))
            .toList();
    final Map<String, Element> result = new LinkedHashMap<>();
    for (var variable : variables) {
      final String variableName = TypeUtils.getName(variable);
      result.put(variableName, variable);
    }
    return result;
  }

  public static Map<String, VariableElement> getVariableTypes(
      final TypeElement typeElement, final Predicate<Element> predicate) {
    final List<? extends Element> elements = typeElement.getEnclosedElements();
    final List<VariableElement> variables = ElementFilter.fieldsIn(elements);
    final Map<String, VariableElement> result = new LinkedHashMap<>();

    for (final VariableElement variable : variables) {
      if (predicate.test(variable)) {
        final String variableName = TypeUtils.getName(variable);
        result.put(variableName, variable);
      }
    }

    return result;
  }

  public static Map<String, TypeName> getVariables(
      final TypeElement typeElement, final Predicate<Element> predicate) {
    return getVariableTypes(typeElement, predicate).entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), TypeUtils.getTypeName(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static Pair<Collection<MethodInfo>, Collection<MethodInfo>> calculateMethodInfo(
      final TypeElement rootElement) {
    final Collection<MethodInfo> unimplementedMethods = new LinkedHashSet<>();
    final Collection<MethodInfo> implementedMethods = new LinkedHashSet<>();
    TypeElement root = rootElement;

    while (root != null) {
      final List<ExecutableElement> methods = ElementFilter.methodsIn(root.getEnclosedElements());
      for (final ExecutableElement method : methods) {
        if (TypeUtils.hasAnyModifier(method, Modifier.ABSTRACT)) {
          unimplementedMethods.add(MethodInfo.create(method));
        } else {
          implementedMethods.add(MethodInfo.create(method));
        }
      }

      final List<TypeElement> interfaces = getInterfaces(rootElement);
      for (final TypeElement element : interfaces) {
        final List<ExecutableElement> interfaceMethods =
            ElementFilter.methodsIn(element.getEnclosedElements());

        for (final ExecutableElement method : interfaceMethods) {
          unimplementedMethods.add(MethodInfo.create(method));
        }
      }

      root = TypeUtils.getSuperclass(root);
    }

    unimplementedMethods.removeAll(implementedMethods);

    return ImmutablePair.of(implementedMethods, unimplementedMethods);
  }

  public static Collection<MethodInfo> findImplementedMethods(final TypeElement rootElement) {
    final Pair<Collection<MethodInfo>, Collection<MethodInfo>> methodInfo =
        calculateMethodInfo(rootElement);
    return methodInfo.getLeft();
  }

  public static List<TypeElement> getInterfaces(final TypeElement root) {
    final List<? extends TypeMirror> interfaces = root.getInterfaces();
    final List<TypeElement> result = new ArrayList<>();

    for (final TypeMirror interfaceMirror : interfaces) {
      result.add(ProcessorUtils.getWrappedType(interfaceMirror));
    }
    return result;
  }
}
