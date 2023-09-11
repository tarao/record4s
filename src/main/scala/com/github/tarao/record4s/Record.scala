package com.github.tarao.record4s

trait Record

object Record {
  val empty: % = new MapRecord(Map.empty)

  extension [R <: %](record: R) {
    def + : Extensible[R] = new Extensible(record)
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
}

val % = new Extensible(Record.empty)

final class MapRecord(private[record4s] val __data: Map[String, Any]) extends %
