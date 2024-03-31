package symphony.execution

import io.circe.Json
import org.apache.pekko.*
import org.apache.pekko.actor.*
import org.openjdk.jmh.annotations.*
import symphony.*
import zio.{ Executor as _, Scope as _, * }
import sangria.execution.*
import sangria.marshalling.circe.*
import sangria.parser.QueryParser

import java.util.concurrent.TimeUnit
import scala.concurrent.*
import scala.concurrent.duration.*
import graphql.{ ExecutionInput, ExecutionResult }
import scala.jdk.FutureConverters.*

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class SymphonyQLBenchmarks {

  implicit val actorSystemScala: ActorSystem = ActorSystem("symphonyActorSystemScala")
  val actorSystemJava: ActorSystem           = ActorSystem("symphonyActorSystemJava")

  implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  val simpleQuery: String =
    """{
          characters {
            name
            origin
          }
       }""".stripMargin

  @TearDown
  def shutdown(): Unit =
    Await.result(actorSystemScala.terminate(), 5.seconds)
    Await.result(actorSystemJava.terminate(), 5.seconds)

  @Benchmark
  def simpleCaliban(): Unit = {
    val io = Caliban.interpreter.execute(simpleQuery)
    Caliban.run(io)
    ()
  }

  @Benchmark
  def simpleSymphonyQLJava(): Unit = {
    val future = SymphonyJava.graphql.runWith(SymphonyQLRequest(simpleQuery))(actorSystemJava)
    Await.result(future, scala.concurrent.duration.Duration.create(1, TimeUnit.MINUTES))
    ()
  }

  @Benchmark
  def simpleSymphonyQLScala(): Unit = {
    val future = SymphonyScala.graphql.runWith(SymphonyQLRequest(simpleQuery))
    Await.result(future, scala.concurrent.duration.Duration.create(1, TimeUnit.MINUTES))
    ()
  }

  @Benchmark
  def simpleGraphQLJava(): Unit = {
    val executionResult = GraphQLJava.build
      .executeAsync(
        ExecutionInput
          .newExecutionInput()
          .query(simpleQuery)
          .build()
      )
      .asScala
    Await.result(executionResult, scala.concurrent.duration.Duration.create(1, TimeUnit.MINUTES))
    ()
  }

  @Benchmark
  def simpleSangria(): Unit = {
    val future: Future[Json] =
      Future.fromTry(QueryParser.parse(simpleQuery)).flatMap(queryAst => Executor.execute(Sangria.schema, queryAst))
    Await.result(future, 1.minute)
    ()
  }
}
