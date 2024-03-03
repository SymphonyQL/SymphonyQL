package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * &#64;ArgExtractor creates a class with extractor method for SymphonyQL Enum Type. Support Enum
 * and Record classes.
 *
 * <p><br>
 * Original enum class OriginEnumExtractor:
 *
 * <pre>{@code
 * @EnumSchema
 * enum OriginEnum {
 *     EARTH, MARS, BELT;
 * }
 * }</pre>
 *
 * <p>Generated enum class OriginEnumExtractor:
 *
 * <pre>{@code
 * import java.util.Arrays;
 * import java.util.function.Function;
 * import javax.annotation.Generated;
 * import scala.util.Either;
 * import scala.util.Right;
 * import symphony.parser.SymphonyQLError;
 * import symphony.parser.SymphonyQLInputValue;
 * import symphony.parser.SymphonyQLValue;
 * import symphony.schema.ArgumentExtractor;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class OriginEnumExtractor {
 *     public static final ArgumentExtractor<OriginEnum> extractor = extractor();
 *
 *     private static final Function<SymphonyQLValue, OriginEnum> function = new Function<SymphonyQLValue, OriginEnum>() {
 *         @Override
 *         public OriginEnum apply(SymphonyQLValue obj) {
 *             if (obj instanceof SymphonyQLValue.EnumValue value) {
 *                 var originenumOptional = Arrays.stream(OriginEnum.values()).filter(o -> o.name().equals(value.value())).findFirst();
 *                 if (originenumOptional.isEmpty()) {
 *                     throw new RuntimeException("Cannot build enum OriginEnum from input");
 *                 }
 *                 return originenumOptional.get();
 *             }
 *
 *             var originenumStringValue = (SymphonyQLValue.StringValue) obj;
 *             var originenumOptional = Arrays.stream(OriginEnum.values()).filter(o -> o.name().equals(originenumStringValue.value())).findFirst();
 *             if (originenumOptional.isEmpty()) {
 *                 throw new RuntimeException("Cannot build enum OriginEnum from input");
 *             }
 *             return originenumOptional.get();
 *         }
 *     };
 *
 *     private OriginEnumExtractor() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static ArgumentExtractor<OriginEnum> extractor() {
 *         return new ArgumentExtractor<OriginEnum>() {
 *             @Override
 *             public Either<SymphonyQLError.ArgumentError, OriginEnum> extract(
 *                     SymphonyQLInputValue input) {
 *                 return switch (input) {
 *                     case SymphonyQLValue.EnumValue obj ->  {
 *                         yield Right.apply(function.apply(obj));
 *                     }
 *                     case SymphonyQLValue.StringValue obj -> {
 *                         yield Right.apply(function.apply(obj));
 *                     }
 *                     default -> throw new RuntimeException("Expected EnumValue or StringValue");
 *                 };
 *             }
 *         };
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface ArgExtractor {}
