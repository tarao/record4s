Defining Your Own Conversions
=============================

Converting records to/from @:api(scala.Product)s is described in [Conversions] section.
You can also define conversions for any other types.

As an example, we use the following `Person` class as the target of conversions.

```scala mdoc
class Person(val name: String, val age: Int) {
  override def toString: String = s"Person(${name} (${age}))"
}
```

Making this class a case class is the easiest way to achieve the conversions but we will
define conversions by hand for exercise without making it a case class.

RecordLike
----------

Giving a @:api(com.github.tarao.record4s.RecordLike) instance for a type enables the type
to be converted to a record.  `RecordLike` consists of type members and a method to
retrieve fields as an iterable.

- Type members
    - `FieldTypes`
        - Type of label-value pairs as a tuple type or a structural type
    - `ElemLabels`
        - Type of field labels as a tuple type
    - `ElemTypes`
        - Type of field values as a tuple type
- Method
    - `iterableOf(r: R): Iterable[(String, Any)]`
        - Retrieve label-value pairs from the conversion target of type `R`

A `RecordLike` instance for `Person` class can be given as the following.

```scala mdoc
import com.github.tarao.record4s.RecordLike

given RecordLike[Person] with
  type FieldTypes = (("name", String), ("age", Int))
  type ElemLabels = ("name", "age")
  type ElemTypes = (String, Int)

  def iterableOf(p: Person): Iterable[(String, Any)] =
    Seq(("name", p.name), ("age", p.age))
```

Then, `Record.from` works for a `Person` instance.

```scala mdoc:mline
import com.github.tarao.record4s.Record

Record.from(Person("tarao", 3))
```

It is also possible to use a `Person` as a right-hand-side argument of concatenation.

```scala mdoc:invisible
import com.github.tarao.record4s.%
```

```scala mdoc:mline
%(email = "tarao@example.com") ++ Person("tarao", 3)
```

More complicated example can be found in the source code of
@:source(com.github.tarao.record4s.RecordLike).

Converter
---------

Giving a @:api(com.github.tarao.record4s.Converter) instance for a type enables the type
to be converted from a record.  `Converter` is simply a wrapper of `From => To` function.
`Converter` for `Person` class can be given as the following.

```scala mdoc
import com.github.tarao.record4s.Converter

type PersonRecord = % { val name: String; val age: Int }

given [R <: PersonRecord]: Converter[R, Person] =
  Converter((record: R) => Person(record.name, record.age))
```

Then, `to[Person]` on a record converts the record to a `Person`.

```scala mdoc:mline
%(name = "tarao", age = 3).to[Person]
```
