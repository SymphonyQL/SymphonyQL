package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * &#64;UnionSchema creates classes with schema method for SymphonyQL Union Type.
 *
 * <p><br>
 * Original interface SearchResult:
 *
 * <pre>{@code
 * @UnionSchema
 * public sealed interface SearchResult permits Book, Author {
 * }
 *
 * @ObjectSchema
 * record Book(String title) implements SearchResult {
 * }
 *
 * @ObjectSchema
 * record Author(String name) implements SearchResult {
 * }
 * }</pre>
 *
 * <p>Generated class SearchResultSchema:
 *
 * <pre>{@code
 * import java.util.Optional;
 * import javax.annotation.Generated;
 * import symphony.schema.Schema;
 * import symphony.schema.builder.UnionBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class SearchResultSchema {
 *     public static final Schema<SearchResult> schema = schema();
 *
 *     private SearchResultSchema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static Schema<SearchResult> schema() {
 *         UnionBuilder<SearchResult> newObject = UnionBuilder.newObject();
 *         newObject.description(Optional.ofNullable("SearchResult"));
 *         newObject.name("SearchResult");
 *         newObject.subSchema("Book", BookSchema.schema);
 *         newObject.subSchema("Author", AuthorSchema.schema);
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface UnionSchema {}
