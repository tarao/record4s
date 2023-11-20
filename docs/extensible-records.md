Extensible Records
==================

Record Construction
-------------------

In record4s, `%` is used for a signature of extensible records.  First, import `%` to use
them.

```scala mdoc
import com.github.tarao.record4s.%
```

Then use `%` as a constructor to instantiate a record.

```scala mdoc:mline
val person = %(name = "tarao", age = 3)
```

Field Access
------------

You can access to fields in records in a type-safe manner.

```scala mdoc:mline
person.name

person.age
```

Accessing an undefined field is statically rejected.

```scala mdoc:fail
person.email
```

Extending Records
-----------------

You can extend records by `+` or `updated`.

```scala mdoc:mline
val personWithEmail = person + (email = "tarao@example.com")
```

It is possible to extend a record with multiple fields.

```scala mdoc:mline
person + (email = "tarao@example.com", occupation = "engineer")
```

Record Concatenation
--------------------

You can concatenate two records by `++` or `concat`.

```scala mdoc:mline
val email = %(email = "tarao@example.com")

person ++ email
```

Field Update
------------

If you extend a record by existing field label, then the field value is overridden.  The
type of the field may differ from the existing one.

```scala mdoc:mline
person + (age = person.age + 1)

personWithEmail + (email = %(user = "tarao", domain = "example.com"))
```

This also applies to concatenation.  The semantics is that "the latter one wins" like
duplicate keys in @:api(scala.collection.immutable.Map) construction or concatenation.
