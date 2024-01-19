import sbt.*

object Dependencies {

  object Versions {
    val scala3Version = "3.3.1"
    val pekkoVersion  = "1.0.0"
    val parboiled     = "2.5.1"
  }

  object Deps {
    import Versions.*

    val core = sbt.Def.setting {
      Seq(
        "org.apache.pekko" %% "pekko-http"    % pekkoVersion,
        "org.apache.pekko" %% "pekko-stream"  % pekkoVersion,
        "org.apache.pekko" %% "pekko-testkit" % pekkoVersion % Test,
        "org.parboiled"    %% "parboiled"     % parboiled
      )
    }
  }

}
