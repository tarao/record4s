Integration with circe
======================

Records can be converted to/from JSON by using [circe][].

[circe]: https://circe.github.io/circe/

Installation
------------

You need an additional module to use records integrated with [circe][].

```scala
libraryDependencies ++= Seq(
  "com.github.tarao" %% "record4s-circe" % "@VERSION@"
),
```

To JSON
-------

Importing `Codec.encoder` enables [circe][] to encode records in the ordinary way.

```scala mdoc:mline
import com.github.tarao.record4s.%
import com.github.tarao.record4s.circe.Codec.encoder
import io.circe.generic.auto.*
import io.circe.syntax.*

val r = %(name = "tarao", age = 3)
val json = r.asJson.noSpaces
```

From JSON
---------

Importing `Codec.encoder` enables [circe][] to decode records in the ordinary way.

```scala mdoc:reset:invisible
```

```scala mdoc:mline
import com.github.tarao.record4s.%
import com.github.tarao.record4s.circe.Codec.decoder
import io.circe.generic.auto.*
import io.circe.parser.parse

val json = """{"name":"tarao","age":3}"""
val r =
  for {
    jsonObj <- parse(json)
  } yield jsonObj.as[% { val name: String; val age: Int }]
```

ArrayRecord
-----------

`ArrayRecord` requires no additional module since it is a @:api(scala.Product), whose
conversions already supported by [circe][] itself.

```scala mdoc:reset:invisible
```

```scala mdoc:mline
import com.github.tarao.record4s.ArrayRecord
import io.circe.generic.auto.*
import io.circe.syntax.*

val r = ArrayRecord(name = "tarao", age = 3)
val json = r.asJson.noSpaces
```

```scala mdoc:reset:invisible
```

```scala mdoc:mline
import com.github.tarao.record4s.ArrayRecord
import io.circe.generic.auto.*
import io.circe.parser.parse

val json = """{"name":"tarao","age":3}"""
val r =
  for {
    jsonObj <- parse(json)
  } yield jsonObj.as[ArrayRecord[(("name", String), ("age", Int))]]
```
