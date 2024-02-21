package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>@EnumSchema creates a class with schema method for SymphonyQL Enum Type.
 * <p>
 * <br/> Original class OriginEnum:
 * <pre>{@code
 * @EnumSchema
 * enum OriginEnum {
 *     EARTH, MARS, BELT;
 * }
 * }</pre>
 * <p>
 * Generated class OriginEnumSchema:
 * <pre>{@code
 * import java.util.function.Function;
 * import javax.annotation.Generated;
 * import symphony.parser.adt.introspection.__EnumValue;
 * import symphony.schema.Schema;
 * import symphony.schema.javadsl.EnumBuilder;
 * import symphony.schema.javadsl.EnumValueBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class OriginEnumSchema {
 *     private OriginEnumSchema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     public static Schema<OriginEnum> schema() {
 *         EnumBuilder<OriginEnum> newEnum = EnumBuilder.newEnum();
 *         newEnum.name("OriginEnum");
 *         newEnum.serialize(new Function<OriginEnum, String>() {
 *             @Override
 *             public String apply(OriginEnum en) {
 *                 return en.name();
 *             }
 *         });
 *         newEnum.value(new Function<EnumValueBuilder, __EnumValue>() {
 *             @Override
 *             public __EnumValue apply(EnumValueBuilder builder) {
 *                 return builder.name("EARTH").build();
 *             }
 *         });
 *         newEnum.value(new Function<EnumValueBuilder, __EnumValue>() {
 *             @Override
 *             public __EnumValue apply(EnumValueBuilder builder) {
 *                 return builder.name("MARS").build();
 *             }
 *         });
 *         newEnum.value(new Function<EnumValueBuilder, __EnumValue>() {
 *             @Override
 *             public __EnumValue apply(EnumValueBuilder builder) {
 *                 return builder.name("BELT").build();
 *             }
 *         });
 *         return newEnum.build();
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface EnumSchema {

}
