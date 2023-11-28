Conversions
===========

```scala mdoc:invisible
import com.github.tarao.record4s.%
```

From Something
--------------

`Record.from` creates a record from a @:api(scala.Product), typically a case class instance.

```scala mdoc:mline
import com.github.tarao.record4s.Record

case class Person(name: String, age: Int)

Record.from(Person("tarao", 3))
```

Anything other than @:api(scala.Product) can be converted to a record if [RecordLike]
 instance is given.

To Something
------------

You can use `to` method to convert a record to a @:api(scala.Product).

```scala mdoc:mline
%(name = "ikura", age = 1).to[Person]
```

A record can be converted to anything other than @:api(scala.Product) if [Converter]
instance is given.

From / To JSON
--------------

Records can be converted from/to JSON using [circe][].  See [Integration with circe] for the
detail.

[circe]: https://circe.github.io/circe/

Upcast
------

Records can implicitly be upcast since their types are represented as [structural types][]
in Scala 3.

```scala mdoc:mline
val person = %(name = "tarao", age = 3)

val named: % { val name: String } = person
```

Note that the runtime value still has statically hidden fields ⸺ `age` of `named` in the
above example.  This is because `named` points to the same object as `person`, where it
has `person.age`.

To drop the hidden fields, use `as` method.

```scala mdoc:mline
val named2 = person.as[% { val name: String }]
```

In this case, `as` makes a copy of `person` only with the visible fields.

[structural types]: https://docs.scala-lang.org/scala3/book/types-structural.html

Selecting / Unselecting / Reordering / Renaming
-----------------------------------------------

It is also possible to shrink a record to have selected fields.

```scala mdoc:invisible:reset
import com.github.tarao.record4s.%
```

```scala mdoc:mline
import com.github.tarao.record4s.select

val person = %(name = "tarao", age = 3, email = "tarao@example.com")

val contact = person(select.name.email)
```

Or to unselect some fields.

```scala mdoc:invisible:nest
```

```scala mdoc:mline
import com.github.tarao.record4s.unselect

val contact = person(unselect.age)
```

Field selection also supports reordering and renaming.

```scala mdoc:invisible:nest
```

```scala mdoc:mline
val account = person(
  select
    .email(rename = "login")
    .name(rename = "displayName"),
)
```

Note that the field order is semantically insignificant.  You can always reorder the
static type of the fields.

```scala mdoc:mline
person

val person2: % {
  val email: String
  val age: Int
  val name: String
} = person
```
