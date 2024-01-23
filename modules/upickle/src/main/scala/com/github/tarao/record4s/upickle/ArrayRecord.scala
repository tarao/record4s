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

package com.github.tarao
package record4s
package upickle

import _root_.upickle.default.{ReadWriter, readwriter}

import upickle.Record.{readDict, writeDict}

object ArrayRecord {
  inline given readWriter[T <: Tuple](using
    r: RecordLike[record4s.ArrayRecord[T]],
  ): ReadWriter[record4s.ArrayRecord[T]] = {
    type Types = r.ElemTypes
    type Labels = r.ElemLabels

    readwriter[ujson.Value].bimap[ArrayRecord[T]](
      record => ujson.Obj(writeDict[Types, Labels](r.iterableOf(record).toMap)),
      json => {
        val dict = json.obj
        val iterable = readDict[Types, Labels, Vector[(String, Any)]](
          dict,
          Vector.newBuilder[(String, Any)],
        ).result()
        record4s.ArrayRecord.newArrayRecord[ArrayRecord[T]](iterable)
      },
    )
  }
}
