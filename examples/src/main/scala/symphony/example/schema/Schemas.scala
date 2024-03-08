package symphony.example.schema

import symphony.parser.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.schema.builder.*
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import symphony.example.schema.Users.*

import java.util.Optional
import scala.jdk.javaapi.OptionConverters

// for parsing input parameters
val argumentExtractor: ArgumentExtractor[FilterArgs] = {
  case SymphonyQLInputValue.ObjectValue(fields) =>
    Right(FilterArgs(Some(Origin.valueOf(fields("origin").asInstanceOf[StringValue].value))))
  case _                                        => throw new RuntimeException("Expected ObjectValue");
}
// Schema DSL
// create enum by EnumBuilder
val enumSchema                                       = EnumBuilder
  .newEnum[Origin]()
  .name("Origin")
  .value(builder => builder.name("EARTH").isDeprecated(false).build())
  .value(builder => builder.name("MARS").isDeprecated(false).build())
  .value(builder => builder.name("BELT").isDeprecated(false).build())
  .serialize(new JavaFunction[Origin, String]() {
    override def apply(t: Origin): String = t.toString
  })
  .build()

// create input schema by InputObjectBuilder
val inputSchema: Schema[FilterArgs] = InputObjectBuilder
  .newObject[FilterArgs]()
  .name("FilterArgs")
  .fields(builder => builder.name("name").schema(Schema.mkOption(enumSchema)).build())
  .build()
// create output schema by ObjectBuilder
val outputSchema: Schema[Character] = ObjectBuilder
  .newObject[Character]()
  .name("Character")
  .field[String](
    builder => builder.name("name").schema(Schema.StringSchema).build(),
    c => c.name
  )
  .field[Origin](
    builder => builder.name("origin").schema(enumSchema).build(),
    c => c.origin
  )
  .build()

// create object (resolver) schema by ObjectBuilder
val queriesSchema: Schema[Queries] = ObjectBuilder
  .newObject[Queries]()
  .name("Queries")
  .fieldWithArg(
    builder =>
      builder
        .name("characters")
        .schema(
          Schema.mkFunction(
            argumentExtractor,
            inputSchema,
            Schema.mkSource(outputSchema)
          )
        )
        .build(),
    a => a.characters
  )
  .build()
