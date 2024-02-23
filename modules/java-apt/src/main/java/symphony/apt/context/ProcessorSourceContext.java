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
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        final var info = getElementInfo();

        for (var pair : info) {
            var element = pair.getLeft();
            elements.add(element);
        }
        return elements;
    }

    public static TypeElement guessOriginElement(final Collection<ProcessorSourceContext> contexts, final String className) {
        for (var context : contexts) {
            var elementsInfo = context.getElementInfo();
            for (var elementInfo : elementsInfo) {
                var classNameInfo = elementInfo.getRight();
                if (StringUtils.equals(classNameInfo, className)) {
                    return elementInfo.getLeft();
                }
            }
        }
        return null;
    }

    public static Collection<ProcessorSourceContext> calculate(final RoundEnvironment roundEnv, final Collection<? extends TypeElement> annotations) {
        var contexts = new ArrayList<ProcessorSourceContext>();

        try {
            for (var annotation : annotations) {
                contexts.add(calculate(roundEnv, annotation));
            }
        } catch (final Exception ex) {
            MessageUtils.error(ExceptionUtils.getMessage(ex));
        }

        return contexts;
    }


    @SuppressWarnings("unchecked")
    private static ProcessorSourceContext calculate(final RoundEnvironment roundEnv, final TypeElement annotation) throws Exception {
        var annotationName = annotation.getQualifiedName().toString();
        final Class<? extends Annotation> annotationClass = ReflectionUtils.getClass(annotationName);

        var allElements = roundEnv.getElementsAnnotatedWith(annotation);
        var filteredElement = TypeUtils.filterWithAnnotation(allElements, annotationClass);

        var typeElements = TypeUtils.foldToTypeElements(filteredElement);

        var generator = SourceCodeGeneratorRegistry.find(annotationName);
        var classes = new ArrayList<Pair<TypeElement, String>>();

        for (var element : typeElements) {
            var className = getClassName(generator, element);
            classes.add(Pair.of(element, className));
        }

        return new ProcessorSourceContext(annotationName, classes);
    }

    private static String getClassName(final CodeGenerator generator, final TypeElement element) {
        if (generator instanceof GeneratedCodeGenerator gcGenerator) {
            var nameModifier = gcGenerator.getNameModifier();
            var originClassName = TypeUtils.getName(element);
            return nameModifier.apply(originClassName);
        }
        return null;
    }

}
