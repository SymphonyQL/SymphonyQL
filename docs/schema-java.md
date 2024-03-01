---
title: Defining the Schema - Java
sidebar_label: Defining the Schema - Java
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-java.md
---

In Java, there is no metaprogramming, we use APT (Java Annotation Processing) to generate codes.

## @EnumSchema

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

## @InputSchema

Defining SymphonyQL **Input Object Type**, for example:
```java
@InputSchema
@ArgExtractor
record FilterArgs(Optional<Origin> origin, Optional<NestedArg> nestedArg) {
}
```

Any custom type (including enumeration) used for **Input Object Type** needs to be annotated with `ArgExtractor`.

As mentioned above, `NestedArg` are used in **Input Object Type**, to generate the correct **Schema**,
it is necessary to define `NestedArg` with `@InputSchema` and `@ArgExtractor`, for example:
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

## @ObjectSchema

Defining SymphonyQL **Object Type**.

It has one argument `withArgs`, which defaults to false, for example:
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

When defining a **Resolver** Object, `withArgs` must be `true`.

Each **Resolver** can contain multiple fields, each of which is a Query/Mutation/Subscription API. For example:
```java
@ObjectSchema(withArgs = true)
record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
```

The type of the **Resolver** field must be `java.util.function.Function` or `java.util.function.Supplier`. For more types, please refer to the [Schema Specification](schema.md).

It is equivalent to the GraphQL Object Type:
```graphql
# There is no FilterArgs, but it has all its fields: origin, nestedArg
type Queries {
    characters(origin: Origin, nestedArg: NestedArgInput): [CharacterOutput!]
}
```

## @IgnoreSchema

Ignore class from SymphonyQL's processing.