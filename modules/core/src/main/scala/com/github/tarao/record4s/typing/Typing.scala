package com.github.tarao.record4s
package typing

/** Base trait typing given instances.
  *
  * Typing given instances infers result type as `Out`. If it is impossible to
  * infer the type then, `Out` is `Nothing` and `Msg` is a string literal type
  * of a message describing violation of the typing rule. `Out` is `Nothing` if
  * the type is successfully inferred.
  *
  * To show the typing failure, use `withPotentialTypingError { ... }`.
  */
trait MaybeError {
  type Out
  type Msg <: String
}

private inline def showTypingError(using err: typing.MaybeError): Unit = {
  import scala.compiletime.{constValue, erasedValue, error}

  inline erasedValue[err.Out] match {
    case _: Nothing => error(constValue[err.Msg])
    case _          => // no error
  }
}

inline def withPotentialTypingError[T](
  inline block: => T,
)(using err: typing.MaybeError): T = {
  showTypingError
  block
}

// A dummy class to carry type information of `T`.  Without this context bound, `Concat`
// somehow drops tag types in `T`.  It can be removed if all tests pass since it does
// nothing semantically.
final class Context[T] private ()
object Context {
  private val instance = new Context[Nothing]

  given [T]: Context[T] = instance.asInstanceOf[Context[T]]
}
