package com.github.tarao.record4s

trait RecordLike[R] {
  type FieldTypes

  extension (r: R) def toIterable: Iterable[(String, Any)]
}
