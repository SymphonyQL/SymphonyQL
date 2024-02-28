package symphony.apt;

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
    final var needToProcess = !(roundEnv.processingOver() || annotations.isEmpty());

    if (needToProcess) {
      ProcessorContextHolder.withContext(
          processorContext,
          () -> {
            var sourceContexts = ProcessorSourceContext.calculate(roundEnv, annotations);

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
    var sourceContexts = processorContext.getSourceContexts();

    for (final var sourceContext : sourceContexts) {
      final var elements = sourceContext.getElements();
      final var annotationName = sourceContext.getAnnotationClassName();

      if (!SourceCodeGenerator.generate(annotationName, elements)) {
        break;
      }
    }
  }
}
