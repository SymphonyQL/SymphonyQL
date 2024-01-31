package symphony.derived

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

import symphony.schema.*

object SchemaDerivationGen {

  implicit def mkOption[A](implicit schema: Schema[A]): Schema[Option[A]] = Schema.mkOption(schema)

  implicit def mkList[A](implicit schema: Schema[A]): Schema[List[A]] = Schema.mkList(schema)

  implicit def mkFuncSchema[A, B](implicit
    argumentExtractor: ArgumentExtractor[A],
    inputSchema: Schema[A],
    outputSchema: Schema[B]
  ): Schema[A => B] = Schema.mkFuncSchema(argumentExtractor, inputSchema, outputSchema)

  implicit def mkSourceSchema[A](implicit schema: Schema[A]): Schema[Source[A, NotUsed]] =
    Schema.mkSourceSchema(schema)

  implicit lazy val unit: Schema[Unit]       = Schema.unit
  implicit lazy val boolean: Schema[Boolean] = Schema.boolean
  implicit lazy val string: Schema[String]   = Schema.string
  implicit lazy val int: Schema[Int]         = Schema.int
  implicit lazy val long: Schema[Long]       = Schema.long
  implicit lazy val double: Schema[Double]   = Schema.double
  implicit lazy val float: Schema[Float]     = Schema.float
}
