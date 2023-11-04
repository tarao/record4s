package com.github.tarao.record4s

import scala.compiletime.{constValueOpt, erasedValue, error, summonInline}
import scala.deriving.Mirror
import scala.util.NotGiven

trait RecordLike[R] {
  type FieldTypes
  type ElemLabels <: Tuple
  type ElemTypes <: Tuple
  type Tags = Any
  type TupledFieldTypes = Tuple.Zip[ElemLabels, ElemTypes]

  def iterableOf(r: R): Iterable[(String, Any)]

  inline def orderedIterableOf(r: R): Iterable[(String, Any)] =
    tidiedIterableOf(r, true)

  inline def tidiedIterableOf(r: R): Iterable[(String, Any)] =
    tidiedIterableOf(r, false)

  inline def tidiedIterableOf(
    r: R,
    ordered: Boolean,
  ): Iterable[(String, Any)] = {
    val labels = elemLabels
    val it = iterableOf(r)

    if (ordered || labels.size != it.size) {
      val m = it.toMap
      labels.map(l => (l, m(l)))
    } else {
      it
    }
  }

  inline def elemLabels: Seq[String] = RecordLike.seqOfLabels[ElemLabels]
}

object RecordLike {
  private[record4s] inline def seqOfLabels[T <: Tuple]: Seq[String] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Seq.empty
      case _: (t *: ts) =>
        val stringOf = summonInline[t <:< String]
        inline constValueOpt[t] match {
          case Some(value) =>
            stringOf(value) +: seqOfLabels[ts]
          case None =>
            error(
              "Types of field labels must be literal string types.\n" + "Found:    " + Macros
                .typeNameOf[t] + "\nRequired: (a literal string type)",
            )
        }
    }

  final class OfProduct[
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
    nonRecord: NotGiven[P <:< Record],
  ): OfProduct[P, m.MirroredElemLabels, m.MirroredElemTypes] =
    new OfProduct

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

  final class OfTuple[T <: Tuple] extends RecordLike[T] {
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

  given ofTuple[T <: Tuple]: OfTuple[T] = new OfTuple
}
