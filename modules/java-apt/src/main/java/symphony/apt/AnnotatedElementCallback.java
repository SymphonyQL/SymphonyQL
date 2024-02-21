package symphony.apt;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;

public interface AnnotatedElementCallback<T extends Annotation> {

    void process(Element element, T annotation) throws Exception;

}
