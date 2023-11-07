/*
 * Copyright (c) 2023 record4s authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.tarao.record4s
package typing

import scala.annotation.implicitNotFound

object ArrayRecord {
  type Aux[R, Out0 <: ProductRecord] = Concat[EmptyTuple, R] {
    type Out = Out0
  }

  final class Concat[R1, R2] private extends MaybeError {
    type NeedDedup <: Boolean
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

  @implicitNotFound("Value '${Label}' is not a member of ${R}")
  final class Lookup[R, Label] private () {
    type Out
    type Index <: Int
  }

  object Lookup {
    private[record4s] val instance = new Lookup[Nothing, Nothing]

    type Aux[R, Label, Index0 <: Int, Out0] = Lookup[R, Label] {
      type Out = Out0
      type Index = Index0
    }

    transparent inline given [R, L <: String]: Lookup[R, L] =
      ${ ArrayRecordMacros.derivedTypingLookupImpl[R, L] }
  }
}
