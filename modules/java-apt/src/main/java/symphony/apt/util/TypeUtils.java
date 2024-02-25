package symphony.apt.util;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.apache.commons.lang3.StringUtils;
import symphony.apt.AnnotatedElementCallback;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.context.ProcessorSourceContext;
import symphony.apt.model.TypeCategory;
import symphony.apt.model.WrappedContext;

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

import static symphony.apt.generator.GeneratedCodeGenerator.EXTRACTOR_METHOD_NAME;
import static symphony.apt.generator.GeneratedCodeGenerator.SCHEMA_METHOD_NAME;

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
        for (final var element : elements) {
            final var annotation = element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    public static boolean hasAnyType(final TypeMirror typeMirror, Class<?>... types) {
        final var typeName = typeMirror.toString();
        for (final var type : types) {
            if (StringUtils.equals(type.getCanonicalName(), typeName)) {
                return true;
            }
        }
        return false;
    }

    public static <T extends Element> Collection<T> filterWithoutAnnotation(
            final Collection<T> elements, final Class<? extends Annotation> annotationClass
    ) {
        final var result = new ArrayList<T>();
        for (final var element : elements) {
            final var annotation = element.getAnnotation(annotationClass);
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
        for (final var element : elements) {
            final var annotation = getAnnotation(annotationClass, element, typeElement);

            if (annotation != null) {
                callback.process(element, annotation);
            }
        }
    }

    public static <T extends Annotation> List<? extends Element> filterWithAnnotation(
            final Collection<? extends Element> elements, final Class<T> annotationClass
    ) {
        final var result = new ArrayList<Element>();

        for (final var element : elements) {
            final var annotation = element.getAnnotation(annotationClass);
            if (annotation != null) {
                result.add(element);
            }
        }

        return result;
    }

    public static TypeElement getSuperclass(final TypeElement root) {
        final var objectClassName = Object.class.getCanonicalName();
        final var superclass = root.getSuperclass();
        final var superclassElement = ProcessorUtils.getWrappedType(superclass);

        if (superclassElement != null) {
            final var rootClassName = String.valueOf(superclassElement.getQualifiedName());
            return rootClassName.equals(objectClassName) ? null : superclassElement;
        }
        return null;
    }

    public static List<TypeMirror> asTypes(final Collection<? extends Element> elements) {
        final var types = new ArrayList<TypeMirror>();
        for (final var element : elements) {
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
        final var typeMirror = getTypeMirror(element);
        final var kind = typeMirror.getKind();

        if (kind == TypeKind.ERROR) {
            final var context = ProcessorContextHolder.getContext();
            final var sourceContexts = context.getSourceContexts();

            final var typeName = String.valueOf(typeMirror);
            final var originElement = ProcessorSourceContext.guessOriginElement(sourceContexts, typeName);

            if (originElement != null) {
                final var packageName = ProcessorUtils.packageName(originElement);
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
        final var elementModifiers = element.getModifiers();
        return hasAnyModifier(elementModifiers, modifiers);
    }

    public static boolean hasAnyModifier(
            final Collection<Modifier> element, final Modifier... modifiers
    ) {
        for (final var modifier : modifiers) {
            if (element.contains(modifier)) {
                return true;
            }
        }
        return false;
    }

    private final static List<String> twoWrappedList = List.of(
            "java.util.Map"
    );

    private final static List<String> oneWrappedList = List.of(
            "java.util.Optional",
            "java.util.List",
            "java.util.Vector",
            "java.util.Set",
            "java.util.concurrent.CompletionStage"
    );

    private final static List<String> scalarList = List.of(
            "java.lang.String",
            "java.math.BigInteger",
            "java.math.BigDecimal"
    );

    private final static List<String> primitiveTypes = List.of(
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

    public static boolean existsTwoParameterizedTypes(TypeName typeName) {
        return twoWrappedList.contains(getRawTypeName(typeName).toString());
    }

    public static boolean existsOneParameterizedType(TypeName typeName) {
        return oneWrappedList.contains(getRawTypeName(typeName).toString());
    }

    public static TypeCategory getTypeCategory(TypeName typeName) {
        if (isPrimitiveType(typeName)) {
            return TypeCategory.SYSTEM_TYPE;
        }
        if (existsOneParameterizedType(typeName)) {
            return TypeCategory.ONE_PARAMETERIZED_TYPE;
        }
        if (existsTwoParameterizedTypes(typeName)) {
            return TypeCategory.TWO_PARAMETERIZED_TYPES;
        }
        if (isCustomObjectType(typeName)) {
            return TypeCategory.CUSTOM_OBJECT_TYPE;
        }

        return TypeCategory.CUSTOM_OBJECT_TYPE;

    }

    public static boolean isPrimitiveType(TypeName typeName) {
        return typeName.isPrimitive() || typeName.isBoxedPrimitive() || scalarList.contains(typeName.toString());
    }

    public static TypeName getRawTypeName(final TypeName typeName) {
        return switch (typeName) {
            case ParameterizedTypeName p -> p.rawType;
            default -> typeName;
        };
    }

    public static List<TypeName> getParameterizedTypes(TypeName typeName) {
        var result = new ArrayList<TypeName>();
        if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
            if (parameterizedTypeName.typeArguments.size() == 2) {
                for (var paramType : parameterizedTypeName.typeArguments) {
                    extractInDecomposableTypes(paramType, result);
                }
            } else {
                for (var innerType : parameterizedTypeName.typeArguments) {
                    result.addAll(getParameterizedTypes(innerType));
                }
            }
        } else {
            result.add(typeName);
        }
        return result;
    }

    private static void extractInDecomposableTypes(TypeName type, List<TypeName> inDecomposableTypes) {
        if (type instanceof ParameterizedTypeName parameterizedTypeName) {
            if (!parameterizedTypeName.typeArguments.isEmpty()) {
                for (TypeName innerType : parameterizedTypeName.typeArguments) {
                    extractInDecomposableTypes(innerType, inDecomposableTypes);
                }
            } else {
                inDecomposableTypes.add(type);
            }
        } else {
            inDecomposableTypes.add(type);
        }
    }

    public static boolean isCustomObjectType(TypeName typeName) {
        var isPrimitiveType = isPrimitiveType(typeName);
        var isWrappedType = existsOneParameterizedType(typeName) || existsTwoParameterizedTypes(typeName);
        return !isPrimitiveType && !isWrappedType;
    }

    public static String buildSchemaWrappedString(WrappedContext wrappedContext, List<Object> args) {
        final var sb = new StringBuilder();
        var info = wrappedContext.fieldTypeName;
        var rawTypeName = getRawTypeName(info).toString();
        if (primitiveTypes.contains(rawTypeName)) {
            args.add(wrappedContext.className);
            args.add(wrappedContext.fieldTypeName.toString());
            sb.append("$T.getSchema($S)");
        } else {
            switch (rawTypeName) {
                case "java.util.Map":
                    args.add(wrappedContext.className);
                    sb.append("$T.createMap(");
                    break;
                case "java.util.List":
                    args.add(wrappedContext.className);
                    sb.append("$T.createList(");
                    break;
                case "java.util.Set":
                    args.add(wrappedContext.className);
                    sb.append("$T.createSet(");
                    break;
                case "java.util.Vector":
                    args.add(wrappedContext.className);
                    sb.append("$T.createVector(");
                    break;
                case "java.util.Optional":
                    args.add(wrappedContext.className);
                    sb.append("$T.createOptional(");
                    break;
                case "org.apache.pekko.stream.javadsl.Source":
                    args.add(wrappedContext.className);
                    sb.append("$T.createSource(");
                    break;
                case "java.util.concurrent.CompletionStage":
                    args.add(wrappedContext.className);
                    sb.append("$T.createCompletionStage(");
                    break;
                default:
                    MessageUtils.note("Custom Schema -> " + info);
                    args.add(ClassName.get("", wrappedContext.suffix.apply(info.toString())));
                    args.add(SCHEMA_METHOD_NAME);
                    return "$T.$N";
            }
        }
        if (info instanceof ParameterizedTypeName parameterizedTypeName && !parameterizedTypeName.typeArguments.isEmpty()) {
            for (int i = 0; i < parameterizedTypeName.typeArguments.size(); i++) {
                wrappedContext.fieldTypeName = parameterizedTypeName.typeArguments.get(i);
                sb.append(buildSchemaWrappedString(wrappedContext, args));
                if (i < parameterizedTypeName.typeArguments.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        if (!primitiveTypes.contains(rawTypeName)) {
            sb.append(")");
        }

        return sb.toString();
    }

    public static String buildExtractorWrappedString(WrappedContext wrappedContext, List<Object> args) {
        final var sb = new StringBuilder();
        var info = wrappedContext.fieldTypeName;
        var rawTypeName = getRawTypeName(info).toString();
        if (primitiveTypes.contains(rawTypeName)) {
            args.add(ParameterizedTypeName.get(wrappedContext.className, wrappedContext.fieldTypeName));
            args.add(wrappedContext.className);
            args.add(wrappedContext.fieldTypeName.toString());
            sb.append("($T)$T.getArgumentExtractor($S)");
        } else {
            switch (rawTypeName) {
                case "java.util.List":
                    args.add(wrappedContext.className);
                    sb.append("$T.createList(");
                    break;
                case "java.util.Set":
                    args.add(wrappedContext.className);
                    sb.append("$T.createSet(");
                    break;
                case "java.util.Vector":
                    args.add(wrappedContext.className);
                    sb.append("$T.createVector(");
                    break;
                case "java.util.Optional":
                    args.add(wrappedContext.className);
                    sb.append("$T.createOptional(");
                    break;
                default:
                    MessageUtils.note("Custom Extractor -> " + info);
                    args.add(ClassName.get("", wrappedContext.suffix.apply(info.toString())));
                    args.add(EXTRACTOR_METHOD_NAME);
                    return "$T.$N";
            }
        }
        if (info instanceof ParameterizedTypeName parameterizedTypeName && !parameterizedTypeName.typeArguments.isEmpty()) {
            for (int i = 0; i < parameterizedTypeName.typeArguments.size(); i++) {
                wrappedContext.fieldTypeName = parameterizedTypeName.typeArguments.get(i);
                sb.append(buildExtractorWrappedString(wrappedContext, args));
                if (i < parameterizedTypeName.typeArguments.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        if (!primitiveTypes.contains(rawTypeName)) {
            sb.append(")");
        }

        return sb.toString();
    }
}
