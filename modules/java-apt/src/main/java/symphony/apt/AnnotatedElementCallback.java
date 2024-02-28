package symphony.apt;

import java.lang.annotation.Annotation;
import javax.lang.model.element.Element;

public interface AnnotatedElementCallback<T extends Annotation> {

  void process(Element element, T annotation) throws Exception;
}
