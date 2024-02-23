package symphony.apt;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import symphony.apt.context.ProcessorContext;
import symphony.apt.context.ProcessorContextFactory;
import symphony.apt.context.ProcessorContextHolder;
import symphony.apt.context.ProcessorSourceContext;

public class SymphonyQLProcessor extends AbstractProcessor {

  private ProcessorContext processorContext;

  @Override
  public final synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    processorContext = ProcessorContextFactory.create(processingEnv);
  }

  @Override
  public final boolean process(
      final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final boolean needToProcess = !(roundEnv.processingOver() || annotations.isEmpty());

    if (needToProcess) {
      ProcessorContextHolder.withContext(
          processorContext,
          () -> {
            final Collection<ProcessorSourceContext> sourceContexts =
                ProcessorSourceContext.calculate(roundEnv, annotations);

            processorContext.setSourceContexts(sourceContexts);

            process();
          });
    }
    return false;
  }

  @Override
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public final Set<String> getSupportedAnnotationTypes() {
    return SourceCodeGeneratorRegistry.getSupportedAnnotations();
  }

  @Override
  public final Set<String> getSupportedOptions() {
    return ProcessorContextFactory.getSupportedOptions();
  }

  private void process() {
    final Collection<ProcessorSourceContext> sourceContexts = processorContext.getSourceContexts();

    for (final ProcessorSourceContext sourceContext : sourceContexts) {
      final List<TypeElement> elements = sourceContext.getElements();
      final String annotationName = sourceContext.getAnnotationClassName();

      if (!SourceCodeGenerator.generate(annotationName, elements)) {
        break;
      }
    }
  }
}
