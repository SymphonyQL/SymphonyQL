---
title: Installing SymphonyQL
sidebar_label: Installing
custom_edit_url: https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/installation.md
---

SymphonyQL only supports Scala3 and Java21.

## Installation using SBT

If you are building with sbt, add the following to your `project/plugins.sbt`:
```scala
libraryDependencies ++= Seq(
  "io.github.symphonyql" % "symphony-core" % "<version>",
  // a default http-server provided by pekko-http
  "io.github.symphonyql" % "symphony-server" % "<version>"
)
```

If you want to develop SymphonyQL application using Java, you also need to add `symphony-java-apt` and some settings.

Here is a complete configuration:
```scala
Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed"
libraryDependencies ++= Seq(
  "io.github.symphonyql" % "symphony-core" % "<version>",
  "io.github.symphonyql" % "symphony-server" % "<version>",
  "io.github.symphonyql" % "symphony-java-apt" % "<version>",
  "javax.annotation" % "javax.annotation-api" % "<version>"
)
Compile / javacOptions ++= Seq(
  "-processor",
  "symphony.apt.SymphonyQLProcessor",
  "-s",
  ((Compile / crossTarget).value / "src_managed").getAbsolutePath
)
```

APT and this setting are unique to Java and are not required in Scala.

## Installation using Maven

If you are building with maven, add the following to your `pom.xml`:
```xml

```
