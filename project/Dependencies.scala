import sbt.*

object Dependencies {

  object Versions {
    val scala3Version = "3.3.1"
    val pekkoVersion  = "1.0.2"
    val parboiled     = "2.5.1"
  }

  object Deps {
    import Versions.*

    // parser schema and query, convert to ADTs
    // all ADTs definition, each GraphQL type has a companion object to construct DSL.
    val parser = sbt.Def.setting {
      Seq(
        "org.parboiled" %% "parboiled" % parboiled
      )
    }

    // prepare, analyzer, run, tracing
    val core = sbt.Def.setting {
      Seq(
        "org.apache.pekko" %% "pekko-http"    % pekkoVersion,
        "org.apache.pekko" %% "pekko-stream"  % pekkoVersion,
        "org.apache.pekko" %% "pekko-testkit" % pekkoVersion % Test
      )
    }
  }

}
