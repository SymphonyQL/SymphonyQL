package symphony.schema.javadsl.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to make a sealed trait an interface instead of a union type or an enum.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface GQLInterface {
    /**
     * @return Optionally provide a list of field names that should be excluded from the interface.
     */
    String[] excludedFields() default {};
}
