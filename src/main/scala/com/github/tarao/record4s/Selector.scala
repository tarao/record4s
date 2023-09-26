package com.github.tarao.record4s

import scala.compiletime.requireConst
import scala.language.dynamics

class Selector[T <: Tuple] extends Dynamic {
  import Selector.:*

  inline def applyDynamic[
    S1 <: Singleton & String,
    S2 <: Singleton & String,
  ](inline label: S1)(inline rename: S2): T :* (S1, S2) = {
    requireConst(label)
    requireConst(rename)
    new Selector
  }

  inline def applyDynamicNamed[
    S1 <: Singleton & String,
    S2 <: Singleton & String,
  ](inline label: S1)(inline arg: ("rename", S2)): T :* (S1, S2) = {
    requireConst(label)
    requireConst(arg._2)
    new Selector
  }

  inline def selectDynamic[
    S <: Singleton & String,
  ](inline label: S): T :* (S, S) = {
    requireConst(label)
    new Selector
  }
}

object Selector {
  type :*[T1 <: Tuple, T2] = Selector[Tuple.Concat[T1, T2 *: EmptyTuple]]

  def of[T <: Tuple]: Selector[T] = new Selector
}

class Unselector[T <: Tuple] extends Dynamic {
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
