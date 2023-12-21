Integration with uPickle
========================

Records can be converted to/from JSON by using [uPickle][].

[uPickle]: https://com-lihaoyi.github.io/upickle/

Installation
------------

You need an additional module to use records integrated with [uPickle][].

```scala
libraryDependencies ++= Seq(
  "com.github.tarao" %% "record4s-upickle" % "@VERSION@"
),
```

From / To JSON
--------------

Importing `Record.readWriter` enables [uPickle][] to decode/encode records in the ordinary way.

```scala mdoc:mline
import com.github.tarao.record4s.%
import com.github.tarao.record4s.upickle.Record.readWriter
import upickle.default.{read, write}

type Person = % { val name: String; val age: Int }

val json = """{"name":"tarao","age":3}"""

// Read from JSON
val r = read[Person](json)

// Write it back
write(r)
```
