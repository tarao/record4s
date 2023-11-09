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

import scala.compiletime.requireConst
import scala.language.dynamics

import typing.Record.Select

final class Selector[T <: Tuple](val labels: Seq[String] = Seq.empty)
    extends Dynamic {
  import Selector.:*

  inline def applyDynamic[
    S1 <: Singleton & String,
    S2 <: Singleton & String,
  ](inline label: S1)(inline rename: S2): T :* (S1, S2) = {
    requireConst(label)
    requireConst(rename)
    new Selector(labels :+ label)
  }

  inline def applyDynamicNamed[
    S1 <: Singleton & String,
    S2 <: Singleton & String,
  ](inline label: S1)(inline arg: ("rename", S2)): T :* (S1, S2) = {
    requireConst(label)
    requireConst(arg._2)
    new Selector(labels :+ label)
  }

  inline def selectDynamic[
    S <: Singleton & String,
  ](inline label: S): T :* (S, S) = {
    requireConst(label)
    new Selector(labels :+ label)
  }
}

object Selector {
  type :*[T1 <: Tuple, T2] = Selector[Tuple.Concat[T1, T2 *: EmptyTuple]]

  def of[T <: Tuple]: Selector[T] = new Selector

  extension [T <: Tuple](s: Selector[T]) {
    // This should be `inline def` with `typing.potentialTypingError` but it seems that
    // inlining doesn't work in `unapply`.
    def unapply[R: RecordLike](record: R)(using
      t: Select[R, T],
      r: RecordLike[t.Out],
    ): r.ElemTypes = {
      val m = summon[RecordLike[R]].iterableOf(record).toMap
      s.labels
        .foldRight(EmptyTuple: Tuple) { (label, values) =>
          m(label) *: values
        }
        .asInstanceOf[r.ElemTypes]
    }
  }
}

final class Unselector[T <: Tuple] extends Dynamic {
  import Unselector.:*

  inline def selectDynamic[
    S <: Singleton & String,
  ](inline label: S): T :* S = {
    requireConst(label)
    new Unselector
  }
}

object Unselector {
  type :*[T1 <: Tuple, T2] = Unselector[Tuple.Concat[T1, T2 *: EmptyTuple]]
}

val select = new Selector[EmptyTuple]

val unselect = new Unselector[EmptyTuple]
