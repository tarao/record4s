package com.github.tarao.record4s

trait RecordLike[R] {
  type FieldTypes

  def iterableOf(r: R): Iterable[(String, Any)]
}
