record4s
===========================================================================================
[![Build status](https://img.shields.io/github/actions/workflow/status/tarao/record4s/ci.yml)](https://github.com/tarao/record4s/actions/workflows/ci.yml)
[![Coverage status](https://codecov.io/gh/tarao/record4s/graph/badge.svg?token=U9309O1VNK)](https://codecov.io/gh/tarao/record4s)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.tarao/record4s_3.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.tarao/record4s_3)

record4s proviedes extensible records for Scala.

Getting Started
---------------

record4s is available for Scala 3 on [Scala.js][] and [Scala Native][], as well as the
standard JVM runtime.  It is published to [Maven Central][].

Add the following dependency to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "com.github.tarao" %% "record4s" % "@VERSION@"
),
```

An easier way to try out record4s is to run `sbt console` in the record4s repository.

```
> git clone https://github.com/tarao/record4s.git
> cd record4s
> sbt console
```

```scala
scala> val r = %(name = "tarao", age = 3)
val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
```

[Maven Central]: https://search.maven.org/
[Scala.js]: https://www.scala-js.org/
[Scala Native]: https://www.scala-native.org/

License
-------

record4s is licensed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
