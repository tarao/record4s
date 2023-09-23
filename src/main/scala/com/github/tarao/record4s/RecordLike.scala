package com.github.tarao.record4s

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror

trait RecordLike[R] {
  type FieldTypes
  type ElemLabels <: Tuple
  type ElemTypes <: Tuple

  def iterableOf(r: R): Iterable[(String, Any)]

  inline def tidiedIterableOf(r: R): Iterable[(String, Any)] = {
    val labels = elemLabels.toSet
    iterableOf(r).filter { case (label, _) => labels.contains(label) }
  }

  inline def elemLabels: Seq[String] = RecordLike.seqOfLabels[ElemLabels]
}

object RecordLike {
  private inline def seqOfLabels[T <: Tuple]: Seq[String] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Seq.empty
      case _: (t *: ts) =>
        val stringOf = summonInline[t <:< String]
        val value = valueOf[t]
        stringOf(value) +: seqOfLabels[ts]
    }

  final class ProductMirrorRecordLike[
    P <: Product,
    ElemLabels0 <: Tuple,
    ElemTypes0 <: Tuple,
  ] extends RecordLike[P] {
    type FieldTypes = Tuple.Zip[ElemLabels0, ElemTypes0]
    type ElemLabels = ElemLabels0
    type ElemTypes = ElemTypes0

    def iterableOf(p: P): Iterable[(String, Any)] =
      p.productElementNames.zip(p.productIterator).toSeq
  }

  given ofProduct[P <: Product](using
    m: Mirror.Of[P],
  ): ProductMirrorRecordLike[P, m.MirroredElemLabels, m.MirroredElemTypes] =
    new ProductMirrorRecordLike

  type LabelsOf[T <: Tuple] <: Tuple = T match {
    case (l, _) *: tail => l *: LabelsOf[tail]
    case head *: tail   => LabelsOf[tail]
    case _              => EmptyTuple
  }

  type TypesOf[T] <: Tuple = T match {
    case (_, t) *: tail => t *: TypesOf[tail]
    case head *: tail   => TypesOf[tail]
    case _              => EmptyTuple
  }

  final class RecordLikeTuple[T <: Tuple] extends RecordLike[T] {
    type FieldTypes = T
    type ElemLabels = LabelsOf[T]
    type ElemTypes = TypesOf[T]

    def iterableOf(tp: T): Iterable[(String, Any)] =
      tp.productIterator
        .collect { case (label: String, value) =>
          (label, value)
        }
        .toSeq
  }

  given ofTuple[T <: Tuple]: RecordLikeTuple[T] = new RecordLikeTuple
}
