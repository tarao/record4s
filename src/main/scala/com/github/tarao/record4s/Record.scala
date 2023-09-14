package com.github.tarao.record4s

/** Base trait for record types.
  *
  * This trait is a placeholder to avoid trouble with defining methods on companion object
  * of `%`.
  */
trait Record

object Record {
  /** An empty record. */
  val empty: % = new MapRecord(Map.empty)

  given canEqualReflexive[R <: %]: CanEqual[R, R] = CanEqual.derived

  extension [R <: %](record: R) {
    /** Extends fields of the record.
      *
      * If a new field has the same name as the existing field, then the new field
      * overrides the old one.
      *
      * @example {{{
      * val r = %(name = "tarao") + (age = 3, email = "tarao@example.com")
      * // val r: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      * }}}
      *
      * @return an object to define new fields
      */
    def + : Extensible[R] = new Extensible(record)

    /** Concatenate this record and another record.
      *
      * If the both record has a field of the same name, then it takes the field from the
      * latter record.
      *
      * @example {{{
      * val r1 = %(name = "tarao", age = 3)
      * val r2 = %(age = 4, email = "tarao@example.com")
      * val r3 = r1 ++ r2
      * // val r3: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 4, email = tarao@example.com)
      * }}}
      *
      * @param   R2     a record type (given `RecordLike[R2]`)
      * @param   other  a record to concatenate
      * @return  a new record which has the both fields from this record and `other`
      */
    transparent inline def ++[R2](other: R2)(using RecordLike[R2]) =
      ${ Macros.concatImpl('record, 'other) }

    /** Concatenate this record and another record.
      *
      * A field of the same name in the both record is statically rejected.
      *
      * @example {{{
      * val r1 = %(name = "tarao", age = 3)
      * val r2 = %(email = "tarao@example.com")
      * val r3 = r1 |+| r2
      * // val r3: com.github.tarao.record4s.%{val name: String; val age: Int} & com.github.tarao.record4s.%{val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      * }}}
      *
      * @param   R2     a record type (given `RecordLike[R2]`)
      * @param   other  a record to concatenate
      * @return  a new record which has the both fields from this record and `other`
      */
    inline def |+|[R2 <: %](other: R2)(using RecordLike[R2]): R & R2 =
      ${ Macros.concatDirectlyImpl('record, 'other) }

    /** Upcast the record to specified type.
      *
      * @example {{{
      * val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
      * // val r1: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      * val r2 = r1.as[% { val name: String; val age: Int }]
      * // val r2: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
      * }}}
      *
      * @param R2  target type
      * @return    a record containing only fields in the target type
      */
    inline def as[R2 >: R]: R2 =
      ${ Macros.upcastImpl[R, R2]('record) }
  }

  given recordLike[R <: %]: RecordLike[R] with {
    type FieldTypes = R

    extension (r: R) def toIterable: Iterable[(String, Any)] = r.__data
  }
}

/** Base class for records.
  *
  * Example {{{
  * val r = %(name = "tarao", age = 3)
  * // val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
  * r.name
  * // val res0: String = tarao
  * r.age
  * // val res1: Int = 3
  * }}}
  */
abstract class % extends Record with Selectable {
  private[record4s] def __data: Map[String, Any]

  def selectDynamic(name: String): Any = __data(name)

  override def toString(): String =
    __data.iterator.map { case (k, v) => s"$k = $v" }.mkString("%(", ", ", ")")

  override def equals(other: Any): Boolean =
    other match {
      case other: % =>
        __data == other.__data
      case _ =>
        false
    }
}

val % = new Extensible(Record.empty)

/** A concrete record class.
  *
  * This class is exposed due to inlining but not intended to be used directly.
  */
final class MapRecord(private[record4s] val __data: Map[String, Any]) extends %
