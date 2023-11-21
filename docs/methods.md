Methods on Records
=================

```scala mdoc:invisible
import com.github.tarao.record4s.%
```

Extension Methods
-----------------

You can define methods on records by defining [extension methods] in a usual manner.

```scala mdoc:mline
object Extension {
  extension (p: % { val name: String; val age: Int }) {
    def firstName: String = p.name.split(" ").head
  }
}
```

```scala mdoc:mline
import Extension.firstName

val person = %(name = "tarao fuguta", age = 3)

person.firstName
```

As you see, you need to explicitly `import` the method in this case.

[extension methods]: https://docs.scala-lang.org/scala3/book/ca-extension-methods.html

Extension Methods with Tags
---------------------------

Records can be tagged and it enables extension methods to be found automatically.

```scala mdoc:invisible:reset
import com.github.tarao.record4s.%
```

```scala mdoc:mline
import com.github.tarao.record4s.Tag

trait Person
object Person {
  extension (p: % { val name: String; val age: Int } & Tag[Person]) {
    def firstName: String = p.name.split(" ").head
  }
}
```

```scala mdoc:mline
val person0 = %(name = "tarao fuguta", age = 3)

val person = person0.tag[Person]

person.firstName
```

The mechanism is that the type of `person` is an intersection type with `Tag[Person]`,
which adds extension methods in `object Person` to the target of implicit search at the
method invocation on `person`.  Without the tag, the method invocation statically fails.

```scala mdoc:fail
person0.firstName
```

Methods on Generic Record Types
-------------------------------

Extension methods can be generic ⸺ i.e., you can specify the least fields that are
necessary for the methods.

```scala mdoc:invisible:reset
import com.github.tarao.record4s.%
import com.github.tarao.record4s.Tag
```

```scala mdoc:mline
trait Person
object Person {
  extension [R <: % { val name: String }](p: R & Tag[Person]) {
    def firstName: String = p.name.split(" ").head
  }
}
```

```scala mdoc:mline
val person = %(name = "tarao fuguta", age = 3).tag[Person]
person.firstName

val personWithEmail = person + (email = "tarao@example.com")
personWithEmail.firstName
```
