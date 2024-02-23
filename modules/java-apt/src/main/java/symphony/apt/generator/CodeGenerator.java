package symphony.apt.generator;

import java.lang.annotation.Annotation;

public interface CodeGenerator {

  Class<? extends Annotation> getAnnotation();

  void generate(CodeGeneratorContext context) throws Exception;

  default void onStart() throws Exception {}

  default void onFinish() throws Exception {}
}
