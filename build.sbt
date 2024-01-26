import Dependencies.Versions.*

inThisBuild(
  List(
    scalaVersion           := scala3Version,
    organization           := "symphonyql.org",
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
    homepage               := Some(url("https://github.com/SymphonyQL")),
    licenses               := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
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
    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
      "-language:dynamics",
      "-explain",
      "unchecked",
      "-deprecation",
      "-feature"
    )
  )

lazy val root = (project in file("."))
  .aggregate(
    core,
    parser,
    server
  )
  .settings(
    publish / skip := true,
    commonSettings,
    commands ++= Commands.value
  )

lazy val parser = (project in file("modules/parser"))
  .settings(
    publish / skip := false,
    commonSettings,
    name := "symphony-parser",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.parser.value
  )

lazy val core = (project in file("modules/core"))
  .dependsOn(parser)
  .settings(
    publish / skip := false,
    commonSettings,
    name := "symphony-core",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.core.value
  )

lazy val server = (project in file("modules/server"))
  .dependsOn(parser)
  .settings(
    publish / skip := false,
    commonSettings,
    name := "symphony-server",
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.server.value
  )
