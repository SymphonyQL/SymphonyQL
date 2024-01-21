import sbt.*

object Dependencies {

  object Versions {
    val scala3Version        = "3.3.1"
    val `pekko-core-Version` = "1.0.2"
    val `pekko-http-Version` = "1.0.0"
    val parboiled            = "2.5.1"
    val scalatest            = "3.2.17"
  }

  object Deps {
    import Versions.*

    // parser schema and query, convert to ADTs
    // all ADTs definition, each GraphQL type has a companion object to construct DSL.
    val parser = sbt.Def.setting {
      Seq(
        "org.parboiled" %% "parboiled" % parboiled,
        "org.scalatest" %% "scalatest" % scalatest % Test
      )
    }

    // prepare, analyzer, run, tracing
    val core = sbt.Def.setting {
      Seq(
        "org.apache.pekko" %% "pekko-http"    % `pekko-http-Version`,
        "org.apache.pekko" %% "pekko-stream"  % `pekko-core-Version`,
        "org.apache.pekko" %% "pekko-testkit" % `pekko-core-Version` % Test
      )
    }
  }

}
