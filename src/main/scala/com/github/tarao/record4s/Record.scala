package com.github.tarao.record4s

trait Record

object Record {
  val empty: % = new MapRecord(Map.empty)

  given canEqualReflexive[R <: %]: CanEqual[R, R] = CanEqual.derived

  extension [R <: %](record: R) {
    def + : Extensible[R] = new Extensible(record)

    transparent inline def ++[R2](other: R2) =
      ${ Macros.concatImpl('record, 'other) }

    inline def |+|[R2 <: %](other: R2): R & R2 =
      ${ Macros.concatDirectlyImpl('record, 'other) }

    inline def as[R2 >: R]: R2 =
      ${ Macros.upcastImpl[R, R2]('record) }
  }

  given recordLike[R <: %]: RecordLike[R] with {
    type FieldTypes = R

    extension (r: R) def toIterable: Iterable[(String, Any)] = r.__data
  }
}

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

final class MapRecord(private[record4s] val __data: Map[String, Any]) extends %
