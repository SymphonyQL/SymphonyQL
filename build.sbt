import Dependencies.Versions.*

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Releases" at "https://s01.oss.sonatype.org/content/repositories/releases"
)

ThisBuild / versionScheme := Some("semver-spec")

inThisBuild(
  List(
    scalaVersion           := scala3_Version,
    organization           := "io.github.jxnu-liguobin", // TODO using io.github.symphonyql
    sonatypeCredentialHost := "oss.sonatype.org",
    sonatypeRepository     := "https://oss.sonatype.org/service/local",
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
    Test / fork      := true,
    run / fork       := true,
    scalaVersion     := scala3_Version,
    doc / sources    := Seq(),
    javafmtOnCompile := true,
    javacOptions ++= Seq("-source", "21", "-encoding", "UTF-8"),
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
    publish / skip       := true,
    commonSettings,
    commands ++= Commands.value,
    jacocoReportSettings := JacocoReportSettings(
      "Jacoco Coverage Report",
      None,
      JacocoThresholds(),
      Seq(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML),
      "UTF-8"
    )
  )

lazy val validator = (project in file("modules/validator"))
  .dependsOn(parser)
  .settings(
    commonSettings,
    publish / skip := false,
    name           := "symphony-validator",
    commands ++= Commands.value
  )

lazy val parser = (project in file("modules/parser"))
  .dependsOn(annotations)
  .settings(
    commonSettings,
    name           := "symphony-parser",
    publish / skip := false,
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.parser
  )

lazy val core = (project in file("modules/core"))
  .dependsOn(parser, validator, annotations)
  .settings(
    commonSettings,
    name           := "symphony-core",
    publish / skip := false,
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.core
  )

lazy val server = (project in file("modules/server"))
  .dependsOn(core)
  .settings(
    commonSettings,
    name           := "symphony-server",
    publish / skip := false,
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.server
  )

lazy val annotations = (project in file("modules/annotations"))
  .settings(
    commonSettings,
    name           := "symphony-annotations",
    publish / skip := false,
    commands ++= Commands.value
  )

lazy val `java-apt` = (project in file("modules/java-apt"))
  .dependsOn(core)
  .settings(
    commonSettings,
    name           := "symphony-java-apt",
    publish / skip := false,
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.apt
  )

lazy val `java-apt-tests` = (project in file("modules/java-apt-tests"))
  .dependsOn(core % "compile->compile;test->test", `java-apt`)
  .settings(
    commonSettings,
    publish / skip := true,
    name           := "symphony-java-apt-tests",
    commands ++= Commands.value,
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
    publish / skip := true,
    commonSettings,
    compileOrder   := CompileOrder.JavaThenScala,
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
  .dependsOn(core, `java-apt`)
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr" %% "caliban"              % "2.5.1",
      "org.apache.pekko"      %% "pekko-stream"         % `pekko-core_Version`,
      "org.parboiled"         %% "parboiled"            % `parboiled_Version`,
      "org.sangria-graphql"   %% "sangria"              % "4.1.0",
      "org.sangria-graphql"   %% "sangria-circe"        % "1.3.2",
      "io.circe"              %% "circe-parser"         % "0.14.6",
      "com.graphql-java"       % "graphql-java"         % "21.3",
      "javax.annotation"       % "javax.annotation-api" % "1.3.2"
    ),
    compileOrder := CompileOrder.JavaThenScala,
    commands ++= Commands.value,
    Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed",
    Compile / javacOptions ++= Seq(
      "-processor",
      "symphony.apt.SymphonyQLProcessor",
      "-s",
      ((Compile / crossTarget).value / "src_managed").getAbsolutePath,
      "-XprintRounds",
      "-Xlint:deprecation"
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
