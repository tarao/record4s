package com.github.tarao.record4s

/** Type tags that only have meaning statically. */
opaque type Tag[T] = Any

private[record4s] object Tag {
  class IsTag[T]

  given [T]: IsTag[Tag[T]] = new IsTag[Tag[T]]
}
