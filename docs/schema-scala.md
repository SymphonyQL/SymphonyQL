---
title: Defining the Schema - Scala
sidebar_label: Defining the Schema - Scala
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-scala.md
---

In most cases, we can use `Schema.derived[A]` to automatically derive the schema, but if we want to define it manually, We can use the builder class in `symphony.schema.builder.*`.

## Manually Derivation

This is a Scala-defined SymphonyQL's API:
```scala
enum Origin {
  case EARTH, MARS, BELT
}

case class Character(name: String, origin: Origin)
case class FilterArgs(origin: Option[Origin])
case class Queries(characters: FilterArgs => Source[Character, NotUsed])
```

Automatically derive a schema:
```scala
Schema.derived[Queries]
// summon[Schema[Queries]]
```

If we want to customize it, we just need to define a new implicit.

### EnumBuilder

Defining SymphonyQL **Enum Type**, for example:
```scala
type JavaFunction[T, R] = java.util.function.Function[T, R]

implicit val enumSchema: Schema[Origin] = EnumBuilder
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
```

### InputObjectBuilder

Defining SymphonyQL **Input Object Type**, for example:
```scala
implicit val inputSchema: Schema[FilterArgs] = InputObjectBuilder
  .newObject[FilterArgs]()
  .name("FilterArgs")
  .fields(builder => builder.name("name").schema(summon[Schema[Option[Origin]]]).build())
  .build()
```

`FilterArgs` will be tiled, so the input parameter is `origin` and `Option<Origin>` is the default supported type, no need for anything extra. For more types, please refer to the [Schema Specification](schema.md).

### ObjectBuilder

Defining simple SymphonyQL **Object Type**, for example:
```scala
implicit val outputSchema: Schema[Character] = ObjectBuilder
  .newObject[Character]()
  .name("Character")
  .field[String](
    builder => builder.name("name").schema(summon[Schema[String]]).build(),
    c => c.name
  )
  .field[Origin](
    builder => builder.name("origin").schema(summon[Schema[Origin]]).build(),
    c => c.origin
  )
  .build()
```

Defining complex SymphonyQL **Object Type** for **resolver**, for example:
```scala
implicit val queriesSchema: Schema[Queries] = ObjectBuilder
  .newObject[Queries]()
  .name("Queries")
  .fieldWithArg(
    builder =>
      builder
        .name("characters")
        .schema(summon[Schema[FilterArgs => scaladsl.Source[Character, NotUsed]]])
        .build(),
    a => a.characters
  )
  .build()
```

Each **resolver** can contain multiple fields, each of which is a Query/Mutation/Subscription API.
For more types, please refer to the [Schema Specification](schema.md).

## Helper Annotations

1. Fields refer to components of the case class.
2. Type refers to the case class

### @GQLDefault

Annotation to specify the default value of an input field.

### @GQLDeprecated

Annotation used to indicate a type or a field is deprecated.

### @GQLDescription

Annotation used to provide a description to a field or a type.

### @GQLExcluded

Annotation used to exclude a field from a type.

### @GQLInputName

Annotation used to customize the name of an input type.

### @GQLInterface

Annotation to make a sealed trait an interface instead of a union type or an enum.

### @GQLName

Annotation used to provide an alternative name to a field or a type.

### @GQLUnion

Annotation to make a sealed trait a union instead of an enum.