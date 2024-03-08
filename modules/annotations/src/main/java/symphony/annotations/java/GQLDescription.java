package symphony.annotations.java;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation used to provide a description to a field or a type. */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface GQLDescription {
  String value();
}
