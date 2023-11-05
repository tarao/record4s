package com.github.tarao.record4s
package typing

object ArrayRecord {
  type Aux[R, Out0 <: ProductRecord] = Concat[EmptyTuple, R] {
    type Out = Out0
  }

  final class Concat[R1, R2] private extends MaybeError {
    type Out <: ProductRecord
  }

  object Concat {
    private[record4s] val instance = new Concat[Nothing, Nothing]

    type Aux[R1, R2, Out0 <: ProductRecord] = Concat[R1, R2] { type Out = Out0 }

    transparent inline given [R1: Context, R2]: Concat[R1, R2] =
      ${ ArrayRecordMacros.derivedTypingConcatImpl }
  }

  type Append[R1, R2 <: Tuple] = Concat[R1, R2]

  object Append {
    type Aux[R1, R2 <: Tuple, Out0 <: ProductRecord] = Concat[R1, R2] {
      type Out = Out0
    }
  }
}
