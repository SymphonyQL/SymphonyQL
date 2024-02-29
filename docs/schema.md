---
title: Schema Specification
sidebar_label: Schema Specification
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema.md
---

A SymphonyQL schema will be generated automatically at compile-time from the types present in your resolver.

The following table shows how to convert common Scala/Java types to SymphonyQL types.

| Scala Type (Java Type)                                                         | SymphonyQL Type                                  |
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
| `Option[A]` (`Optional[A]`)                                                    | Nullable A                                       |
| `List[A]` (`List[A]`)                                                          | List of A                                        |
| `Set[A]` (`Set[A]`)                                                            | List of A                                        |
| `Seq[A]` (not have)                                                            | List of A                                        |
| `Vector[A]` (`Vector[A]`)                                                      | List of A                                        |
| `A => B` (`Function[A, B]`)                                                    | A and B                                          |
| `() => A` (`Supplier[A]`)                                                      | A                                                |
| `Future[A]` (`CompletionStage[A]`)                                             | Nullable A                                       |
| `Tuple2[A, B]` (not have)                                                      | Object with 2 fields `_1` and `_2`               |
| `Either[A, B]` (not have)                                                      | Object with 2 nullable fields `left` and `right` |
| `Map[A, B]` (`Map[A, B]`)                                                      | List of Object with 2 fields `key` and `value`   |
| pekko-streams `scaladsl.Source[A, NotUsed]`<br/>(`javadsl.Source[A, NotUsed]`) | A (subscription) or List of A (query, mutation)  |