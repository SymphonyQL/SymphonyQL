import Dependencies.Versions.*

inThisBuild(
  List(
    scalaVersion           := scala3_Version,
    organization           := "org.symphonyql",
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
    homepage               := Some(url("https://github.com/SymphonyQL")),
    licenses               := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers             := List(
      Developer(
        id = "jxnu-liguobin",
        name = "正在登陆",
        email = "dreamylost@outlook.com",
        url = url("https://github.com/jxnu-liguobin")
      )
    )
  )
)

lazy val commonSettings =
  Seq(
    Test / fork  := true,
    run / fork   := true,
    scalaVersion := scala3_Version,
    scalacOptions ++= Seq(
      "-language:dynamics",
      "-explain",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-explain-types",
      "-Ykind-projector",
      "-language:higherKinds",
      "-language:existentials",
      "-Xfatal-warnings"
    ) ++ Seq("-Xmax-inlines", "100")
  )

lazy val root = (project in file("."))
  .aggregate(
    core,
    parser,
    server,
    validator,
    `java-apt`,
    examples,
    benchmarks
  )
  .settings(
    publish / skip := true,
    commonSettings,
    commands ++= Commands.value
  )

lazy val validator = (project in file("modules/validator"))
  .dependsOn(parser)
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-validator",
    commands ++= Commands.value
  )

lazy val parser = (project in file("modules/parser"))
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-parser",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.parser.value
  )

lazy val core = (project in file("modules/core"))
  .dependsOn(parser, validator)
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-core",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.core.value
  )

lazy val server     = (project in file("modules/server"))
  .dependsOn(core)
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-server",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.server.value
  )
lazy val `java-apt` = (project in file("modules/java-apt"))
  .settings(
    publish / skip := false,
    name           := "symphony-java-apt",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.apt.value,
    javafmtOnCompile := true
  )

lazy val examples = (project in file("examples"))
  .dependsOn(server, core, `java-apt`)
  .settings(
    publish / skip := true,
    commonSettings,
    commands ++= Commands.value,
    Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed",
    libraryDependencies ++= Seq(
      "javax.annotation" % "javax.annotation-api" % "1.3.2"
    ),
    Compile / javacOptions ++= Seq(
      "-processor",
      "symphony.apt.SymphonyQLProcessor",
      "-s",
      ((Compile / crossTarget).value / "src_managed").getAbsolutePath,
      "-XprintRounds"
    )
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .settings(commonSettings)
  .settings(
    publish / skip := true
  )
  .dependsOn(core)
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr" %% "caliban"       % "2.5.1",
      "org.apache.pekko"      %% "pekko-stream"  % `pekko-core_Version`,
      "org.parboiled"         %% "parboiled"     % `parboiled_Version`,
      "org.sangria-graphql"   %% "sangria"       % "4.1.0",
      "org.sangria-graphql"   %% "sangria-circe" % "1.3.2",
      "io.circe"              %% "circe-parser"  % "0.14.6",
      "com.graphql-java"       % "graphql-java"  % "21.3"
    )
  )
