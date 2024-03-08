package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * &#64;InterfaceSchema creates classes with schema method for SymphonyQL Interface Type.
 *
 * <p><br>
 * Original interface NestedInterface, Mid1, Mid2:
 *
 * <pre>{@code
 * import symphony.apt.annotation.InterfaceSchema;
 * import symphony.apt.annotation.ObjectSchema;
 *
 * @InterfaceSchema
 * public sealed interface NestedInterface {
 * }
 *
 *
 * @InterfaceSchema
 * sealed interface Mid1 extends NestedInterface {
 * }
 *
 * @InterfaceSchema
 * sealed interface Mid2 extends NestedInterface {
 * }
 *
 * @ObjectSchema
 * record FooA(String a, String b, String c) implements Mid1 {
 * }
 *
 * @ObjectSchema
 * record FooB(String b, String c, String d) implements Mid1, Mid2 {
 * }
 *
 * @ObjectSchema
 * record FooC(String b, String d, String e) implements Mid2 {
 * }
 * }</pre>
 *
 * <p>Generated class NestedInterfaceSchema:
 *
 * <pre>{@code
 * import java.util.Optional;
 * import javax.annotation.Generated;
 * import symphony.schema.Schema;
 * import symphony.schema.builder.InterfaceBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class NestedInterfaceSchema {
 *     public static final Schema<NestedInterface> schema = schema();
 *
 *     private NestedInterfaceSchema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static Schema<NestedInterface> schema() {
 *         InterfaceBuilder<NestedInterface> newObject = InterfaceBuilder.newObject();
 *         newObject.description(Optional.empty());
 *         newObject.origin(Optional.of("symphony.apt.tests.NestedInterface"));
 *         newObject.name("NestedInterface");
 *         newObject.subSchema("Mid1", Mid1Schema.schema);
 *         newObject.subSchema("Mid2", Mid2Schema.schema);
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 *
 * <p>Generated class Mid1Schema:
 *
 * <pre>{@code
 * import java.util.Optional;
 * import javax.annotation.Generated;
 * import symphony.schema.Schema;
 * import symphony.schema.builder.InterfaceBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class Mid1Schema {
 *     public static final Schema<Mid1> schema = schema();
 *
 *     private Mid1Schema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static Schema<Mid1> schema() {
 *         InterfaceBuilder<Mid1> newObject = InterfaceBuilder.newObject();
 *         newObject.description(Optional.empty());
 *         newObject.origin(Optional.of("symphony.apt.tests.Mid1"));
 *         newObject.name("Mid1");
 *         newObject.subSchema("FooA", FooASchema.schema);
 *         newObject.subSchema("FooB", FooBSchema.schema);
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 *
 * <p>Generated class Mid2Schema:
 *
 * <pre>{@code
 * import java.util.Optional;
 * import javax.annotation.Generated;
 * import symphony.schema.Schema;
 * import symphony.schema.builder.InterfaceBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class Mid2Schema {
 *     public static final Schema<Mid2> schema = schema();
 *
 *     private Mid2Schema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static Schema<Mid2> schema() {
 *         InterfaceBuilder<Mid2> newObject = InterfaceBuilder.newObject();
 *         newObject.description(Optional.empty());
 *         newObject.origin(Optional.of("symphony.apt.tests.Mid2"));
 *         newObject.name("Mid2");
 *         newObject.subSchema("FooB", FooBSchema.schema);
 *         newObject.subSchema("FooC", FooCSchema.schema);
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface InterfaceSchema {}
