/*
 * Copyright 2023 record4s authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
      ${ ArrayRecordMacros.derivedTypingLookupImpl }
  }
}
