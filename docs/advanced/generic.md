Generic Records
===============

Generic Field Lookup
--------------------

It is possible to retrieve a field value of an arbitrary type from records by using
`Record.lookup`.  To express the type of the field value, which is unknown until the
record type is specified, you can use `typing.Record.Lookup`.  In the following example,
`getValue` method retrieves a field value named `value` from records of any type.

```scala mdoc:mline
import com.github.tarao.record4s.{%, Record}
import com.github.tarao.record4s.typing.Record.Lookup

def getValue[R <: %, V](record: R)(using
  Lookup.Aux[R, "value", V],
): V = Record.lookup(record, "value")

val r1 = %(value = "tarao")
val r2 = %(value = 3)

getValue(r1)

getValue(r2)
```

Note that `(using Lookup.Aux[R, L, V]): V` is a shorthand for `(using l: Lookup[R, L]):
l.Out`.

Of course, it doesn't compile for a record without `value` field.

```scala mdoc:fail
val r3 = %(name = "tarao")

getValue(r3)
```

Extending Generic Records with Concrete Fields
----------------------------------------------

To define a method to extend a generic record with some concrete field, we need to somehow
calculate the extended result record type.  This can be done by using `typing.Record.Append`.

For example, `withEmail` method, which expects a domain name and returns a record extended
by `email` field of E-mail address, whose local part is filled by the first segment of
`name` field of the original record, can be defined as the following.

```scala mdoc:mline
import com.github.tarao.record4s.Tag
import com.github.tarao.record4s.typing.Record.Append

trait Person
object Person {
  extension [R <: % { val name: String }](p: R & Tag[Person]) {
    def firstName: String = p.name.split(" ").head

    def withEmail[RR <: %](
      domain: String,
      localPart: String = p.firstName,
    )(using
      Append.Aux[R & Tag[Person], ("email", String) *: EmptyTuple, RR],
    ): RR = p + (email = s"${localPart}@${domain}")
  }
}
```

The method can be called on an arbitrary record with `name` field and `Person` tag.

```scala mdoc:mline
val person = %(name = "tarao fuguta", age = 3)
  .tag[Person]
  .withEmail("example.com")
```

There is also `typing.Record.Concat` to calculate concatenation of two record types.  The
above example can be rewritten with `Concat` as the following.

```scala mdoc:nest:invisible
```

```scala mdoc:mline
import com.github.tarao.record4s.typing.Record.Concat

trait Person
object Person {
  extension [R <: % { val name: String }](p: R & Tag[Person]) {
    def firstName: String = p.name.split(" ").head

    def withEmail[RR <: %](
      domain: String,
      localPart: String = p.firstName,
    )(using
      Concat.Aux[R & Tag[Person], % { val email: String }, RR],
    ): RR = p ++ %(email = s"${localPart}@${domain}")
  }
}
```

Concatenating Two Generic Records
---------------------------------

You may think that you can define a method to concatenate two generic records by using
`Concat` but it doesn't work in a simple way.

```scala mdoc:fail
def concat[R1 <: %, R2 <: %, RR <: %](r1: R1, r2: R2)(using
  Concat.Aux[R1, R2, RR],
): RR = r1 ++ r2
```

The problem is that the right-hand-side argument type of `++` is restricted to be a
concrete type for type safety.  In this case, defining an inline method makes it work.

```scala mdoc:mline
inline def concat[R1 <: %, R2 <: %, RR <: %](r1: R1, r2: R2)(using
  Concat.Aux[R1, R2, RR],
): RR = r1 ++ r2

concat(%(name = "tarao"), %(age = 3))
```
