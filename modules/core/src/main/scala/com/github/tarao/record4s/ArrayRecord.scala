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
    ElemTypes <: Tuple,
    P <: ProductRecord,
  ](elemLabels: Seq[String])
      extends Mirror.Product {
    type MirroredMonoType = P
    type MirroredType = P
    type MirroredLabel = "ProductRecord"
    type MirroredElemTypes = ElemTypes
    type MirroredElemLabels = ElemLabels

    def fromProduct(p: Product): P =
      new VectorRecord(elemLabels.zip(p.productIterator).toVector)
        .asInstanceOf[P]
  }

  inline given [R](using
    r: RecordLike[ArrayRecord[R]],
  ): MirrorOfProductRecord[r.ElemLabels, r.ElemTypes, ArrayRecord[R]] =
    new MirrorOfProductRecord(RecordLike.seqOfLabels[r.ElemLabels])

  given [Elems, R, P <: Product](using
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

object ArrayRecord extends ArrayRecord.Extensible[EmptyTuple] {
  protected def record: ArrayRecord[EmptyTuple] = empty

  val empty: ArrayRecord[EmptyTuple] = newArrayRecord[EmptyTuple](Vector.empty)

  import typing.withPotentialTypingError

  transparent inline def lookup[R](
    record: ArrayRecord[R],
    label: String,
  ) =
    ${ ArrayRecordMacros.selectImpl('record, 'label) }

  inline def from[T: RecordLike, RR <: %](x: T)(using
    ev: typing.Concat.Aux[EmptyTuple, T, RR],
    rr: RecordLike[RR],
  ): ArrayRecord[rr.TupledFieldTypes] = withPotentialTypingError {
    empty ++ x
  }

  extension [R](record: ArrayRecord[R]) {
    def + : Extensible[R] = new Extensible.Appender(record)

    inline def ++[R2: RecordLike, RR <: %](
      other: R2,
    )(using
      ev: typing.Concat.Aux[R, R2, RR],
      rr: RecordLike[RR],
    ): ArrayRecord[rr.TupledFieldTypes] =
      withPotentialTypingError {
        newArrayRecord[rr.TupledFieldTypes](
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
      r: RecordLike[ArrayRecord[R]],
      conv: Converter[ArrayRecord[R], r.ElemTypes],
    ): r.ElemTypes = record.to[r.ElemTypes]

    transparent inline def upcast[R2] =
      ${ ArrayRecordMacros.upcastImpl[R, R2]('record) }

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

    inline def toRecord[RR <: %](using typing.Aux[ArrayRecord[R], RR]): RR =
      withPotentialTypingError {
        Record.from(record)
      }
  }

  // - Putting `apply` in the extension breaks `ArrayRecord.applyDynamicNamed`.
  // - Defining `to` in the extension breaks JMH.
  implicit class OpsCompat[R](private val record: ArrayRecord[R])
      extends AnyVal {
    inline def apply[S <: Tuple, RR <: %](s: Selector[S])(using
      r: RecordLike[ArrayRecord[R]],
      ev: typing.Select.Aux[R, S, RR],
      rr: RecordLike[RR],
    ): ArrayRecord[rr.TupledFieldTypes] = withPotentialTypingError {
      val sel = selection[S]
      val m = summon[RecordLike[ArrayRecord[R]]].iterableOf(record).toMap
      newArrayRecord[rr.TupledFieldTypes](
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
      ev: typing.Unselect.Aux[R, U, RR],
      rr: RecordLike[RR],
      ar: RecordLike[ArrayRecord[Tuple.Zip[rr.ElemLabels, rr.ElemTypes]]],
    ): ArrayRecord[Tuple.Zip[rr.ElemLabels, rr.ElemTypes]] =
      withPotentialTypingError {
        type ft = Tuple.Zip[rr.ElemLabels, rr.ElemTypes]
        newArrayRecord[ft](
          ar
            .orderedIterableOf(record.asInstanceOf[ArrayRecord[ft]])
            .toVector,
        )
      }

    def to[To](using conv: Converter[ArrayRecord[R], To]): To = conv(record)
  }

  given canEqualReflexive[R]: CanEqual[ArrayRecord[R], ArrayRecord[R]] =
    CanEqual.derived

  final class RecordLikeArrayRecord[R] extends RecordLike[ArrayRecord[R]] {
    def iterableOf(r: ArrayRecord[R]): Iterable[(String, Any)] = r.__fields
  }

  transparent inline given recordLike[R]: RecordLike[ArrayRecord[R]] =
    ${ ArrayRecordMacros.derivedRecordLikeImpl }

  private def newArrayRecord[R](
    fields: IndexedSeq[(String, Any)],
  ): ArrayRecord[R] =
    new VectorRecord(fields.toVector).asInstanceOf[ArrayRecord[R]]

  trait Extensible[R] extends Any with Dynamic {
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
    class Appender[R](override protected val record: ArrayRecord[R])
        extends AnyVal
        with Extensible[R]
  }
}

final class VectorRecord(fields: IndexedSeq[(String, Any)])
    extends ArrayRecord[EmptyTuple] {
  override private[record4s] val __fields: Vector[(String, Any)] =
    fields.toVector
}
