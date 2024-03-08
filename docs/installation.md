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
  "io.github.jxnu-liguobin" %% "symphony-core" % "<version>",
  // a default http-server provided by pekko-http
  "io.github.jxnu-liguobin" %% "symphony-server" % "<version>"
)
```

If you want to develop SymphonyQL application using Java, you also need to add `symphony-java-apt` and some settings.

Here is a complete configuration:
> Temporary path
```scala
Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed"
libraryDependencies ++= Seq(
  "io.github.jxnu-liguobin" %% "symphony-core" % "<version>",
  "io.github.jxnu-liguobin" %% "symphony-server" % "<version>",
  "io.github.jxnu-liguobin" %% "symphony-java-apt" % "<version>",
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
<properties>
    <symphonyql.version>version</symphonyql.version>
    <javax.annotation.api.version>1.3.2</javax.annotation.api.version>
    <maven.compiler.plugin.version>3.7.0</maven.compiler.plugin.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.jxnu-liguobin</groupId>
        <artifactId>symphony-core_3</artifactId>
        <version>${symphonyql.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.jxnu-liguobin</groupId>
        <artifactId>symphony-server_3</artifactId>
        <version>${symphonyql.version}</version>
    </dependency>
    <dependency>
        <groupId>javax.annotation</groupId>
        <artifactId>javax.annotation-api</artifactId>
        <version>${javax.annotation.api.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>${maven.compiler.plugin.version}</version>
            <configuration>
                <forceJavacCompilerUse>true</forceJavacCompilerUse>
                <annotationProcessorPaths>
                    <annotationProcessorPath>
                        <groupId>io.github.jxnu-liguobin</groupId>
                        <artifactId>symphony-java-apt</artifactId>
                        <version>${symphonyql.version}</version>
                    </annotationProcessorPath>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```
