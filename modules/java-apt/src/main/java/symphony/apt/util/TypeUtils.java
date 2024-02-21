package symphony.apt.util;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.apache.commons.lang3.StringUtils;
import symphony.apt.AnnotatedElementCallback;
import symphony.apt.context.ProcessorContext;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.context.ProcessorSourceContext;
import symphony.apt.model.TypeInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TypeUtils {

    private TypeUtils() {
        throw new UnsupportedOperationException();
    }


    public static Collection<TypeElement> foldToTypeElements(final Collection<? extends Element> allElements) {
        final Collection<TypeElement> elements = new HashSet<>();

        for (final Element element : allElements) {
            if (element instanceof TypeElement) {
                elements.add((TypeElement) element);
            } else if (element instanceof VariableElement || element instanceof ExecutableElement) {
                final Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement instanceof TypeElement) {
                    elements.add((TypeElement) enclosingElement);
                } else if (enclosingElement instanceof ExecutableElement) {
                    elements.add((TypeElement) enclosingElement.getEnclosingElement());
                }
            } else {
                throw new UnsupportedOperationException("Unknown type of element");
            }
        }

        return elements;
    }

    public static <T extends Annotation> T getAnnotation(
            final Class<T> annotationClass, final Element... elements
    ) {
        for (final Element element : elements) {
            final T annotation = element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    public static boolean hasAnyType(final TypeMirror typeMirror, Class<?>... types) {
        final String typeName = typeMirror.toString();
        for (final Class<?> type : types) {
            if (StringUtils.equals(type.getCanonicalName(), typeName)) {
                return true;
            }
        }
        return false;
    }

    public static <T extends Element> Collection<T> filterWithoutAnnotation(
            final Collection<T> elements, final Class<? extends Annotation> annotationClass
    ) {
        final Collection<T> result = new ArrayList<>();
        for (final T element : elements) {
            final Annotation annotation = element.getAnnotation(annotationClass);
            if (annotation == null) {
                result.add(element);
            }
        }
        return result;
    }

    public static <T extends Annotation> void filterWithAnnotation(
            final TypeElement typeElement, final Collection<? extends Element> elements,
            final Class<T> annotationClass, final AnnotatedElementCallback<T> callback
    ) throws Exception {
        for (final Element element : elements) {
            final T annotation = getAnnotation(annotationClass, element, typeElement);

            if (annotation != null) {
                callback.process(element, annotation);
            }
        }
    }

    public static <T extends Annotation> List<? extends Element> filterWithAnnotation(
            final Collection<? extends Element> elements, final Class<T> annotationClass
    ) {
        final List<Element> result = new ArrayList<>();

        for (final Element element : elements) {
            final T annotation = element.getAnnotation(annotationClass);
            if (annotation != null) {
                result.add(element);
            }
        }

        return result;
    }

    public static TypeElement getSuperclass(final TypeElement root) {
        final String objectClassName = Object.class.getCanonicalName();
        final TypeMirror superclass = root.getSuperclass();
        final TypeElement superclassElement = ProcessorUtils.getWrappedType(superclass);

        if (superclassElement != null) {
            final String rootClassName = String.valueOf(superclassElement.getQualifiedName());
            return rootClassName.equals(objectClassName) ? null : superclassElement;
        }
        return null;
    }

    public static List<TypeMirror> asTypes(final Collection<? extends Element> elements) {
        final List<TypeMirror> types = new ArrayList<>();
        for (final Element element : elements) {
            types.add(element.asType());
        }
        return types;
    }

    public static String getName(final Element element) {
        return element.getSimpleName().toString();
    }

    public static TypeName getTypeName(final Element element) {
        return getTypeName(element, false);
    }

    public static TypeName getTypeName(final Element element, final boolean wrap) {
        final TypeMirror typeMirror = getTypeMirror(element);
        final TypeKind kind = typeMirror.getKind();

        if (kind == TypeKind.ERROR) {
            final ProcessorContext context = ProcessorContextHolder.getContext();
            final Collection<ProcessorSourceContext> sourceContexts = context.getSourceContexts();

            final String typeName = String.valueOf(typeMirror);
            final TypeElement originElement = ProcessorSourceContext.guessOriginElement(sourceContexts, typeName);

            if (originElement != null) {
                final String packageName = ProcessorUtils.packageName(originElement);
                return ClassName.get(packageName, typeName);
            }
            return ClassName.bestGuess(typeName);
        }

        return TypeName.get(wrap ? ProcessorUtils.getWrappedType(typeMirror).asType() : typeMirror);
    }

    public static TypeMirror getTypeMirror(final Element element) {
        return element instanceof ExecutableElement
                ? ((ExecutableElement) element).getReturnType() : element.asType();
    }

    public static TypeName getArrayTypeName(final Element parameter) {
        return ArrayTypeName.of(TypeName.get(parameter.asType()));
    }

    public static boolean hasAnyModifier(final Element element, Modifier... modifiers) {
        final Set<Modifier> elementModifiers = element.getModifiers();
        return hasAnyModifier(elementModifiers, modifiers);
    }

    public static boolean hasAnyModifier(
            final Collection<Modifier> element, final Modifier... modifiers
    ) {
        for (final Modifier modifier : modifiers) {
            if (element.contains(modifier)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWrappedType(TypeName typeName) {
        var wrappedList = List.of(
                "java.util.Optional",
                "java.util.Map",
                "java.util.List",
                "java.util.Vector",
                "java.util.Set",
                "java.util.concurrent.CompletionStage"
        );
        return wrappedList.contains(getRawTypeName(typeName).toString());
    }


    public static boolean isPrimitiveType(TypeName typeName) {
        var scalarList = List.of(
                "java.lang.String",
                "java.math.BigInteger",
                "java.math.BigDecimal"
        );
        return typeName.isPrimitive() || typeName.isBoxedPrimitive() || scalarList.contains(typeName.toString());
    }

    public static TypeName getRawTypeName(final TypeName typeName) {
        return switch (typeName) {
            case ParameterizedTypeName p -> p.rawType;
            default -> typeName;
        };
    }

    public static TypeName getFirstNestedParameterizedTypeName(final TypeName typeName) {
        return switch (typeName) {
            case ParameterizedTypeName p -> {
                if (!p.typeArguments.isEmpty()) {
                    yield getFirstNestedParameterizedTypeName(p.typeArguments.getFirst());
                } else {
                    yield p;
                }
            }
            default -> typeName;
        };
    }

    public static boolean isCustomType(TypeName typeName) {
        var isPrimitiveType = isPrimitiveType(typeName);
        var isWrappedType = isWrappedType(typeName);
        return !isPrimitiveType && !isWrappedType;
    }


    public static TypeInfo getTypeInfo(TypeName typeName, int depth) {
        TypeInfo typeDesc;
        if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
            List<TypeInfo> parameterizedTypes = new ArrayList<>();
            for (TypeName typeArgument : parameterizedTypeName.typeArguments) {
                parameterizedTypes.add(getTypeInfo(typeArgument, depth + 1));
            }
            typeDesc = new TypeInfo(parameterizedTypeName.rawType.toString(), depth);
            typeDesc.setParameterizedTypes(parameterizedTypes);
        } else if (typeName instanceof ClassName className) {
            typeDesc = new TypeInfo(className.packageName() + "." + className.simpleName(), depth);
        } else {
            typeDesc = new TypeInfo(typeName.toString(), depth);
        }

        return typeDesc;
    }

    public static List<String> primitiveTypes = List.of(
            "java.lang.Boolean",
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Short",
            "java.math.BigInteger",
            "java.math.BigDecimal",
            "java.lang.Void",
            "boolean",
            "int",
            "long",
            "float",
            "double",
            "short",
            "void"
    );

    public static String getWrappedCallString(TypeInfo info) {
        StringBuilder sb = new StringBuilder();
        if (primitiveTypes.contains(info.getName())) {
            sb.append("$T.getSchema($S)");
        } else {
            switch (info.getName()) {
                case "java.util.List":
                    sb.append("$T.createList(");
                    break;
                case "java.util.Set":
                    sb.append("$T.createSet(");
                    break;
                case "java.util.Vector":
                    sb.append("$T.createVector(");
                    break;
                case "java.util.Optional":
                    sb.append("$T.createOptional(");
                    break;
                case "org.apache.pekko.stream.javadsl.Source":
                    sb.append("$T.createSource(");
                    break;
                case "java.util.concurrent.CompletionStage":
                    sb.append("$T.createCompletionStage(");
                    break;
                default:
                    return "$T.$N()";
            }
        }
        if (info.getParameterizedTypes() != null && !info.getParameterizedTypes().isEmpty()) {
            for (int i = 0; i < info.getParameterizedTypes().size(); i++) {
                TypeInfo argInfo = info.getParameterizedTypes().get(i);
                sb.append(getWrappedCallString(argInfo));
                if (i < info.getParameterizedTypes().size() - 1) {
                    sb.append(", ");
                }
            }
        }

        if (!primitiveTypes.contains(info.getName())) {
            sb.append(")");
        }
        return sb.toString();
    }
}
