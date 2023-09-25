package com.github.tarao.record4s

/** Base trait for record types.
  *
  * This trait is a placeholder to avoid trouble with defining methods on
  * companion object of `%`.
  */
trait Record

object Record {

  /** An empty record. */
  val empty = newMapRecord[%](Map.empty)

  /** Get the field value of specified label.
    *
    * It is essentially the same as `record.{label}` but it can access to fields
    * hidden by own methods of `class %`.
    *
    * @example
    *   {{{
    * val r = %(value = 3, toString = 10)
    *
    * r.value
    * // val res0: Int = 3
    * r.toString
    * // val res1: String = %(value = 3, toString = 10)
    *
    * Record.lookup(r, "value")
    * // val res2: Int = 3
    * Record.lookup(r, "toString")
    * // val res3: Int = 10
    *   }}}
    *
    * @param record
    *   a record
    * @param label
    *   a string literal field name
    * @return
    *   the value of the field named by `label`
    */
  transparent inline def lookup[R <: %](record: R, label: String) =
    ${ Macros.lookupImpl('record, 'label) }

  /** Construct a record from something else.
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val p = Person("tarao", 3)
    * val r = Record.from(p)
    * // val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
    *   }}}
    *
    * @tparam T
    *   some type given `RecordLike[T]`
    * @param x
    *   something that is record like
    * @return
    *   a record
    */
  inline def from[T, RR <: %](x: T)(using
    RecordLike[T],
    typing.Aux[T, RR],
  ): RR = empty ++ x

  extension [R <: %](record: R) {

    /** Extend the record by fields.
      *
      * If a new field has the same name as the existing field, then the new
      * field overrides the old one.
      *
      * @example
      *   {{{
      * val r = %(name = "tarao") + (age = 3, email = "tarao@example.com")
      * // val r: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      *   }}}
      *
      * @return
      *   an object to define new fields
      */
    def + : Extensible[R] = new Extensible(record)

    /** Concatenate this record and another record.
      *
      * If the both record has a field of the same name, then it takes the field
      * from the latter record.
      *
      * @example
      *   {{{
      * val r1 = %(name = "tarao", age = 3)
      * val r2 = %(age = 4, email = "tarao@example.com")
      * val r3 = r1 ++ r2
      * // val r3: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 4, email = tarao@example.com)
      *   }}}
      *
      * @tparam R2
      *   a record type (given `RecordLike[R2]`)
      * @param other
      *   a record to concatenate
      * @return
      *   a new record which has the both fields from this record and `other`
      */
    inline def ++[R2: RecordLike, RR <: %](
      other: R2,
    )(using typing.Concat.Aux[R, R2, RR]): RR =
      newMapRecord[RR](
        record.__data ++ summon[RecordLike[R2]].tidiedIterableOf(other),
      )

    /** Give a type tag to this record.
      *
      * @example
      *   {{{
      * trait Person; object Person {
      *   extension (p: %{val name: String} & Tag[Person]) {
      *     def firstName: String = p.name.split(" ").head
      *   }
      * }
      *
      * val r = %(name = "tarao fuguta", age = 3).tag[Person]
      * r.firstName
      * // val res0: String = tarao
      *   }}}
      *
      * @tparam T
      *   an arbitrary type used as a tag
      * @return
      *   the same record with a tag type
      */
    def tag[T]: R & Tag[T] =
      record.asInstanceOf[R & Tag[T]]

    /** Upcast the record to specified type.
      *
      * @example
      *   {{{
      * val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
      * // val r1: com.github.tarao.record4s.%{val name: String; val age: Int; val email: String} = %(name = tarao, age = 3, email = tarao@example.com)
      * val r2 = r1.as[% { val name: String; val age: Int }]
      * // val r2: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
      *   }}}
      *
      * @tparam R2
      *   target type
      * @return
      *   a record containing only fields in the target type
      */
    inline def as[R2 >: R <: `%`: RecordLike]: R2 =
      newMapRecord[R2](summon[RecordLike[R2]].tidiedIterableOf(record))

    /** Convert this record to a `Product`.
      *
      * Target product type `P` must provide:
      *
      *   - a `Mirror.Of[P]`
      *     - used via `RecordLike[P]`
      *   - an `apply` method in the companion object of `P`
      *     - the argument order must be the same as
      *       `Mirror.Of[P]#MirroredElemLabels`
      *
      * `case class`es conform to the above conditions.
      *
      * @example
      *   {{{
      * case class Person(name: String, age: Int)
      * val r = %(name = "tarao", age = 3)
      * r.to[Person]
      * // val res0: Person = Person(tarao,3)
      *   }}}
      *
      * @tparam P
      *   a target product type (given `RecordLike[P]`)
      * @return
      *   a new product instance
      */
    inline def to[P <: Product](using RecordLike[P]): P =
      ${ Macros.toProductImpl[R, P]('record) }

    /** Convert this record to a `Tuple`.
      *
      * @example
      *   {{{
      * val r1 = %(name = "tarao", age = 3)
      * r1.toTuple
      * // val res0: (("name", String), ("age", Int)) = ((name,tarao),(age,3))
      *   }}}
      *
      * @return fields of label-value pairs as a tuple
      */
    inline def toTuple(using
      r: RecordLike[R],
    ): Tuple.Zip[r.ElemLabels, r.ElemTypes] = {
      val tuple = r
        .elemLabels
        .foldRight(EmptyTuple: Tuple) { (label, tuple) =>
          (label, record.__data(label)) *: tuple
        }
      tuple.asInstanceOf[Tuple.Zip[r.ElemLabels, r.ElemTypes]]
    }
  }

  given canEqualReflexive[R <: %]: CanEqual[R, R] = CanEqual.derived

  final class RecordLikeRecord[R <: %] extends RecordLike[R] {
    def iterableOf(r: R): Iterable[(String, Any)] = r.__data
  }

  transparent inline given recordLike[R <: %]: RecordLike[R] =
    ${ Macros.derivedRecordLikeImpl }

  private def newMapRecord[R <: %](record: Iterable[(String, Any)]): R =
    new MapRecord(record.toMap).asInstanceOf[R]

  import scala.language.dynamics

  class Extensible[R](private val record: R) extends AnyVal with Dynamic {
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
  * {{{
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

  def selectDynamic(name: String): Any =
    __data(scala.reflect.NameTransformer.decode(name))

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

val % = new Record.Extensible(Record.empty)

/** A concrete record class.
  *
  * This class is exposed due to inlining but not intended to be used directly.
  */
final class MapRecord(private[record4s] val __data: Map[String, Any]) extends %
