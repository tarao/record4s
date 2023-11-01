package com.github.tarao.record4s

import scala.deriving.Mirror
import scala.language.dynamics
import util.SeqOps.deduped

abstract class ProductRecord extends Record with Product {
  private[record4s] def __fields: IndexedSeq[(String, Any)]

  override def productPrefix: String = "ProductRecord"

  override def productArity: Int = __fields.size

  override def productElement(n: Int): Any =
    if (n >= 0 && n < productArity)
      __fields(n)._2
    else
      throw new IndexOutOfBoundsException(
        s"$n is out of bounds (min 0, max ${productArity - 1})",
      )

  override def productElementName(n: Int): String =
    if (n >= 0 && n < productArity)
      __fields(n)._1
    else
      throw new IndexOutOfBoundsException(
        s"$n is out of bounds (min 0, max ${productArity - 1})",
      )

  override def toString(): String =
    __fields
      .iterator
      .map { case (k, v) => s"$k = $v" }
      .mkString(s"${productPrefix}(", ", ", ")")

  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[ProductRecord]

  override def equals(other: Any): Boolean =
    other match {
      case other: ProductRecord =>
        __fields == other.__fields
      case _ =>
        false
    }
}

object ProductRecord {
  class MirrorOfProductRecord[
    ElemLabels <: Tuple,
    ElemTypes,
    P <: ProductRecord,
  ](elemLabels: Seq[String])
      extends Mirror.Product {
    type MirroredMonoType = P
    type MirroredType = P
    type MirroredLabel = "ProductRecord"
    type MirroredElemTypes = ElemTypes
    type MirroredElemLabels = ElemLabels

    def fromProduct(p: Product): P =
      new VectorRecord[%](elemLabels.zip(p.productIterator).toVector)
        .asInstanceOf[P]
  }

  inline given [R <: %](using
    r: RecordLike[ArrayRecord[R]],
  ): MirrorOfProductRecord[r.ElemLabels, r.ElemTypes, ArrayRecord[R]] =
    new MirrorOfProductRecord(RecordLike.seqOfLabels[r.ElemLabels])

  given [Elems, R <: %, P <: Product](using
    from: Mirror.ProductOf[ArrayRecord[R]],
    to: Mirror.ProductOf[P],
    ev: from.MirroredElemTypes <:< to.MirroredElemTypes,
  ): Converter[ArrayRecord[R], P] =
    new Converter {
      def apply(record: ArrayRecord[R]): P = to.fromProduct(record)
    }
}

abstract class ArrayRecord[R] extends ProductRecord with Dynamic {
  override def productPrefix: String = "ArrayRecord"

  transparent inline def selectDynamic(name: String) =
    ${ ArrayRecordMacros.selectImpl('this, 'name) }
}

object ArrayRecord extends ArrayRecord.Extensible[%] {
  protected def record: ArrayRecord[%] = empty

  val empty: ArrayRecord[%] = newArrayRecord[%](Vector.empty)

  import typing.withPotentialTypingError

  transparent inline def lookup[R <: %](record: ArrayRecord[R], label: String) =
    ${ ArrayRecordMacros.selectImpl('record, 'label) }

  inline def from[T, RR <: %](x: T)(using
    RecordLike[T],
    typing.Aux[T, RR],
  ): ArrayRecord[RR] = withPotentialTypingError {
    empty ++ x
  }

  extension [R <: %](record: ArrayRecord[R]) {
    def + : Extensible[R] = new Extensible.Appender(record)

    inline def ++[R2: RecordLike, RR <: %](
      other: R2,
    )(using typing.Concat.Aux[R, R2, RR]): ArrayRecord[RR] =
      withPotentialTypingError {
        newArrayRecord[RR](
          record
            .__fields
            .toVector
            .concat(summon[RecordLike[R2]].orderedIterableOf(other))
            .deduped
            .iterator
            .toVector,
        )
      }

    def tag[T]: ArrayRecord[R & Tag[T]] =
      record.asInstanceOf[ArrayRecord[R & Tag[T]]]

    def values(using
      r: RecordLike[R],
      conv: Converter[ArrayRecord[R], r.ElemTypes],
    ): r.ElemTypes = record.to[r.ElemTypes]

    inline def upcast[R2 >: R <: `%`: RecordLike]: ArrayRecord[R2] =
      newArrayRecord[R2](
        summon[RecordLike[ArrayRecord[R2]]]
          .orderedIterableOf(record.asInstanceOf[ArrayRecord[R2]])
          .toVector,
      )

    def to[To](using conv: Converter[ArrayRecord[R], To]): To = conv(record)

    inline def toTuple(using
      r: RecordLike[ArrayRecord[R]],
      conv: Converter[ArrayRecord[R], r.ElemTypes],
    ): Tuple.Zip[r.ElemLabels, r.ElemTypes] =
      r.elemLabels
        .zip(record.productIterator)
        .foldRight(EmptyTuple: Tuple) { (zipped, tuple) =>
          zipped *: tuple
        }
        .asInstanceOf[Tuple.Zip[r.ElemLabels, r.ElemTypes]]

    def toRecord: R = new MapRecord(record.__fields.toMap).asInstanceOf[R]
  }

  // Putting `apply` in the extension breaks `ArrayRecord.applyDynamicNamed`.
  implicit class Apply[R <: %](private val record: ArrayRecord[R])
      extends AnyVal {
    inline def apply[S <: Tuple, RR <: %](s: Selector[S])(using
      typing.Select.Aux[R, S, RR],
      RecordLike[ArrayRecord[R]],
    ): ArrayRecord[RR] = withPotentialTypingError {
      val sel = selection[S]
      val m = summon[RecordLike[ArrayRecord[R]]].iterableOf(record).toMap
      newArrayRecord[RR](
        sel
          .map((label, newLabel) => (newLabel, m(label)))
          .deduped
          .iterator
          .toVector,
      )
    }

    private inline def selection[S <: Tuple]: Seq[(String, String)] = {
      import scala.compiletime.{erasedValue, summonInline}

      inline erasedValue[S] match {
        case _: ((label, newLabel) *: tail) =>
          val st1 = summonInline[label <:< String]
          val st2 = summonInline[newLabel <:< String]
          (st1(valueOf[label]), st2(valueOf[newLabel])) +: selection[tail]
        case _: (label *: tail) =>
          val st = summonInline[label <:< String]
          val labelStr = st(valueOf[label])
          (labelStr, labelStr) +: selection[tail]
        case _: EmptyTuple =>
          Seq.empty
      }
    }

    inline def apply[U <: Tuple, RR <: %](u: Unselector[U])(using
      typing.Unselect.Aux[R, U, RR],
      RecordLike[ArrayRecord[RR]],
      R <:< RR,
    ): ArrayRecord[RR] = withPotentialTypingError {
      newArrayRecord[RR](
        summon[RecordLike[ArrayRecord[RR]]]
          .orderedIterableOf(record.asInstanceOf[ArrayRecord[RR]])
          .toVector,
      )
    }
  }

  given canEqualReflexive[R <: %]: CanEqual[ArrayRecord[R], ArrayRecord[R]] =
    CanEqual.derived

  final class RecordLikeArrayRecord[R <: %] extends RecordLike[ArrayRecord[R]] {
    def iterableOf(r: ArrayRecord[R]): Iterable[(String, Any)] = r.__fields
  }

  transparent inline given recordLike[R <: %](using
    ev: RecordLike[R],
  ): RecordLike[ArrayRecord[R]] =
    (new RecordLikeArrayRecord).asInstanceOf[
      RecordLikeArrayRecord[R] {
        type FieldTypes = ev.FieldTypes
        type ElemLabels = ev.ElemLabels
        type ElemTypes = ev.ElemTypes
      },
    ]

  private def newArrayRecord[R <: %](
    fields: IndexedSeq[(String, Any)],
  ): ArrayRecord[R] =
    new VectorRecord[R](fields.toVector)

  trait Extensible[R <: %] extends Any with Dynamic {
    protected def record: ArrayRecord[R]

    transparent inline def applyDynamic(method: String)(
      inline fields: (String, Any)*,
    ) =
      ${ ArrayRecordMacros.applyImpl('record, 'method, 'fields) }

    transparent inline def applyDynamicNamed(method: String)(
      inline fields: (String, Any)*,
    ) =
      ${ ArrayRecordMacros.applyImpl('record, 'method, 'fields) }
  }

  object Extensible {
    class Appender[R <: %](override protected val record: ArrayRecord[R])
        extends AnyVal
        with Extensible[R]
  }
}

final class VectorRecord[R](fields: IndexedSeq[(String, Any)])
    extends ArrayRecord[R] {
  override private[record4s] val __fields: Vector[(String, Any)] =
    fields.toVector
}
