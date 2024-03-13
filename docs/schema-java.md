---
title: Defining the Schema - Java
sidebar_label: Defining the Schema - Java
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-java.md
---

In Java, there is no metaprogramming, we use APT (Java Annotation Processing) to generate codes.

## Core annotations

### `@EnumSchema`

Defining GraphQL **Enum Type**, for example:
```java
@EnumSchema
enum OriginEnum {
    EARTH, MARS, BELT
}
```

The enumeration used in **Input Object Type** must be annotated with `@ArgExtractor`.

The snippet above will produce the following GraphQL type:
```graphql
enum Origin {
  EARTH
  MARS
  BELT
}
```

### `@InputSchema`

Defining GraphQL **Input Object Type**, for example:
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

The snippet above will produce the following GraphQL type:
```graphql
input NestedArgInput {
    id: String!
    name: String
}
```

### `@ObjectSchema`

Defining simple GraphQL **Object Type**, for example:
```java
@ObjectSchema
record CharacterOutput(String name, Origin origin) {
}
```

The object can be any record class, nested types also require annotation.

The snippet above will produce the following GraphQL type:
```graphql
type CharacterOutput {
  name: String!
  origin: Origin!
}
```

Defining complex GraphQL **Object Type** for **resolver**, for example:
```java
@ObjectSchema
record Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {
}
```

Each **resolver** can contain multiple fields, each of which is a Query/Mutation/Subscription API. 
For more types, please refer to the [Schema Specification](schema.md).

The snippet above will produce the following GraphQL type:
```graphql
# There is no FilterArgs, but it has all its fields: origin, nestedArg
type Queries {
    characters(origin: Origin, nestedArg: NestedArgInput): [CharacterOutput!]
}
```

### `@UnionSchema`

Defining simple GraphQL **Union Type**, for example:
```java
@UnionSchema
public sealed interface SearchResult permits Book, Author {
}

@ObjectSchema
record Book(String title) implements SearchResult {
}

@ObjectSchema
record Author(String name) implements SearchResult {
}
```

The snippet above will produce the following GraphQL type:
```graphql
union SearchResult = Book | Author

type Author {
    name: String
}

type Book {
    title: String
}
```

### `@InterfaceSchema`

Defining simple GraphQL **Interface Type**, for example:
```java
@InterfaceSchema
public sealed interface NestedInterface {
}


@InterfaceSchema
sealed interface Mid1 extends NestedInterface {
}

@InterfaceSchema
sealed interface Mid2 extends NestedInterface {
}

@ObjectSchema
record FooA(String a, String b, String c) implements Mid1 {
}

@ObjectSchema
record FooB(String b, String c, String d) implements Mid1, Mid2 {
}

@ObjectSchema
record FooC(String b, String d, String e) implements Mid2 {
}
```

The snippet above will produce the following GraphQL type:
```graphql
interface Mid1 implements NestedInterface {
    b: String
    c: String
}

interface Mid2 implements NestedInterface {
    b: String
    d: String
}

interface NestedInterface {
    b: String
}

type FooA implements Mid1 {
    a: String
    b: String
    c: String
}

type FooB implements Mid1 & Mid2 {
    b: String
    c: String
    d: String
}

type FooC implements Mid2 {
    b: String
    d: String
    e: String
}
```

### `@IgnoreSchema`

Annotation to ignore class from SymphonyQL's processing.

## Creating a schema manually

If we want to define it manually, we can use the builder class in `symphony.schema.builder.*` and add the `@IgnoreSchema` annotation on record class.

Then, we should create a class **under the same package**:
- If record class `A` is **Object** (or *Enum*, *Union*, *Interface*), a class named `ASchema` should be created with the field `public static final Schema<A> schema = ???;`.
- If record class `A` is **Input Object (or *Enum*)**, a class named `AInputSchema` should be created with the field `public static final Schema<A> schema = ???;`.
- It is also possible to customize the `ArgumentExtractor<A>`, simply created a class named `AExtractor` with the field `public static final ArgumentExtractor<A> extractor = ???;`.

We can use the builder class in `symphony.schema.builder.*` to create `Schema<A>`.

If these are not provided, an error will be reported by javac on which type has `@IgnoreSchema`, such as `A or schema can't be found.`.

## Tool Annotations

1. Fields refer to components of the record class.
2. Type refers to the record class

### `@GQLDefault`

Annotation to specify the default value of an input field.

### `@GQLDeprecated`

Annotation used to indicate a type or a field is deprecated.

### `@GQLDescription`

Annotation used to provide a description to a field or a type.

### `@GQLExcluded`

Annotation used to exclude a field from a type.

### `@GQLInputName`

Annotation used to customize the name of an input type.

### `@GQLName`

Annotation used to provide an alternative name to a field or a type.