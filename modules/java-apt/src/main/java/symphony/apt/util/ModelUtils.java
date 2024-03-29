package symphony.apt.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import symphony.apt.Constant;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.model.MethodInfo;

public final class ModelUtils {

  private ModelUtils() {
    throw new UnsupportedOperationException();
  }

  public static String getName(final AnnotationMirror annotation) {
    return annotation.getAnnotationType().toString();
  }

  public static Predicate<Element> createHasFunctionalFieldPredicate(
      final TypeElement typeElement) {
    final Collection<MethodInfo> methods = findImplementedMethods(typeElement);
    return element -> {
      final var name = TypeUtils.getSimpleName(element);
      final var m = MethodInfo.find(methods, name, Collections.emptyList());

      if (m != null) {
        final var mElement = m.getElement();
        if (m.getReturnType().toString().startsWith(Constant.JAVA_FUNCTION_CLASS)
            || m.getReturnType().toString().startsWith(Constant.JAVA_SUPPLIER_CLASS)) {
          return TypeUtils.hasAnyModifier(mElement, Modifier.PUBLIC);
        }
        return false;
      }
      return false;
    };
  }

  public static Predicate<Element> createHasFieldPredicate(final TypeElement typeElement) {
    final Collection<MethodInfo> methods = findImplementedMethods(typeElement);
    return element -> {
      final var name = TypeUtils.getSimpleName(element);
      final var m = MethodInfo.find(methods, name, Collections.emptyList());

      if (m != null) {
        final var mElement = m.getElement();
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
    final var result = new LinkedHashMap<String, Element>();
    for (var variable : variables) {
      final var variableName = TypeUtils.getSimpleName(variable);
      result.put(variableName, variable);
    }
    return result;
  }

  public static Map<String, Element> getPermittedSubclasses(final TypeElement typeElement) {
    final List<? extends TypeMirror> elements = typeElement.getPermittedSubclasses();
    final var result = new LinkedHashMap<String, Element>();
    final var env = ProcessorContextHolder.getProcessingEnvironment();
    final var typeUtils = env.getTypeUtils();

    for (final var element : elements) {
      final var el = typeUtils.asElement(element);
      final var simpleName = TypeUtils.getSimpleName(el);
      result.put(simpleName, el);
    }

    return result;
  }

  public static Map<String, RecordComponentElement> getRecordComponents(
      final TypeElement typeElement) {
    final var elements = typeElement.getRecordComponents();
    final var result = new LinkedHashMap<String, RecordComponentElement>();

    for (final var element : elements) {
      final var variableName = TypeUtils.getSimpleName(element);
      result.put(variableName, element);
    }

    return result;
  }

  public static Pair<Collection<MethodInfo>, Collection<MethodInfo>> calculateMethodInfo(
      final TypeElement rootElement) {
    final var unimplementedMethods = new LinkedHashSet<MethodInfo>();
    final var implementedMethods = new LinkedHashSet<MethodInfo>();
    var root = rootElement;

    while (root != null) {
      final var methods = ElementFilter.methodsIn(root.getEnclosedElements());
      for (final var method : methods) {
        if (TypeUtils.hasAnyModifier(method, Modifier.ABSTRACT)) {
          unimplementedMethods.add(MethodInfo.create(method));
        } else {
          implementedMethods.add(MethodInfo.create(method));
        }
      }

      final var interfaces = getInterfaces(rootElement);
      for (final var element : interfaces) {
        final var interfaceMethods = ElementFilter.methodsIn(element.getEnclosedElements());

        for (final var method : interfaceMethods) {
          unimplementedMethods.add(MethodInfo.create(method));
        }
      }

      root = TypeUtils.getSuperclass(root);
    }

    unimplementedMethods.removeAll(implementedMethods);

    return ImmutablePair.of(implementedMethods, unimplementedMethods);
  }

  public static Collection<MethodInfo> findImplementedMethods(final TypeElement rootElement) {
    final var methodInfo = calculateMethodInfo(rootElement);
    return methodInfo.getLeft();
  }

  public static List<TypeElement> getInterfaces(final TypeElement root) {
    final List<? extends TypeMirror> interfaces = root.getInterfaces();
    final var result = new ArrayList<TypeElement>();

    for (final var interfaceMirror : interfaces) {
      result.add(ProcessorUtils.getWrappedType(interfaceMirror));
    }
    return result;
  }
}
