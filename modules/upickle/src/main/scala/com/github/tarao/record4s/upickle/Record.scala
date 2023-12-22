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

import _root_.upickle.core.LinkedHashMap
import _root_.upickle.default.{
  ReadWriter,
  Reader,
  Writer,
  read,
  readwriter,
  writeJs,
}

import scala.collection.mutable.Builder
import scala.compiletime.{constValue, erasedValue, summonInline}

object Record {
  private[upickle] inline def writeDict[Types, Labels](
    record: Map[String, Any],
    res: LinkedHashMap[String, ujson.Value] = LinkedHashMap(),
  ): LinkedHashMap[String, ujson.Value] =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        res

      case _: (tpe *: types, label *: labels) =>
        val labelStr = constValue[label & String]
        val value =
          inline erasedValue[tpe] match {
            case _: ujson.Value =>
              record(labelStr).asInstanceOf[ujson.Value]
            case _ =>
              val writer = summonInline[Writer[tpe]]
              val elem = record(labelStr).asInstanceOf[tpe]
              writeJs[tpe](elem)(using writer)
          }
        writeDict[types, labels](record, res += (labelStr -> value))
    }

  private[upickle] inline def readDict[Types, Labels, C](
    dict: LinkedHashMap[String, ujson.Value],
    res: Builder[(String, Any), C],
  ): Builder[(String, Any), C] =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        res

      case _: (tpe *: types, label *: labels) =>
        val labelStr = constValue[label & String]
        val jsonElem = dict.getOrElse(labelStr, ujson.Null)
        val reader = summonInline[Reader[tpe]]
        val elem = read[tpe](jsonElem)(using reader)
        res += (labelStr -> elem)
        readDict[types, labels, C](dict, res)
    }

  inline given readWriter[R <: %](using r: RecordLike[R]): ReadWriter[R] = {
    type Types = r.ElemTypes
    type Labels = r.ElemLabels

    readwriter[ujson.Value].bimap[R](
      record => ujson.Obj(writeDict[Types, Labels](r.iterableOf(record).toMap)),
      json => {
        val dict = json.obj
        val iterable = readDict[Types, Labels, Map[String, Any]](
          dict,
          Map.newBuilder[String, Any],
        ).result()
        record4s.Record.newMapRecord[R](iterable)
      },
    )
  }
}
