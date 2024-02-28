package symphony.apt;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import symphony.apt.generator.CodeGenerator;
import symphony.apt.util.ServiceLoaderUtils;

public final class SourceCodeGeneratorRegistry {

  private static final Map<String, ? extends CodeGenerator> CODE_GENERATORS = createCodeWriterMap();

  private SourceCodeGeneratorRegistry() {
    throw new UnsupportedOperationException();
  }

  public static Set<String> getSupportedAnnotations() {
    return CODE_GENERATORS.keySet();
  }

  public static CodeGenerator find(final String annotationClassName) {
    return CODE_GENERATORS.get(annotationClassName);
  }

  private static Map<String, ? extends CodeGenerator> createCodeWriterMap() {
    var map = new LinkedHashMap<String, CodeGenerator>();
    var generators = ServiceLoaderUtils.load(CodeGenerator.class);

    for (var generator : generators) {
      final Class<? extends Annotation> annotation = generator.getAnnotation();
      var annotationName = annotation.getName();
      map.put(annotationName, generator);
    }

    return map;
  }
}
