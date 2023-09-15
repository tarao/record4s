package com.github.tarao.record4s

import scala.deriving.Mirror

trait RecordLike[R] {
  type FieldTypes

  def iterableOf(r: R): Iterable[(String, Any)]
}

object RecordLike {
  trait RecordLikeProductMirror[
    P <: Product,
    ElemLabels <: Tuple,
    ElemTypes <: Tuple,
  ] extends RecordLike[P] {
    type FieldTypes = Tuple.Zip[ElemLabels, ElemTypes]
  }

  given ofProduct[P <: Product](using
    m: Mirror.Of[P],
  ): RecordLikeProductMirror[P, m.MirroredElemLabels, m.MirroredElemTypes] =
    new RecordLikeProductMirror {
      def iterableOf(p: P): Iterable[(String, Any)] =
        p.productElementNames.zip(p.productIterator).toSeq
    }
}
