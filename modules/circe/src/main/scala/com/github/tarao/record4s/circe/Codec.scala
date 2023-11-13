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
package circe

import io.circe.{Decoder, Encoder, HCursor, Json}

object Codec {
  inline given encoder[R <: %, RR <: ProductRecord](using
    ar: typing.ArrayRecord.Aux[R, RR],
    enc: Encoder[RR],
  ): Encoder[R] = new Encoder[R] {
    final def apply(record: R): Json = enc(ArrayRecord.from(record))
  }

  inline given decoder[R <: %](using
    r: RecordLike[R],
    dec: Decoder[ArrayRecord[r.TupledFieldTypes]],
    c: typing.Record.Concat[%, ArrayRecord[r.TupledFieldTypes]],
    ev: c.Out =:= R,
  ): Decoder[R] = new Decoder[R] {
    final def apply(c: HCursor): Decoder.Result[R] =
      dec(c).map(ar => ev(ar.toRecord))
  }
}
