package symphony.example.schema

import symphony.example.schema.Users.*
import symphony.parser.*
import symphony.parser.SymphonyQLValue.StringValue
import symphony.parser.adt.introspection.*
import symphony.schema.*
import symphony.schema.Stage.createObject
import symphony.schema.javadsl.*
// for parsing input parameters
val argumentExtractor: ArgumentExtractor[FilterArgs] = {
  case SymphonyQLInputValue.ObjectValue(fields) =>
    Right(FilterArgs(Some(Origin.valueOf(fields("origin").asInstanceOf[StringValue].value))))
  case _                                        => Left(SymphonyQLError.ArgumentError("error"))
}
// Schema DSL
// or create enum schema by `Schema.mkEnum`
val enumSchema                                       = EnumBuilder
  .newEnum[Origin]()
  .name("Origin")
  .values(
    __EnumValue("EARTH", None, false, None, None),
    __EnumValue("MARS", None, false, None, None),
    __EnumValue("BELT", None, false, None, None)
  )
  .serialize(new JavaFunction[Origin, String]() {
    override def apply(t: Origin): String = t.toString
  })
  .build()

// create input schema by InputObjectBuilder
val inputSchema: Schema[FilterArgs] = InputObjectBuilder
  .newInputObject[FilterArgs]()
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

// create object (resolver) schema by ObjectBuilder with `hasArgs`
val queriesSchema: Schema[Queries] = ObjectBuilder
  .newObject[Queries]()
  .name("Queries")
  .fieldWithArg(
    builder =>
      builder
        .name("characters")
        .hasArgs(true)
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
