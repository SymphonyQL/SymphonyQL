package symphony.example.schema

import symphony.example.schema.Users.*
import symphony.parser.*
import symphony.parser.SymphonyQLValue.StringValue
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.schema.javadsl.*
// for parsing input parameters
val argumentExtractor: ArgumentExtractor[FilterArgs] = {
  case SymphonyQLInputValue.ObjectValue(fields) =>
    Right(FilterArgs(Some(Origin.valueOf(fields("origin").asInstanceOf[StringValue].value))))
  case _                                        => Left(SymphonyQLError.ArgumentError("error"))
}
// Schema DSL
// create enum by EnumBuilder
val enumSchema                                       = EnumBuilder
  .newEnum[Origin]()
  .name("Origin")
  .values(
    builder => builder.name("EARTH").isDeprecated(false).build(),
    builder => builder.name("MARS").isDeprecated(false).build(),
    builder => builder.name("BELT").isDeprecated(false).build()
  )
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
  .field(builder => builder.name("name").schema(Schema.StringSchema).build())
  .field(builder => builder.name("origin").schema(enumSchema).build())
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
    a => Stage.derivesStageByReflection(argumentExtractor, args => a.characters(args))
  )
  .build()
