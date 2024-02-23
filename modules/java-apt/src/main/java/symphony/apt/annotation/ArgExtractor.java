package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>@ArgExtractor creates a class with extractor method for SymphonyQL Enum Type.
 * <p>
 * <br/> Original enum class OriginEnumExtractor:
 * <pre>{@code
 * @EnumSchema
 * enum OriginEnum {
 *     EARTH, MARS, BELT;
 * }
 * }</pre>
 * <p>
 * Generated enum class OriginEnumExtractor:
 * <pre>{@code
 * import java.util.function.Function;
 * import javax.annotation.Generated;
 * import scala.util.Either;
 * import scala.util.Left;
 * import scala.util.Right;
 * import symphony.parser.SymphonyQLError;
 * import symphony.parser.SymphonyQLInputValue;
 * import symphony.parser.SymphonyQLValue;
 * import symphony.schema.ArgumentExtractor;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class OriginEnumExtractor {
 *     private static final Function<SymphonyQLValue, OriginEnum> function = new Function<SymphonyQLValue, OriginEnum>() {
 *         @Override
 *         public OriginEnum apply(SymphonyQLValue obj) {
 *             if (obj instanceof SymphonyQLValue.EnumValue value) {
 *                 var originenumEither = ArgumentExtractor.StringArg().extract(value);
 *                 return switch (originenumEither) {
 *                     case Right<SymphonyQLError.ArgumentError, ?> right -> {
 *                         yield (OriginEnum) OriginEnum.valueOf((String)right.value());
 *                     }
 *                     case Left<SymphonyQLError.ArgumentError, ?> left -> {
 *                         throw new RuntimeException("Cannot build enum symphony.example.schema.OriginEnum from input", left.value());
 *                     }
 *                 };
 *             }
 *             var originenumEither = ArgumentExtractor.StringArg().extract(obj);
 *             return switch (originenumEither) {
 *                 case Right<SymphonyQLError.ArgumentError, ?> right -> {
 *                     yield (OriginEnum) OriginEnum.valueOf((String)right.value());
 *                 }
 *                 case Left<SymphonyQLError.ArgumentError, ?> left -> {
 *                     throw new RuntimeException("Cannot build enum symphony.example.schema.OriginEnum from input", left.value());
 *                 }
 *             };
 *         }
 *     };
 *
 *     public static final ArgumentExtractor<OriginEnum> extractor = extractor();
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
 *                     case SymphonyQLValue.StringValue obj ->  {
 *                         yield Right.apply(function.apply(obj));
 *                     }
 *                     default -> Left.apply(new SymphonyQLError.ArgumentError("Expected ObjectValue"));
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
public @interface ArgExtractor {

}
