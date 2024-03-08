# SymphonyQL

> A GraphQL implementation built with Apache Pekko.

[![CI][Badge-CI]][Link-CI] 
[![Badge-Codecov]][Link-Codecov] 
[![Badge-Snapshots]][Link-Snapshots]

[Badge-CI]: https://github.com/SymphonyQL/SymphonyQL/actions/workflows/ScalaCI.yml/badge.svg
[Link-CI]: https://github.com/SymphonyQL/SymphonyQL/actions
[Badge-Codecov]: https://codecov.io/gh/SymphonyQL/SymphonyQL/graph/badge.svg?token=00GZ559DH7
[Link-Codecov]: https://codecov.io/gh/SymphonyQL/SymphonyQL
[Badge-Snapshots]: https://img.shields.io/nexus/s/io.github.jxnu-liguobin/symphony-core_3?server=https%3A%2F%2Foss.sonatype.org
[Link-Snapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/jxnu-liguobin/symphony-core_3/


## Documentation

[SymphonyQL homepage](https://SymphonyQL.github.io/SymphonyQL)

## Highlights

- support for Java 21: record classes, sealed interface.
- minimal dependencies, no adapter required.
- native support for [Apache Pekko](https://github.com/apache/incubator-pekko), including Java and Scala.
- minimal amount of boilerplate: no need to manually define a schema for every type in your API.

## Quickstart

[Quickstart Java](https://symphonyql.github.io/SymphonyQL/docs/quickstart-java)

[Quickstart Scala](https://symphonyql.github.io/SymphonyQL/docs/quickstart-scala)

## Inspire By 

1. [caliban](https://github.com/ghostdogpr/caliban)
2. [graphql-java](https://github.com/graphql-java/graphql-java)

The design of this library references caliban and graphql-java, and a large number of ADTs and Utils have been copied from caliban.
