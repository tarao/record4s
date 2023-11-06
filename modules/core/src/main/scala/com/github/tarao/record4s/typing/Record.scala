package com.github.tarao.record4s
package typing

import scala.annotation.implicitNotFound

object Record {
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

  @implicitNotFound("Value '${Label}' is not a member of ${R}")
  final class Lookup[R, Label] private () {
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
}
