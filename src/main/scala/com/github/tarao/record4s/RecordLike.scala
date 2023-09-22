package com.github.tarao.record4s

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror

trait RecordLike[R] {
  type FieldTypes
  type ElemLabels <: Tuple

  def iterableOf(r: R): Iterable[(String, Any)]

  inline def tidiedIterableOf(r: R): Iterable[(String, Any)] = {
    val labels = RecordLike.setOfLabels[ElemLabels]
    iterableOf(r).filter { case (label, _) => labels.contains(label) }
  }
}

object RecordLike {
  private inline def setOfLabels[T <: Tuple]: Set[String] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Set.empty
      case _: (t *: ts) =>
        val stringOf = summonInline[t <:< String]
        val value = valueOf[t]
        setOfLabels[ts] + stringOf(value)
    }

  final class RecordLikeProductMirror[
    P <: Product,
    ElemLabels0 <: Tuple,
    FieldTypes0 <: Tuple,
  ] extends RecordLike[P] {
    type FieldTypes = Tuple.Zip[ElemLabels0, FieldTypes0]
    type ElemLabels = ElemLabels0

    def iterableOf(p: P): Iterable[(String, Any)] =
      p.productElementNames.zip(p.productIterator).toSeq
  }

  given ofProduct[P <: Product](using
    m: Mirror.Of[P],
  ): RecordLikeProductMirror[P, m.MirroredElemLabels, m.MirroredElemTypes] =
    new RecordLikeProductMirror

  type LabelsOf[T <: Tuple] <: Tuple = T match {
    case (l, _) *: tail => l *: LabelsOf[tail]
    case head *: tail   => LabelsOf[tail]
    case _              => EmptyTuple
  }

  final class RecordLikeTuple[T <: Tuple] extends RecordLike[T] {
    type FieldTypes = T
    type ElemLabels = LabelsOf[T]

    def iterableOf(tp: T): Iterable[(String, Any)] =
      tp.productIterator
        .collect { case (label: String, value) =>
          (label, value)
        }
        .toSeq
  }

  given ofTuple[T <: Tuple]: RecordLikeTuple[T] = new RecordLikeTuple
}
