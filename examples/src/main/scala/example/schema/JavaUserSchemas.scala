package example.schema

import symphony.parser.*
import symphony.parser.SymphonyQLValue.StringValue
import symphony.parser.adt.introspection.IntrospectionEnumValue
import symphony.schema.*
import symphony.schema.javadsl.*
import UserAPI.*
// for parsing input parameters
val argumentExtractor: ArgumentExtractor[FilterArgs] = {
  case SymphonyQLInputValue.ObjectValue(fields) =>
    Right(FilterArgs(Some(Origin.valueOf(fields("origin").asInstanceOf[StringValue].value))))
  case _                                        => Left(SymphonyQLError.ArgumentError("error"))
}
// create enum schema by `Schema.mkEnum`
val enumSchema                                       = Schema
  .mkEnum[Origin](
    "Origin",
    None,
    List(
      IntrospectionEnumValue("EARTH", None, false, None, None),
      IntrospectionEnumValue("MARS", None, false, None, None),
      IntrospectionEnumValue("BELT", None, false, None, None)
    ),
    _.toString
  )
// Schema DSL
// create input schema by InputObjectBuilder
val inputSchema: Schema[FilterArgs]                  = InputObjectBuilder
  .newInputObject[FilterArgs]()
  .name("FilterArgs")
  .fields(builder => builder.name("name").schema(Schema.mkOption(enumSchema)).build())
  .build()
// create output schema by ObjectBuilder
val outputSchema: Schema[Character]                  = ObjectBuilder
  .newObject[Character]()
  .name("Character")
  .fields(
    builder => builder.name("name").schema(Schema.StringSchema).build() -> (_ => Stage.createNull()),
    builder => builder.name("origin").schema(enumSchema).build() -> (_ => Stage.createNull())
  )
  .build()

// create object (resolver) schema by ObjectBuilder with `hasArgs`
val queriesSchema: Schema[Queries] = ObjectBuilder
  .newObject[Queries]()
  .name("Queries")
  .fields(builder =>
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
      .build() -> (a =>
      Stage.FunctionStage { args =>
        Stage.createScalaSource {
          a.characters(argumentExtractor.extract(SymphonyQLInputValue.ObjectValue(args)).toOption.orNull).map { c =>
            Stage.ObjectStage(
              "Character",
              Map(
                "name"   -> Stage.createPure(SymphonyQLValue.StringValue(c.name)),
                "origin" -> Stage.createPure(SymphonyQLValue.EnumValue(c.origin.toString))
              )
            )
          }
        }
      }
    ),
  )
  .build()
