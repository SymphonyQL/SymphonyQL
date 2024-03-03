---
title: Defining the Schema - Java
sidebar_label: Defining the Schema - Java
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-java.md
---

In Java, there is no metaprogramming, we use APT (Java Annotation Processing) to generate codes.

## Core Annotations

If we want to define it manually, we can use the builder class in `symphony.schema.builder.*` and add the `@IgnoreSchema` annotation on record class.

Then, we should create a class **under the same package**:
- Record class `A` is **Object** (or *Enum*). Create an `ASchema` class with field `public static final Schema<A> schema = ???;`.
- Record class `A` is **Input Object**. Create an `AInputSchema` class with field `public static final Schema<A> schema = ???;`.
- Record class `A` is **Argument Extractor** (*Input Object* or if *Enum* is in *Input Object*). Create an `AExtractor` class with field `public static final ArgumentExtractor<A> extractor = ???;`.

If these are not provided, an error will be reported by javac on which type has `@IgnoreSchema`, such as `A or schema can't be found.`.

### @EnumSchema

Defining SymphonyQL **Enum Type**, for example:
```java
@EnumSchema
enum OriginEnum {
    EARTH, MARS, BELT
}
```

The enumeration used in **Input Object Type** must be annotated with `@ArgExtractor`.

It is equivalent to the GraphQL Enum Type:
```graphql
enum Origin {
  EARTH
  MARS
  BELT
}
```

### @InputSchema

Defining SymphonyQL **Input Object Type**, for example:
```java
@InputSchema
@ArgExtractor
record FilterArgs(Optional<Origin> origin, Optional<NestedArg> nestedArg) {
}
```

Any custom type (including enumeration) used for **Input Object Type** needs to be annotated with `ArgExtractor`.

`FilterArgs` will be tiled, so the input parameters are `origin` and `nestedArg`, and `Optional<Origin>` is the default supported type, no need for anything extra. For more types, please refer to the [Schema Specification](schema.md).

As mentioned above, `NestedArg` is a custom type used in **Input Object Type**.

In order to generate the correct **Schema**, `NestedArg` must be defined with `@InputSchema` and `@ArgExtractor`, for example:
```java
@InputSchema
@ArgExtractor
record NestedArg(String id, Optional<String> name) {
}
```

It is equivalent to the GraphQL Input Type:
```graphql
input NestedArgInput {
    id: String!
    name: String
}
```

### @ObjectSchema

Defining simple SymphonyQL **Object Type**, for example:
```java
@ObjectSchema
record CharacterOutput(String name, Origin origin) {
}
```

The object can be any record class, nested types also require annotation.

It is equivalent to the GraphQL Object Type:
```graphql
type CharacterOutput {
  name: String!
  origin: Origin!
}
```

Defining complex SymphonyQL **Object Type** for **resolver**, for example:
```java
@ObjectSchema
record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
```

Each **resolver** can contain multiple fields, each of which is a Query/Mutation/Subscription API. 
For more types, please refer to the [Schema Specification](schema.md).

It is equivalent to the GraphQL Object Type:
```graphql
# There is no FilterArgs, but it has all its fields: origin, nestedArg
type Queries {
    characters(origin: Origin, nestedArg: NestedArgInput): [CharacterOutput!]
}
```

## Helper Annotations

1. Fields refer to components of the record class.
2. Type refers to the record class

### @IgnoreSchema

Annotation to ignore class from SymphonyQL's processing.

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

Annotation to make an interface type.

### @GQLName

Annotation used to provide an alternative name to a field or a type.

### @GQLUnion

Annotation to make an interface a union instead of an interface.