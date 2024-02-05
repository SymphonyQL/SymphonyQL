package symphony.execution

import io.circe.Json

import org.apache.pekko.*
import org.apache.pekko.actor.*
import org.openjdk.jmh.annotations.*
import symphony.*
import zio.{ Executor as _, Scope as _, * }
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import java.util.concurrent.TimeUnit
import scala.concurrent.*
import scala.concurrent.duration.*

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class SymphonyQLBenchmarks {

  implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")

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
    Await.result(actorSystem.terminate(), 5.seconds)

  @Benchmark
  def simpleCaliban(): Unit = {
    val io = Caliban.interpreter.execute(simpleQuery)
    Caliban.run(io)
    ()
  }

  @Benchmark
  def simpleSymphonyQL(): Unit = {
    val future = Symphony.graphql.runWith(SymphonyQLRequest(Some(simpleQuery)))
    Await.result(future, scala.concurrent.duration.Duration.create(1, TimeUnit.MINUTES))
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
