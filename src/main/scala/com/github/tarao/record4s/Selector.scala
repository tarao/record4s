package com.github.tarao.record4s

import scala.compiletime.requireConst
import scala.language.dynamics

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
    def unapply[R <: %](record: R)(using
      t: typing.Select[R, T],
      r: RecordLike[t.Out],
    ): r.ElemTypes =
      s.labels
        .foldRight(EmptyTuple: Tuple) { (label, values) =>
          record.__data(label) *: values
        }
        .asInstanceOf[r.ElemTypes]
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
