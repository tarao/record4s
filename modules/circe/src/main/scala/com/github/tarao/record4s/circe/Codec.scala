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
