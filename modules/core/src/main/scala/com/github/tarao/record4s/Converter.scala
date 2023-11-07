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

import scala.deriving.Mirror

trait Converter[From, To] {
  def apply(record: From): To
}

object Converter {

  /** Converter instance from a record to a product
    *
    * Target product type `P` must provide `Mirror.Of[P]`.
    *
    * @example
    *   ```
    *   case class Person(name: String, age: Int)
    *   val r = %(name = "tarao", age = 3)
    *   r.to[Person]
    *   // val res0: Person = Person(tarao,3)
    *   ```
    */
  inline given [R <: %, P <: Product](using
    m: Mirror.ProductOf[P],
    r1: RecordLike[P],
    s: typing.Record.Select[R, r1.ElemLabels],
    r2: RecordLike[s.Out],
    ev: r2.ElemTypes <:< m.MirroredElemTypes,
  ): Converter[R, P] =
    new Converter {
      def apply(record: R): P = {
        val s = Selector.of[r1.ElemLabels]
        m.fromTuple(ev(record(s).values))
      }
    }

  inline given [R <: %, T <: NonEmptyTuple](using
    r: RecordLike[R],
    ev: r.TupledFieldTypes =:= T,
  ): Converter[R, T] = new Converter {
    def apply(record: R): T = ev(record.toTuple)
  }
}
