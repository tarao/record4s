Scala.js Support
================

Basically, record4s works fine in Scala.js because it is mostly written in pure Scala
code.  Sometimes you may wish to use records with native JavaScript code and that will
require some additional mechanism.

Converting a Record from / to a Native JavaScript Object
--------------------------------------------------------

You can use `fromJS` and `toJS` to convert a record from/to a native JS object.

```scala
import com.github.tarao.record4s.%
import scala.scalajs.js

val obj: js.Any = js.Dynamic.literal(name = "tarao", age = 3)

// convert obj to a record
val r = Record.fromJS[% { val name: String; val age: Int }](obj)
// val r: %{val name: String; val age: Int} = %(name = tarao, age = 3)

// convert the record again to a JS object
val j = r.toJS
// val j: js.Any = [object Object]
```

Converting a Record from / to a JSON string
-------------------------------------------

Methods `fromJSON` and `toJSON` convert a record from/to JSON string internally using
`JSON.parse` and `JSON.stringify`.

```scala
import com.github.tarao.record4s.%

val json = """{"name":"tarao","age":3}"""

// convert a JSON string to a record
val r = Record.fromJSON[% { val name: String; val age: Int }](json)
// val r: %{val name: String; val age: Int} = %(name = tarao, age = 3)

// convert the record again to a JSON string
val j = r.toJSON
// val j: String = {"name":"tarao","age":3}
```

Converting Nested Types
-----------------------

All these conversions described above support nested types.

```scala
val r = %(
  name = "tarao",
  age  = 3,
  email = %(
    local  = "tarao",
    domain = "example.com",
  ),
)
val json = r.toJSON
// val json: String = {"name":"tarao","age":3,"email":{"local":"tarao","domain":"example.com"}}
```

It is also possible to convert a type in which a record type appears as a nested type.  In this case you have to use [native-converter][] directly.

```scala
import org.getshaka.nativeconverter.fromJson

val json = """[{"name":"tarao","age":3}]"""
val seq = json.fromJson[Seq[%{ val name: String; val age: Int }]]
// val seq: Seq[%{ val name: String; val age: Int }] = List(%(name = tarao, age = 3))
```

```scala
import org.getshaka.nativeconverter.NativeConverter.SeqConv

val s = Seq(
  %(name = "tarao", age = 3),
  %(name = "ikura", age = 1),
)
// val s: Seq[%{val name: String; val age: Int}] = List(%(name = tarao, age = 3), %(name = ikura, age = 1))
val json = s.toJson
// val json: String = [{"name":"tarao","age":3},{"name":"ikura","age":1}]
```

If you have your own class, you have to provide a given instance of `NativeConverter[_]`
for that class.  See the documentation of [native-converter][] for the detail.

[native-converter]: https://github.com/getshaka-org/native-converter
