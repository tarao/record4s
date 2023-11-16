Pattern Matching
================

```scala mdoc:invisible
import com.github.tarao.record4s.%
```

Using `select`
--------------

Pattern matching is provided via `select`.

```scala mdoc:mline
import com.github.tarao.record4s.select

val person = %(name = "tarao", age = 3, email = "tarao@example.com")

person match {
  case select.name.age(name, age) =>
    println(s"${name} (${age})")
}
```

Nested pattern matching works of course.

```scala mdoc:mline
val pattern = "(.*)@(.*)".r

person match {
  case select.name.email(name, pattern(user, _)) =>
    val isSame = if (name == user) "the same" else "not the same"
    println(s"name and email user name are ${isSame}")
}
```

Named pattern matching
----------------------

Named pattern matching something like this is currently not yet supported.

```scala
person match {
  case %(name = name, age = age) => ???
}
```

We need language improvement discussed on [SIP-43][] to implement this kind of thing.

[SIP-43]: https://github.com/scala/improvement-proposals/pull/44
