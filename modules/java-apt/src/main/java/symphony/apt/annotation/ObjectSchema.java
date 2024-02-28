package symphony.apt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * &#64;ObjectSchema creates a class with schema method for SymphonyQL Object Type. Support Enum and
 * Record classes.
 *
 * <p>Example1:<br>
 * Original class OutputObject:
 *
 * <pre>{@code
 * @ObjectSchema
 * record OutputObject(
 *         Optional<OriginEnum> optionalEnum,
 *         Optional<String> optionalString,
 * ) { }
 * }</pre>
 *
 * <p>Generated class OutputObjectSchema:
 *
 * <pre>{@code
 * import java.math.BigDecimal;
 * import java.util.List;
 * import java.util.Map;
 * import java.util.Optional;
 * import java.util.function.Function;
 * import javax.annotation.Generated;
 * import symphony.parser.adt.introspection.__Field;
 * import symphony.schema.Schema;
 * import symphony.schema.javadsl.FieldBuilder;
 * import symphony.schema.javadsl.ObjectBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class OutputObjectSchema {
 *     public static final Schema<OutputObject> schema = schema();
 *
 *     private OutputObjectSchema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static Schema<OutputObject> schema() {
 *         ObjectBuilder<OutputObject> newObject = ObjectBuilder.newObject();
 *         newObject.name("OutputObject");
 *         newObject.field(
 *             new Function<FieldBuilder, __Field>() {
 *                 @Override
 *                 public __Field apply(FieldBuilder builder) {
 *                     return builder.name("originEnum").schema(symphony.example.schema.complex.OriginEnumSchema.schema).build();
 *                 }
 *             },
 *             new Function<OutputObject, OriginEnum>() {
 *                 @Override
 *                 public OriginEnum apply(OutputObject obj) {
 *                     return obj.originEnum();
 *                 }
 *             }
 *         );
 *         newObject.field(
 *             new Function<FieldBuilder, __Field>() {
 *                 @Override
 *                 public __Field apply(FieldBuilder builder) {
 *                     return builder.name("optionalString").schema(Schema.createOptional((Schema<String>) Schema.getSchema("java.lang.String"))).build();
 *                 }
 *             },
 *             new Function<OutputObject, Optional<String>>() {
 *                 @Override
 *                 public Optional<String> apply(OutputObject obj) {
 *                     return obj.optionalString();
 *                 }
 *             }
 *         );
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 *
 * <p>Example2:<br>
 *
 * <pre>{@code
 * @ObjectSchema(withArgs = true)
 * record QueriesObject(
 *         Function<InputObject, Source<OutputObject, NotUsed>> characters,
 *         Function<InputObject, OutputObject> character,
 * ) { }
 * }</pre>
 *
 * <p>Generated class QueriesObjectSchema:
 *
 * <pre>{@code
 * import java.util.function.Function;
 * import java.util.function.Supplier;
 * import javax.annotation.Generated;
 * import org.apache.pekko.NotUsed;
 * import org.apache.pekko.stream.javadsl.Source;
 * import symphony.parser.adt.introspection.__Field;
 * import symphony.schema.ArgumentExtractor;
 * import symphony.schema.Schema;
 * import symphony.schema.javadsl.FieldBuilder;
 * import symphony.schema.javadsl.ObjectBuilder;
 *
 * @Generated("symphony.apt.SymphonyQLProcessor")
 * @SuppressWarnings("all")
 * public final class QueriesObjectSchema {
 *     public static final Schema<QueriesObject> schema = schema();
 *
 *     private QueriesObjectSchema() {
 *         throw new UnsupportedOperationException();
 *     }
 *
 *     private static Schema<QueriesObject> schema() {
 *         ObjectBuilder<QueriesObject> newObject = ObjectBuilder.newObject();
 *         newObject.name("QueriesObject");
 *         newObject.fieldWithArg(
 *                 new Function<FieldBuilder, __Field>() {
 *                     @Override
 *                     public __Field apply(FieldBuilder builder) {
 *                         return builder.name("characters").schema(Schema.createFunction(symphony.example.schema.complex.InputObjectInputSchema.schema, symphony.example.schema.complex.InputObjectExtractor.extractor, Schema.createSource(symphony.example.schema.complex.OutputObjectSchema.schema))).build();
 *                     }
 *                 },
 *                 new Function<QueriesObject, Function<InputObject, Source<OutputObject, NotUsed>>>() {
 *                     @Override
 *                     public Function<InputObject, Source<OutputObject, NotUsed>> apply(QueriesObject obj) {
 *                         return obj.characters();
 *                     }
 *                 }
 *         );
 *         newObject.fieldWithArg(
 *                 new Function<FieldBuilder, __Field>() {
 *                     @Override
 *                     public __Field apply(FieldBuilder builder) {
 *                         return builder.name("character").schema(Schema.createFunction(symphony.example.schema.complex.InputObjectInputSchema.schema, symphony.example.schema.complex.InputObjectExtractor.extractor, symphony.example.schema.complex.OutputObjectSchema.schema)).build();
 *                     }
 *                 },
 *                 new Function<QueriesObject, Function<InputObject, OutputObject>>() {
 *                     @Override
 *                     public Function<InputObject, OutputObject> apply(QueriesObject obj) {
 *                         return obj.character();
 *                     }
 *                 }
 *         );
 *         return newObject.build();
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface ObjectSchema {
  boolean withArgs() default false;
}
