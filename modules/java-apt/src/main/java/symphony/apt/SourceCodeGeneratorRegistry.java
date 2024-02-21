package symphony.apt;


import symphony.apt.generator.CodeGenerator;
import symphony.apt.util.ServiceLoaderUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public final class SourceCodeGeneratorRegistry {

    private static final Map<String, ? extends CodeGenerator> CODE_GENERATORS =
            createCodeWriterMap();


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
        final Map<String, CodeGenerator> map = new LinkedHashMap<>();
        final Collection<CodeGenerator> generators = ServiceLoaderUtils.load(CodeGenerator.class);
        
        for (final CodeGenerator generator : generators) {
            final Class<? extends Annotation> annotation = generator.getAnnotation();
            final String annotationName = annotation.getName();
            map.put(annotationName, generator);
        }

        return map;
    }

}
