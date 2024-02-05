# Schemas

A SymphonyQL schema will be derived automatically at compile-time from the types present in your resolver.

The following table shows how to convert common Scala/Java types to SymphonyQL types.

| Scala Type (Java Type)                                                                                       | SymphonyQL Type                                 |
|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `Boolean` (`boolean`)                                                                                        | Boolean                                         |
| `Int` (`int`)                                                                                                | Int                                             |
| `Float` (`float`)                                                                                            | Float                                           |
| `Double` (`double`)                                                                                          | Float                                           |
| `String` (`String`)                                                                                          | String                                          |
| `Unit` (`void`)                                                                                              | Unit (custom scalar)                            |
| `Long` (`long`)                                                                                              | Long (custom scalar)                            |
| Case Class (Record Class)                                                                                    | Object                                          |
| `Option[A]` (`Optional[A]`)                                                                                  | Nullable A                                      |
| `List[A]` (`java.util.List[A]`)                                                                              | List of A                                       |
| `Set[A]` (`java.util.Set[A]`)                                                                                | List of A                                       |
| `Seq[A]` (not have)                                                                                          | List of A                                       |
| `Vector[A]` (`java.util.Vector[A]`)                                                                          | List of A                                       |
| `A => B` (`java.util.function.Function`)                                                                     | A and B                                         |
| `Future[A]` (`CompletionStage`)                                                                              | Nullable A                                      |
| `org.apache.pekko.stream.scaladsl.Source[A, NotUsed]` (`org.apache.pekko.stream.javadsl.Source[A, NotUsed]`) | A (subscription) or List of A (query, mutation) |