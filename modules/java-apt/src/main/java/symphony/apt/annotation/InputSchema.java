package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * &#64;InputSchema creates a class with schema method for SymphonyQL Input Type. Support Enum and
 * Record classes.
 *
 * <p><br>
 * Original class InputObject:
 *
 * <pre>{@code
 * @InputSchema
 * record InputObject(
 *         Optional<OriginEnum> optionalEnum,
 *         Optional<String> optionalString,
 * ) {
 * }
 * }</pre>
 *
 * <p>Generated class InputObjectSchema:
 *
 * <pre>{@code
 * import java.util.function.Function;
 * import javax.annotation.Generated;
 * import symphony.parser.adt.introspection.__Field;
 * import symphony.schema.Schema;
 * import symphony.schema.javadsl.FieldBuilder;
 * import symphony.schema.javadsl.InputObjectBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class InputObjectSchema {
 *     private InputObjectSchema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     public static Schema<InputObject> schema() {
 *         InputObjectBuilder<InputObject> newObject = InputObjectBuilder.newObject();
 *         newObject.name("InputObject");
 *         newObject.field(new Function<FieldBuilder, __Field>() {
 *              @Override
 *              public __Field apply(FieldBuilder builder) {
 *                  return builder.name("optionalEnum").schema(Schema.createOptional(symphony.example.schema.OriginEnumSchema.schema())).build();
 *              }
 *          });
 *         newObject.field(new Function<FieldBuilder, __Field>() {
 *              @Override
 *              public __Field apply(FieldBuilder builder) {
 *                  return builder.name("optionalString").schema(Schema.createOptional(Schema.getSchema("java.lang.String"))).build();
 *              }
 *          });
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface InputSchema {}
