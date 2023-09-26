package com.github.tarao.record4s
package typing

type Aux[R, Out0 <: %] = Concat[%, R] { type Out = Out0 }

final class Concat[R1, R2] private {
  type Out <: %
}

object Concat {
  private[record4s] val instance = new Concat[Nothing, Nothing]

  type Aux[R1, R2, Out0 <: %] = Concat[R1, R2] { type Out = Out0 }

  transparent inline given [R1: RecordLike, R2: RecordLike]: Concat[R1, R2] =
    ${ Macros.derivedTypingConcatImpl }
}

type Append[R1, R2 <: Tuple] = Concat[R1, R2]

object Append {
  type Aux[R1, R2 <: Tuple, Out0 <: %] = Concat[R1, R2] { type Out = Out0 }
}

final class Select[R, S] private {
  type Out <: %
}

object Select {
  private[record4s] val instance = new Select[Nothing, Nothing]

  type Aux[R, S, Out0 <: %] = Select[R, S] { type Out = Out0 }

  transparent inline given [R: RecordLike, S <: Tuple]: Select[R, S] =
    ${ Macros.derivedTypingSelectImpl }
}
