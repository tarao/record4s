package com.github.tarao.record4s

object Typing {
  type Aux[R, Out0 <: %] = Concat[%, R] { type Out = Out0 }

  final class Concat[R1, R2] {
    type Out <: %
  }

  object Concat {
    type Aux[R1, R2, Out0 <: %] = Concat[R1, R2] { type Out = Out0 }

    transparent inline given [R1: RecordLike, R2: RecordLike]: Concat[R1, R2] =
      ${ Macros.derivedTypingConcatImpl }
  }

  object Append {
    type Aux[R1, R2 <: Tuple, Out0 <: %] = Concat[R1, R2] { type Out = Out0 }
  }
}
