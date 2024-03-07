---
title: Schema Specification
sidebar_label: Schema Specification
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema.md
---

A SymphonyQL schema will be generated automatically at compile-time from the types present in your resolver.

The following table shows how to convert common Scala/Java types to GraphQL types.

| Scala Type (Java Type)                                                         | GraphQL Type                                     |
|--------------------------------------------------------------------------------|--------------------------------------------------|
| `Boolean` (`boolean`)                                                          | Boolean                                          |
| `Int` (`int`)                                                                  | Int                                              |
| `Short` (`short`)                                                              | Int                                              |
| `Float` (`float`)                                                              | Float                                            |
| `Double` (`double`)                                                            | Float                                            |
| `String` (`String`)                                                            | String                                           |
| `BigDecimal` (`BigDecimal`)                                                    | BigDecimal (custom scalar)                       |
| `Unit` (`void`)                                                                | Unit (custom scalar)                             |
| `Long` (`long`)                                                                | Long (custom scalar)                             |
| `BigInt` (`BigInteger`)                                                        | BigInt (custom scalar)                           |
| Case Class (Record Class)                                                      | Object                                           |
| Enum Class (Enum Class)                                                        | Enum                                             |
| Sealed Trait (Sealed Interface)                                                | Enum or Union or Interface                       |
| `Option[A]` (`Optional<A>`)                                                    | Nullable A                                       |
| `List[A]` (`List<A>`)                                                          | List of A                                        |
| `Set[A]` (`Set<A>`)                                                            | List of A                                        |
| `Seq[A]` (not have)                                                            | List of A                                        |
| `Vector[A]` (`Vector<A>`)                                                      | List of A                                        |
| `A => B` (`Function<A, B>`)                                                    | A and B                                          |
| `() => A` (`Supplier<A>`)                                                      | A                                                |
| `Future[A]` (`CompletionStage<A>`)                                             | Nullable A                                       |
| `Tuple2[A, B]` (not have)                                                      | Object with 2 fields `_1` and `_2`               |
| `Either[A, B]` (not have)                                                      | Object with 2 nullable fields `left` and `right` |
| `Map[A, B]` (`Map<A, B>`)                                                      | List of Object with 2 fields `key` and `value`   |
| pekko-streams `scaladsl.Source[A, NotUsed]`<br/>(`javadsl.Source<A, NotUsed>`) | A (subscription) or List of A (query, mutation)  |

## Scala

### Enums, unions, interfaces

A sealed trait will be converted to a different GraphQL type depending on its content:

- a sealed trait with only case objects will be converted to an `ENUM`
- a sealed trait with only case classes will be converted to a `UNION`

GraphQL does not support empty objects, so in case a sealed trait mixes case classes and case objects, a union type will be created and the case objects will have a "fake" field named `_` which is not queryable:
```scala
sealed trait ORIGIN
object ORIGIN {
  case object EARTH extends ORIGIN
  case object MARS  extends ORIGIN
  case object BELT  extends ORIGIN
}
```

The snippet above will produce the following GraphQL type:
```graphql
enum Origin {
  BELT
  EARTH
  MARS
}
```

Here's an example of union:
```scala
sealed trait Role
object Role {
  case class Captain(shipName: String) extends Role
  case class Engineer(specialty: String) extends Role
  case object Mechanic extends Role
}
```

The snippet above will produce the following GraphQL type:
```graphql
union Role = Captain | Engineer | Mechanic

type Captain {
  shipName: String!
}

type Engineer {
  specialty: String!
}

type Mechanic {
  _: Boolean!
}
```

## Java

### Enums, unions, interfaces

In Java, only the enum classes can be used to define GraphQL enum types.

A sealed interface will be converted to a different GraphQL type depending on its content:

- a sealed interface with only record classes will be converted to a `UNION` if exists `@UnionSchema` on interface type.
- a sealed interface with only record classes will be converted to a `INTERFACE` if exists `@InterfaceSchema` on interface type.

Sealed interfaces have other subclasses that are not supported at this time.

Here's an example of union:
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