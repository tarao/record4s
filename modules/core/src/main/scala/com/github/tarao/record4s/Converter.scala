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

import scala.deriving.Mirror

class Converter[From, To](f: From => To) {
  def apply(record: From): To = f(record)
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
  ): Converter[R, P] = new Converter((record: R) => {
    val s = Selector.of[r1.ElemLabels]
    m.fromTuple(ev(record(s).values))
  })

  inline given [R <: %, T <: NonEmptyTuple](using
    r: RecordLike[R],
    ev: r.TupledFieldTypes =:= T,
  ): Converter[R, T] = new Converter((record: R) => ev(record.toTuple))
}
