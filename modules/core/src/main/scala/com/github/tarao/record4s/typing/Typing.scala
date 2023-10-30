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

// A dummy class to carry type information of `T`.  Without this context bound, `Concat`
// somehow drops tag types in `T`.  It can be removed if all tests pass since it does
// nothing semantically.
final class Context[T] private ()
object Context {
  private val instance = new Context[Nothing]

  given [T]: Context[T] = instance.asInstanceOf[Context[T]]
}

type Aux[R, Out0 <: %] = Concat[%, R] { type Out = Out0 }

final class Concat[R1, R2] private extends MaybeError {
  type Out <: %
}

object Concat {
  private[record4s] val instance = new Concat[Nothing, Nothing]

  type Aux[R1, R2, Out0 <: %] = Concat[R1, R2] { type Out = Out0 }

  transparent inline given [R1: Context, R2]: Concat[R1, R2] =
    ${ Macros.derivedTypingConcatImpl }
}

type Append[R1, R2 <: Tuple] = Concat[R1, R2]

object Append {
  type Aux[R1, R2 <: Tuple, Out0 <: %] = Concat[R1, R2] { type Out = Out0 }
}

final class Lookup[R, Label] private extends MaybeError {
  type Out
}

object Lookup {
  private[record4s] val instance = new Lookup[Nothing, Nothing]

  type Aux[R, Label, Out0] = Lookup[R, Label] { type Out = Out0 }

  transparent inline given [R <: %, L <: String]: Lookup[R, L] =
    ${ Macros.derivedTypingLookupImpl }
}

final class Select[R, S] private extends MaybeError {
  type Out <: %
}

object Select {
  private[record4s] val instance = new Select[Nothing, Nothing]

  type Aux[R, S, Out0 <: %] = Select[R, S] { type Out = Out0 }

  transparent inline given [R: RecordLike, S <: Tuple]: Select[R, S] =
    ${ Macros.derivedTypingSelectImpl }
}

final class Unselect[R, U] private extends MaybeError {
  type Out <: %
}

object Unselect {
  private[record4s] val instance = new Unselect[Nothing, Nothing]

  type Aux[R, U, Out0 <: %] = Unselect[R, U] { type Out = Out0 }

  transparent inline given [R: RecordLike, S <: Tuple]: Unselect[R, S] =
    ${ Macros.derivedTypingUnselectImpl }
}

private inline def showTypingError(using err: typing.MaybeError): Unit = {
  import scala.compiletime.{erasedValue, constValue, error}

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
