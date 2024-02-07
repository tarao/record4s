Generic Records
===============

Generic Field Lookup
--------------------

It is possible to retrieve a field value of an arbitrary type from records by using
`typing.syntax`.  To express the type of the field value, which is unknown until the
record type is specified, you can use `typing.syntax.in`.  In the following example,
`getValue` method retrieves a field value named `value` from records of any type.

```scala mdoc:mline
import com.github.tarao.record4s.{%, Record}
import com.github.tarao.record4s.typing.syntax.{:=, in}

def getValue[R <: %, V](record: R)(using
  V := ("value" in R),
): V = Record.lookup(record, "value")

val r1 = %(value = "tarao")
val r2 = %(value = 3)

getValue(r1)

getValue(r2)
```

Of course, it doesn't compile for a record without `value` field.

```scala mdoc:fail
val r3 = %(name = "tarao")

getValue(r3)
```

Extending Generic Records with Concrete Fields
----------------------------------------------

To define a method to extend a generic record with some concrete field, we need to somehow
calculate the extended result record type.  This can be done by using `typing.syntax.++`.

For example, `withEmail` method, which expects a domain name and returns a record extended
by `email` field of E-mail address, whose local part is filled by the first segment of
`name` field of the original record, can be defined as the following.

```scala mdoc:mline
import com.github.tarao.record4s.Tag
import com.github.tarao.record4s.typing.syntax.++

trait Person
object Person {
  extension [R <: % { val name: String }](p: R & Tag[Person]) {
    def firstName: String = p.name.split(" ").head

    def withEmail[RR <: %](
      domain: String,
      localPart: String = p.firstName,
    )(using
      RR := (R & Tag[Person]) ++ % { val email: String },
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

It is also possible to calculate concatenation of two record types in the same way.  The
above example can be rewritten as the following.

```scala mdoc:nest:invisible
```

```scala mdoc:mline
trait Person
object Person {
  extension [R <: % { val name: String }](p: R & Tag[Person]) {
    def firstName: String = p.name.split(" ").head

    def withEmail[RR <: %](
      domain: String,
      localPart: String = p.firstName,
    )(using
      RR := (R & Tag[Person]) ++ % { val email: String },
    ): RR = p ++ %(email = s"${localPart}@${domain}")
  }
}
```

Concatenating Two Generic Records
---------------------------------

You may think that you can define a method to concatenate two generic records by using
`typing.syntax.++` but it doesn't work in a simple way.

```scala mdoc:fail
def concat[R1 <: %, R2 <: %, RR <: %](r1: R1, r2: R2)(using
  RR := R1 ++ R2,
): RR = r1 ++ r2
```

The problem is that the right-hand-side argument type of `++` is restricted to be a
concrete type for type safety.  In this case, defining an inline method makes it work.

```scala mdoc:mline
inline def concat[R1 <: %, R2 <: %, RR <: %](r1: R1, r2: R2)(using
  RR := R1 ++ R2,
): RR = r1 ++ r2

concat(%(name = "tarao"), %(age = 3))
```
