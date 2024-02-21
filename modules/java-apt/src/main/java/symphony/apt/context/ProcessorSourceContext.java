package symphony.apt.context;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import symphony.apt.SourceCodeGeneratorRegistry;
import symphony.apt.generator.CodeGenerator;
import symphony.apt.generator.GeneratedCodeGenerator;
import symphony.apt.util.MessageUtils;
import symphony.apt.util.ReflectionUtils;
import symphony.apt.util.TypeUtils;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ProcessorSourceContext {

    private final String annotationClassName;
    private final List<Pair<TypeElement, String>> elementInfo;


    public ProcessorSourceContext(final String annotationClassName, final List<Pair<TypeElement, String>> elementsInfo) {
        this.annotationClassName = annotationClassName;
        this.elementInfo = elementsInfo;
    }


    public final String getAnnotationClassName() {
        return annotationClassName;
    }

    public final List<Pair<TypeElement, String>> getElementInfo() {
        return elementInfo;
    }

    public final List<TypeElement> getElements() {
        final List<TypeElement> elements = new ArrayList<>();
        final List<Pair<TypeElement, String>> info = getElementInfo();

        for (final Pair<TypeElement, String> pair : info) {
            final TypeElement element = pair.getLeft();
            elements.add(element);
        }
        return elements;
    }

    public static TypeElement guessOriginElement(final Collection<ProcessorSourceContext> contexts, final String className) {
        for (final ProcessorSourceContext context : contexts) {
            final List<Pair<TypeElement, String>> elementsInfo = context.getElementInfo();
            for (final Pair<TypeElement, String> elementInfo : elementsInfo) {
                final String classNameInfo = elementInfo.getRight();
                if (StringUtils.equals(classNameInfo, className)) {
                    return elementInfo.getLeft();
                }
            }
        }
        return null;
    }

    public static Collection<ProcessorSourceContext> calculate(final RoundEnvironment roundEnv, final Collection<? extends TypeElement> annotations) {
        final List<ProcessorSourceContext> contexts = new ArrayList<>();

        try {
            for (final TypeElement annotation : annotations) {
                contexts.add(calculate(roundEnv, annotation));
            }
        } catch (final Exception ex) {
            MessageUtils.error(ExceptionUtils.getMessage(ex));
        }

        return contexts;
    }


    @SuppressWarnings("unchecked")
    private static ProcessorSourceContext calculate(final RoundEnvironment roundEnv, final TypeElement annotation) throws Exception {
        final String annotationName = annotation.getQualifiedName().toString();
        final Class<? extends Annotation> annotationClass = ReflectionUtils.getClass(annotationName);

        final Set<? extends Element> allElements = roundEnv.getElementsAnnotatedWith(annotation);
        final Collection<? extends Element> filteredElement = TypeUtils.filterWithAnnotation(allElements, annotationClass);

        final Collection<TypeElement> typeElements = TypeUtils.foldToTypeElements(filteredElement);

        final CodeGenerator generator = SourceCodeGeneratorRegistry.find(annotationName);
        final List<Pair<TypeElement, String>> classes = new ArrayList<>();

        for (final TypeElement element : typeElements) {
            final String className = getClassName(generator, element);
            classes.add(Pair.of(element, className));
        }

        return new ProcessorSourceContext(annotationName, classes);
    }

    private static String getClassName(final CodeGenerator generator, final TypeElement element) {
        if (generator instanceof GeneratedCodeGenerator gcGenerator) {
            final Function<String, String> nameModifier = gcGenerator.getNameModifier();
            final String originClassName = TypeUtils.getName(element);
            return nameModifier.apply(originClassName);
        }
        return null;
    }

}
