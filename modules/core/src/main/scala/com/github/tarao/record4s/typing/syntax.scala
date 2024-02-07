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

object syntax {
  type :=[Out0, A] = A match {
    case Record.Concat[r1, r2]      => Record.Concat.Aux[r1, r2, Out0]
    case Record.Lookup[r, l]        => Record.Lookup.Aux[r, l, Out0]
    case ArrayRecord.Concat[r1, r2] => ArrayRecord.Concat.Aux[r1, r2, Out0]
    case ArrayRecord.Lookup[r, l] =>
      ArrayRecord.Lookup[r, l] { type Out = Out0 }
  }

  type ++[R1, R2] = R1 match {
    case % => Record.Concat[R1, R2]
    case _ => ArrayRecord.Concat[R1, R2]
  }

  type in[L, R] = R match {
    case % => Record.Lookup[R, L]
    case _ => ArrayRecord.Lookup[R, L]
  }

  type by[L, I] = L match {
    case o := (l in r) => ArrayRecord.Lookup.Aux[r, l, I, o]
  }
}
