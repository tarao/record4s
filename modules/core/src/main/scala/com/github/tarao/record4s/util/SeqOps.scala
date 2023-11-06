package com.github.tarao.record4s.util

object SeqOps {
  extension [V](keyValueSeq: Seq[(String, V)]) {
    def deduped: IterableOnce[(String, V)] = {
      val seen = collection.mutable.HashSet[String]()
      val result = collection.mutable.ListBuffer.empty[(String, V)]
      keyValueSeq.reverseIterator.foreach { case (key, value) =>
        if (seen.add(key)) result.prepend((key, value))
      }
      result
    }
  }
}
