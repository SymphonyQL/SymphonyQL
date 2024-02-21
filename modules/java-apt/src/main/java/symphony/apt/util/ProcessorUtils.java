package symphony.apt.util;

import symphony.apt.context.ProcessorContextHolder;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;


public final class ProcessorUtils {

    private ProcessorUtils() {
        throw new UnsupportedOperationException();
    }


    public static JavaFileObject createSourceFile(
        final TypeElement baseElement, final String packageName, final String className
    ) throws Exception {
        final ProcessingEnvironment env = ProcessorContextHolder.getProcessingEnvironment();
        final Filer filer = env.getFiler();

        return filer.createSourceFile(packageName + SourceTextUtils.PACKAGE_SEPARATOR + className, baseElement);
    }

    public static String packageName(final TypeElement element) {
        final ProcessingEnvironment env = ProcessorContextHolder.getProcessingEnvironment();
        final Elements elementUtils = env.getElementUtils();
        final PackageElement packageElement = elementUtils.getPackageOf(element);
        return packageElement.getQualifiedName().toString();
    }

    public static TypeElement getWrappedType(final TypeMirror mirror) {
        final ProcessingEnvironment env = ProcessorContextHolder.getProcessingEnvironment();
        final Types typeUtils = env.getTypeUtils();

        final TypeKind kind = mirror.getKind();
        final boolean primitive = kind.isPrimitive();

        if (primitive) {
            return typeUtils.boxedClass((PrimitiveType) mirror);
        }
        return (TypeElement) typeUtils.asElement(mirror);
    }

}
