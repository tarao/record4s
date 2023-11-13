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

import typing.Record.{Aux, Concat, Lookup, Select, Unselect}

/** Base trait for record types.
  *
  * This trait is a placeholder to avoid trouble with defining methods on
  * companion object of `%`.
  */
trait Record

object Record {
  import typing.withPotentialTypingError

  /** An empty record. */
  val empty = newMapRecord[%](Map.empty)

  /** Get the field value of specified label.
    *
    * It is essentially the same as `record.{label}` but it can access to fields
    * hidden by own methods of `class %`.
    *
    * @example
    *   ```
    *   val r = %(value = 3, toString = 10)
    *
    *   r.value
    *   // val res0: Int = 3
    *   r.toString
    *   // val res1: String = %(value = 3, toString = 10)
    *
    *   Record.lookup(r, "value")
    *   // val res2: Int = 3
    *   Record.lookup(r, "toString")
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
  def lookup[R <: %, L <: String & Singleton, Out](record: R, label: L)(using
    Lookup.Aux[R, L, Out],
  ): Out =
    record.__lookup(label).asInstanceOf[Out]

  /** Construct a record from something else.
    *
    * @example
    *   ```
    *   case class Person(name: String, age: Int)
    *   val p = Person("tarao", 3)
    *   val r = Record.from(p)
    *   // val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
    *   ```
    *
    * @tparam T
    *   some type given `RecordLike[T]`
    * @param x
    *   something that is record like
    * @return
    *   a record
    */
  inline def from[T: RecordLike, RR <: %](x: T)(using Aux[T, RR]): RR =
    withPotentialTypingError {
      empty ++ x
    }

  extension [R <: %](record: R) {

    /** Extend the record by fields.
      *
      * If a new field has the same name as the existing field, then the new
      * field overrides the old one.
      *
      * @example
      *   ```
      *   val r = %(name = "tarao") + (age = 3, email = "tarao@example.com")
      *   // val r: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      *   ```
      *
      * @return
      *   an object to define new fields
      */
    def updated: Extensible[R] = new Extensible(record)

    /** Alias for `updated` */
    inline def + : Extensible[R] = updated

    /** Concatenate this record and another record.
      *
      * If the both record has a field of the same name, then it takes the field
      * from the latter record.
      *
      * @example
      *   ```
      *   val r1 = %(name = "tarao", age = 3)
      *   val r2 = %(age = 4, email = "tarao@example.com")
      *   val r3 = r1 ++ r2
      *   // val r3: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 4, email = tarao@example.com)
      *   ```
      *
      * @tparam R2
      *   a record type (given `RecordLike[R2]`)
      * @param other
      *   a record to concatenate
      * @return
      *   a new record which has the both fields from this record and `other`
      */
    inline def concat[R2: RecordLike, RR <: %](
      other: R2,
    )(using Concat.Aux[R, R2, RR]): RR = withPotentialTypingError {
      newMapRecord[RR](
        record
          .__iterable
          .toMap
          .concat(summon[RecordLike[R2]].tidiedIterableOf(other)),
      )
    }

    /** Alias for `concat` */
    inline def ++[R2: RecordLike, RR <: %](
      other: R2,
    )(using Concat.Aux[R, R2, RR]): RR = concat(other)

    /** Create a new record by selecting some fields of an existing record.
      *
      * @example
      *   ```
      *   val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
      *   val r2 = r1(select.name.age)
      *   // val r2: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
      *   val r3 = r1(select.name(rename = "nickname").age)
      *   // val r3: com.github.tarao.record4s.%{val nickname: String; val age: Int} = %(nickname = tarao, age = 3)
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
      Select.Aux[R, S, RR],
    ): RR = withPotentialTypingError {
      newMapRecord[RR](toSelectedIterable[S])
    }

    private inline def toSelectedIterable[S <: Tuple]: Seq[(String, Any)] = {
      import scala.compiletime.{erasedValue, summonInline}

      inline erasedValue[S] match {
        case _: ((label, newLabel) *: tail) =>
          val st1 = summonInline[label <:< String]
          val st2 = summonInline[newLabel <:< String]
          (
            st2(valueOf[newLabel]),
            record.__lookup(st1(valueOf[label])),
          ) +: toSelectedIterable[tail]
        case _: (label *: tail) =>
          val st = summonInline[label <:< String]
          val labelStr = st(valueOf[label])
          (labelStr, record.__lookup(labelStr)) +: toSelectedIterable[tail]
        case _: EmptyTuple =>
          Seq.empty
      }
    }

    /** Create a new record by unselecting some fields of an existing record.
      *
      * @example
      *   ```
      *   val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
      *   val r2 = r1(unselect.email)
      *   // val r2: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
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
    inline def apply[U <: Tuple, RR <: %](u: Unselector[U])(using
      Unselect.Aux[R, U, RR],
      RecordLike[RR],
      R <:< RR,
    ): RR = withPotentialTypingError {
      newMapRecord[RR](summon[RecordLike[RR]].tidiedIterableOf(record))
    }

    /** Give a type tag to this record.
      *
      * @example
      *   ```
      *   trait Person; object Person {
      *     extension (p: %{val name: String} & Tag[Person]) {
      *       def firstName: String = p.name.split(" ").head
      *     }
      *   }
      *
      *   val r = %(name = "tarao fuguta", age = 3).tag[Person]
      *   r.firstName
      *   // val res0: String = tarao
      *   ```
      *
      * @tparam T
      *   an arbitrary type used as a tag
      * @return
      *   the same record with a tag type
      */
    def tag[T]: R & Tag[T] =
      record.asInstanceOf[R & Tag[T]]

    /** Return values of this record as a `Tuple`.
      *
      * @example
      *   ```
      *   val r1 = %(name = "tarao", age = 3)
      *   r1.values
      *   // val res0: (String, Int) = (tarao,3)
      *   ```
      *
      * @return
      *   values of the record as a tuple
      */
    inline def values(using r: RecordLike[R]): r.ElemTypes =
      r.elemLabels
        .foldRight(EmptyTuple: Tuple) { (label, tuple) =>
          record.__lookup(label) *: tuple
        }
        .asInstanceOf[r.ElemTypes]

    /** Upcast the record to specified type.
      *
      * @example
      *   ```
      *   val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
      *   // val r1: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      *   val r2 = r1.as[% { val name: String; val age: Int }]
      *   // val r2: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
      *   ```
      *
      * @tparam R2
      *   target type
      * @return
      *   a record containing only fields in the target type
      */
    inline def as[R2 >: R <: `%`: RecordLike]: R2 =
      newMapRecord[R2](summon[RecordLike[R2]].tidiedIterableOf(record))

    /** Convert this record to a `To`.
      *
      * @example
      *   ```
      *   case class Person(name: String, age: Int)
      *   val r = %(name = "tarao", age = 3)
      *   r.to[Person]
      *   // val res0: Person = Person(tarao,3)
      *   ```
      *
      * @tparam To
      *   a type to which the record is converted
      * @return
      *   a new product instance
      */
    def to[To](using conv: Converter[R, To]): To = conv(record)

    /** Convert this record to a `Tuple`.
      *
      * @example
      *   ```
      *   val r1 = %(name = "tarao", age = 3)
      *   r1.toTuple
      *   // val res0: (("name", String), ("age", Int)) = ((name,tarao),(age,3))
      *   ```
      *
      * @return
      *   fields of label-value pairs as a tuple
      */
    inline def toTuple(using
      r: RecordLike[R],
    ): Tuple.Zip[r.ElemLabels, r.ElemTypes] = {
      val tuple = r
        .elemLabels
        .foldRight(EmptyTuple: Tuple) { (label, tuple) =>
          (label, record.__lookup(label)) *: tuple
        }
      tuple.asInstanceOf[Tuple.Zip[r.ElemLabels, r.ElemTypes]]
    }
  }

  given canEqualReflexive[R <: %]: CanEqual[R, R] = CanEqual.derived

  final class RecordLikeRecord[R <: %] extends RecordLike[R] {
    def iterableOf(r: R): Iterable[(String, Any)] = r.__iterable
  }

  transparent inline given recordLike[R <: %]: RecordLike[R] =
    ${ Macros.derivedRecordLikeImpl }

  private def newMapRecord[R <: %](record: Iterable[(String, Any)]): R =
    new MapRecord(record.toMap).asInstanceOf[R]

  import scala.language.dynamics

  class Extensible[R <: %](private val record: R) extends AnyVal with Dynamic {
    transparent inline def applyDynamic(method: String)(
      inline fields: (String, Any)*,
    ) =
      ${ Macros.applyImpl('record, 'method, 'fields) }

    transparent inline def applyDynamicNamed(method: String)(
      inline fields: (String, Any)*,
    ) =
      ${ Macros.applyImpl('record, 'method, 'fields) }
  }
}

/** Base class for records.
  *
  * Example
  * ```
  * val r = %(name = "tarao", age = 3)
  * // val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
  * r.name
  * // val res0: String = tarao
  * r.age
  * // val res1: Int = 3
  * ```
  */
abstract class % extends Record with Selectable {
  private[record4s] def __lookup(key: String): Any

  private[record4s] def __iterable: Iterable[(String, Any)]

  def selectDynamic(name: String): Any =
    __lookup(scala.reflect.NameTransformer.decode(name))

  /** Stringify the record.
    *
    * The order of key-value pairs may differ from the order of static field
    * types. It also shows statically hidden fields if the static type of the
    * record was narrowed by upcast.
    */
  override def toString(): String =
    __iterable
      .iterator
      .map { case (k, v) => s"$k = $v" }
      .mkString("%(", ", ", ")")

  override def equals(other: Any): Boolean =
    other match {
      case other: % =>
        __iterable == other.__iterable
      case _ =>
        false
    }
}

val % = new Record.Extensible(Record.empty)

/** A concrete record class.
  *
  * This class is exposed due to inlining but not intended to be used directly.
  */
final class MapRecord(private val __data: Map[String, Any]) extends % {
  override private[record4s] def __lookup(key: String): Any = __data(key)

  override private[record4s] def __iterable: Iterable[(String, Any)] = __data
}
