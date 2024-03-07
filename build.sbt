import Dependencies.Versions.*

inThisBuild(
  List(
    scalaVersion           := scala3_Version,
    organization           := "io.github.symphonyql",
    sonatypeCredentialHost := "oss.sonatype.org",
    sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
    homepage               := Some(url("https://github.com/SymphonyQL")),
    licenses               := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers             := List(
      Developer(
        id = "jxnu-liguobin",
        name = "jxnu-liguobin",
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

lazy val SymphonyQL = (project in file("."))
  .aggregate(
    core,
    parser,
    server,
    validator,
    `java-apt`,
    `java-apt-tests`,
    annotations,
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
  .dependsOn(annotations)
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-parser",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.parser
  )

lazy val core = (project in file("modules/core"))
  .dependsOn(parser, validator, annotations)
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-core",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.core
  )

lazy val server = (project in file("modules/server"))
  .dependsOn(core)
  .settings(
    publish / skip := false,
    commonSettings,
    name           := "symphony-server",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.server
  )

lazy val annotations = (project in file("modules/annotations"))
  .settings(
    publish / skip   := false,
    name             := "symphony-annotations",
    commands ++= Commands.value,
    javafmtOnCompile := true
  )

lazy val `java-apt` = (project in file("modules/java-apt"))
  .dependsOn(core)
  .settings(
    publish / skip   := false,
    name             := "symphony-java-apt",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.apt,
    javafmtOnCompile := true
  )

lazy val `java-apt-tests` = (project in file("modules/java-apt-tests"))
  .dependsOn(core, `java-apt`)
  .settings(
    publish / skip   := true,
    name             := "symphony-java-apt-tests",
    commands ++= Commands.value,
    compileOrder     := CompileOrder.JavaThenScala,
    commands ++= Commands.value,
    javafmtOnCompile := true,
    Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed",
    libraryDependencies ++= Dependencies.Deps.`apt-tests`,
    Compile / javacOptions ++= Seq(
      "-processor",
      "symphony.apt.SymphonyQLProcessor",
      "-s",
      ((Compile / crossTarget).value / "src_managed").getAbsolutePath,
      "-XprintRounds",
      "-Xlint:deprecation"
    )
  )

lazy val examples = (project in file("examples"))
  .dependsOn(server, core, `java-apt`)
  .settings(
    publish / skip   := true,
    commonSettings,
    compileOrder     := CompileOrder.JavaThenScala,
    commands ++= Commands.value,
    javafmtOnCompile := true,
    Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed",
    libraryDependencies ++= Seq(
      "javax.annotation" % "javax.annotation-api" % "1.3.2"
    ),
    Compile / javacOptions ++= Seq(
      "-processor",
      "symphony.apt.SymphonyQLProcessor",
      "-s",
      ((Compile / crossTarget).value / "src_managed").getAbsolutePath,
      "-XprintRounds",
      "-Xlint:deprecation"
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

lazy val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    name           := "symphony-docs",
    mdocIn         := (ThisBuild / baseDirectory).value / "docs",
    run / fork     := true,
    scalacOptions -= "-Xfatal-warnings",
    scalacOptions += "-Wunused:imports"
  )
  .dependsOn(core, parser, `java-apt`, server, validator)
