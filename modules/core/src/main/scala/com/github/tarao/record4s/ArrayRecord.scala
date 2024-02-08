/*
 * Copyright 2023 record4s authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tarao.record4s

import scala.deriving.Mirror
import scala.language.dynamics

import typing.ArrayRecord.{Aux, Concat, Lookup}
import typing.Record.{Select, Unselect}
import util.SeqOps.deduped

/** Base record class compatible with Product. */
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

  /** Stringify the record.
    *
    * The order of key-value pairs is exactly the same as that of the static
    * field types.
    */
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
  import ArrayRecord.newArrayRecord

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
      newArrayRecord[P](elemLabels.zip(p.productIterator).toVector)
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

/** Record class with constant-time field access.
  *
  * It provides extensible records in a different way than `%`. There are
  * following differences compared to `%`.
  *
  *   - Field access is faster (constant time)
  *   - Creation is faster
  *   - Update time grows significantly when record size grows
  *   - Compilation is a bit slower
  *   - No implicit upcast
  *
  * Example
  * ```
  * val r = ArrayRecord(name = "tarao", age = 3)
  * // val r: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
  * r.name
  * // val res0: String = tarao
  * r.age
  * // val res1: Int = 3
  * ```
  */
abstract class ArrayRecord[R] extends ProductRecord with Dynamic {
  override def productPrefix: String = "ArrayRecord"

  transparent inline def selectDynamic[L <: String & Singleton](name: L) =
    ${ ArrayRecordMacros.selectImpl('this, 'name) }
}

object ArrayRecord
    extends ArrayRecord.Extensible[EmptyTuple]
    with ArrayRecordPlatformSpecific {
  import scala.compiletime.{constValue, erasedValue, summonInline}
  import typing.withPotentialTypingError

  protected def record: ArrayRecord[EmptyTuple] = empty

  /** An empty record */
  val empty: ArrayRecord[EmptyTuple] =
    newArrayRecord[ArrayRecord[EmptyTuple]](Vector.empty)

  /** Get the field value of specified label.
    *
    * It is essentially the same as `record.{label}` but it can access to fields
    * hidden by own methods of `class ArrayRecord`.
    *
    * @example
    *   ```
    *   val r = ArrayRecord(value = 3, toString = 10)
    *
    *   r.value
    *   // val res0: Int = 3
    *   r.toString
    *   // val res1: String = ArrayRecord(value = 3, toString = 10)
    *
    *   ArrayRecord.lookup(r, "value")
    *   // val res2: Int = 3
    *   ArrayRecord.lookup(r, "toString")
    *   // val res3: Int = 10
    *   ```
    *
    * @param record
    *   a record
    * @param label
    *   a string literal field name
    * @return
    *   the value of the field named by `label`
    */
  inline def lookup[R, L <: String & Singleton, Index <: Int, Out](
    record: ArrayRecord[R],
    label: L,
  )(using Lookup.Aux[R, L, Index, Out]): Out =
    record.__fields(constValue[Index])._2.asInstanceOf[Out]

  /** Construct a record from something else.
    *
    * @example
    *   ```
    *   case class Person(name: String, age: Int)
    *   val p = Person("tarao", 3)
    *   val r = ArrayRecord.from(p)
    *   // val r: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
    *   ```
    *
    * @tparam T
    *   some type given `RecordLike[T]`
    * @param x
    *   something that is record like
    * @return
    *   a record
    */
  inline def from[T: RecordLike, RR <: ProductRecord](x: T)(using
    Aux[T, RR],
  ): RR = withPotentialTypingError {
    empty ++ x
  }

  extension [R](record: ArrayRecord[R]) {

    /** Extend the record by fields.
      *
      * If a new field has the same name as the existing field, then the new
      * field overrides the old one.
      *
      * @example
      *   ```
      *   val r = ArrayRecord(name = "tarao") + (age = 3, email = "tarao@example.com")
      *   // val r: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int), ("email", String))] = ArrayRecord(name = tarao, age = 3, email = tarao@example.com)
      *   ```
      *
      * @return
      *   an object to define new fields
      */
    def updated: Extensible[R] = new Extensible.Appender(record)

    /** Alias for `updated` */
    inline def + : Extensible[R] = updated

    /** Concatenate this record and another record.
      *
      * If the both record has a field of the same name, then it takes the field
      * from the latter record.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3)
      *   val r2 = ArrayRecord(age = 4, email = "tarao@example.com")
      *   val r3 = r1 ++ r2
      *   // val r3: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int), ("email", String))] = ArrayRecord(name = tarao, age = 4, email = tarao@example.com)
      *   ```
      *
      * @tparam R2
      *   a record type (given `RecordLike[R2]`)
      * @param other
      *   a record to concatenate
      * @return
      *   a new record which has the both fields from this record and `other`
      */
    inline def concat[R2: RecordLike, RR <: ProductRecord](
      other: R2,
    )(using c: Concat[R, R2]): c.Out =
      withPotentialTypingError {
        summon[typing.Concrete[R2]]
        val vec = record
          .__fields
          .toVector
        val otherIt = summon[RecordLike[R2]].orderedIterableOf(other)
        inline erasedValue[c.NeedDedup] match {
          case _: false =>
            newArrayRecord[c.Out](vec.concat(otherIt))
          case _ =>
            newArrayRecord[c.Out](unsafeConcat(vec, otherIt))
        }
      }

    /** Alias for `concat` */
    inline def ++[R2: RecordLike, RR <: ProductRecord](
      other: R2,
    )(using c: Concat[R, R2]): c.Out = concat(other)

    /** Give a type tag to this record.
      *
      * @example
      *   ```
      *   trait Person; object Person {
      *     extension [T <: Tuple](p: ArrayRecord[(("name", String) *: T) & Tag[Person]]) {
      *       def firstName: String = p.name.split(" ").head
      *     }
      *   }
      *
      *   val r = ArrayRecord(name = "tarao fuguta", age = 3).tag[Person]
      *   r.firstName
      *   // val res0: String = tarao
      *   ```
      *
      * @tparam T
      *   an arbitrary type used as a tag
      * @return
      *   the same record with a tag type
      */
    def tag[T]: ArrayRecord[R & Tag[T]] =
      record.asInstanceOf[ArrayRecord[R & Tag[T]]]

    /** Return values of this record as a `Tuple`.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3)
      *   r1.values
      *   // val res0: (String, Int) = (tarao,3)
      *   ```
      *
      * @return
      *   values of the record as a tuple
      */
    def values(using
      r: RecordLike[ArrayRecord[R]],
      conv: Converter[ArrayRecord[R], r.ElemTypes],
    ): r.ElemTypes = record.to[r.ElemTypes]

    /** Upcast the record to specified type.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
      *   // val r1: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int), ("email", String))] = ArrayRecord(name = tarao, age = 3, email = tarao@example.com)
      *   val r2 = r1.upcast[(("name", String), ("age", Int))]
      *   // val r2: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
      *   ```
      *
      * @tparam R2
      *   target type
      * @return
      *   a record containing only fields in the target type
      */
    transparent inline def upcast[R2] =
      ${ ArrayRecordMacros.upcastImpl[R, R2]('record) }

    private[record4s] inline def shrinkTo[R2](using
      r: RecordLike[ArrayRecord[R2]],
    ): ArrayRecord[R2] =
      newArrayRecord[ArrayRecord[R2]](
        // This is quite hacky: `forceOrderedIterableOf` makes a Map from
        // `record.__fields` and collect values according to `R2`.  It is unsafe to do
        // `record.asInstanceOf[ArrayRecord[R2]]` in most cases but it's OK here because
        // `forceOrderedIterableOf` doesn't depend on the static type of the argument.
        r.forceOrderedIterableOf(record.asInstanceOf[ArrayRecord[R2]]).toVector,
      )

    /** Convert this record to a `Tuple`.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3)
      *   r1.toTuple
      *   // val res0: (("name", String), ("age", Int)) = ((name,tarao),(age,3))
      *   ```
      *
      * @return
      *   fields of label-value pairs as a tuple
      */
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

    /** Convert to a record of type `%`.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3)
      *   // val r1: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
      *   r1.toRecord
      *   // val res0: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
      *   ```
      *
      * @return
      *   a record of type `%`
      */
    inline def toRecord[RR <: %](using
      typing.Record.Aux[ArrayRecord[R], RR],
    ): RR =
      withPotentialTypingError {
        Record.from(record)
      }
  }

  // - Putting `apply` in the extension breaks `ArrayRecord.applyDynamicNamed`.
  // - Defining `to` in the extension breaks JMH.
  implicit class OpsCompat[R](private val record: ArrayRecord[R])
      extends AnyVal {

    /** Create a new record by selecting some fields of an existing record.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
      *   val r2 = r1(select.name.age)
      *   // val r2: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
      *   val r3 = r1(select.name(rename = "nickname").age)
      *   // val r3: com.github.tarao.record4s.ArrayRecord[(("nickname", String), ("age", Int))] = ArrayRecord(nickname = tarao, age = 3)
      *   ```
      *
      * @tparam S
      *   list of selected field as a Tuple
      * @tparam RR
      *   type of the new record
      * @param s
      *   selection of fields created by `select`
      * @return
      *   a new record with the selected fields
      */
    inline def apply[S <: Tuple, RR <: %](s: Selector[S])(using
      r: RecordLike[ArrayRecord[R]],
      ev: Select.Aux[R, S, RR],
      rr: RecordLike[RR],
    ): ArrayRecord[rr.TupledFieldTypes] = withPotentialTypingError {
      val sel = selection[S]
      val m = r.iterableOf(record).toMap
      newArrayRecord[ArrayRecord[rr.TupledFieldTypes]](
        sel
          .map((label, newLabel) => (newLabel, m(label)))
          .deduped
          .iterator
          .toVector,
      )
    }

    private inline def selection[S <: Tuple]: Seq[(String, String)] = {
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

    /** Create a new record by unselecting some fields of an existing record.
      *
      * @example
      *   ```
      *   val r1 = ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
      *   val r2 = r1(unselect.email)
      *   // val r2: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
      *   ```
      *
      * @tparam U
      *   list of unselected field as a Tuple
      * @tparam RR
      *   type of the new record
      * @param u
      *   unselection of fields created by `unselect`
      * @return
      *   a new record without the unselected fields
      */
    inline def apply[U <: Tuple, R2 <: %, RR <: %](u: Unselector[U])(using
      r: typing.Record.Aux[ArrayRecord[R], R2],
      ev: Unselect.Aux[R2, U, RR],
      rr: RecordLike[RR],
    ): ArrayRecord[Tuple.Zip[rr.ElemLabels, rr.ElemTypes]] =
      withPotentialTypingError {
        withPotentialTypingError {
          record.shrinkTo[Tuple.Zip[rr.ElemLabels, rr.ElemTypes]]
        }(using ev)
      }(using r)

    /** Convert this record to a `To`.
      *
      * @example
      *   ```
      *   case class Person(name: String, age: Int)
      *   val r = ArrayRecord(name = "tarao", age = 3)
      *   r.to[Person]
      *   // val res0: Person = Person(tarao,3)
      *   ```
      *
      * @tparam To
      *   a type to which the record is converted
      * @return
      *   a new product instance
      */
    def to[To](using conv: Converter[ArrayRecord[R], To]): To = conv(record)
  }

  given canEqualReflexive[R]: CanEqual[ArrayRecord[R], ArrayRecord[R]] =
    CanEqual.derived

  final class RecordLikeArrayRecord[R] extends RecordLike[ArrayRecord[R]] {
    def iterableOf(r: ArrayRecord[R]): Iterable[(String, Any)] = r.__fields
  }

  transparent inline given recordLike[R]: RecordLike[ArrayRecord[R]] =
    ${ ArrayRecordMacros.derivedRecordLikeImpl }

  private[record4s] def newArrayRecord[R](
    fields: IndexedSeq[(String, Any)],
  ): R =
    new VectorRecord(fields.toVector).asInstanceOf[R]

  // the same as `(record ++ tidiedIterable).deduped` but use `Vector.updated` for
  // optimization
  private[record4s] def unsafeConcat(
    record: Vector[(String, Any)],
    tidiedIterable: Iterable[(String, Any)],
  ): Vector[(String, Any)] = {
    val m = tidiedIterable.toMap
    val updates = record.zipWithIndex.filter { case ((key, _), _) =>
      m.contains(key)
    }
    val dups = updates.map(_._1._1).toSet
    updates
      .foldLeft(record) { case (vec, ((key, _), index)) =>
        vec.updated(index, (key, m(key)))
      }
      .concat(tidiedIterable.filterNot(f => dups.contains(f._1)))
  }

  trait Extensible[R] extends Any with Dynamic {
    protected def record: ArrayRecord[R]

    transparent inline def applyDynamic(method: String)(
      inline fields: Any*,
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

/** A concrete record class.
  *
  * This class is exposed due to inlining but not intended to be used directly.
  */
final class VectorRecord private[record4s] (fields: IndexedSeq[(String, Any)])
    extends ArrayRecord[EmptyTuple] {
  override private[record4s] val __fields: Vector[(String, Any)] =
    fields.toVector
}
