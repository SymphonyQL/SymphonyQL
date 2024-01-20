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

lazy val `symphony` = (project in file("."))
  .aggregate(
    `symphony-core`,
    `symphony-parser`
  )
  .settings(
    publish / skip := true,
    commonSettings,
    commands ++= Commands.value
  )

lazy val `symphony-parser` = (project in file("symphony-parser"))
  .settings(
    publish / skip := false,
    commonSettings,
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.parser.value
  )

lazy val `symphony-core` = (project in file("symphony-core"))
  .settings(
    publish / skip := false,
    commonSettings,
    commands ++= Commands.value,
    libraryDependencies ++= Dependencies.Deps.core.value
  )
