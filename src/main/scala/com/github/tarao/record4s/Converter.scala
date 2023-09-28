package com.github.tarao.record4s

import scala.deriving.Mirror

trait Converter[From, To] {
  def apply(record: From): To
}

object Converter {
  import typing.withPotentialTypingError

  /** Converter instance from a record to a product
    *
    * Target product type `P` must provide `Mirror.Of[P]`.
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val r = %(name = "tarao", age = 3)
    * r.to[Person]
    * // val res0: Person = Person(tarao,3)
    *   }}}
    */
  inline given [R <: %, P <: Product](using
    m: Mirror.ProductOf[P],
    r1: RecordLike[P],
    s: typing.Select[R, r1.ElemLabels],
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
    ev: Tuple.Zip[r.ElemLabels, r.ElemTypes] =:= T,
  ): Converter[R, T] = new Converter {
    def apply(record: R): T = ev(record.toTuple)
  }
}
