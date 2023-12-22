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

package com.github.tarao.record4s
package circe

import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.collection.mutable.Builder
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.util.NotGiven

object Codec {
  private inline def encodeFields[Types, Labels](
    record: Map[String, Any],
    res: Builder[(String, Json), Map[String, Json]] =
      Map.newBuilder[String, Json],
  ): Builder[(String, Json), Map[String, Json]] =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        res

      case _: (tpe *: types, label *: labels) =>
        val labelStr = constValue[label & String]
        val value =
          inline erasedValue[tpe] match {
            case _: Json =>
              record(labelStr).asInstanceOf[Json]
            case _ =>
              val enc = summonInline[Encoder[tpe]]
              enc(record(labelStr).asInstanceOf[tpe])
          }
        res += (labelStr -> value)
        encodeFields[types, labels](record, res)
    }

  private inline def decodeFields[Types, Labels](
    c: HCursor,
    res: Builder[(String, Any), Map[String, Any]] = Map.newBuilder[String, Any],
  ): Decoder.Result[Builder[(String, Any), Map[String, Any]]] =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        Right(res)

      case _: (tpe *: types, label *: labels) =>
        val labelStr = constValue[label & String]
        val dec = summonInline[Decoder[tpe]]
        c.downField(labelStr).as[tpe](using dec).flatMap { value =>
          res += (labelStr -> value)
          decodeFields[types, labels](c, res)
        }
    }

  inline given encoder[R <: %](using
    r: RecordLike[R],
    nonTuple: NotGiven[R <:< Tuple],
  ): Encoder[R] = {
    type Types = r.ElemTypes
    type Labels = r.ElemLabels

    new Encoder[R] {
      final def apply(record: R): Json = {
        val enc = summonInline[Encoder[Map[String, Json]]]
        enc(encodeFields[Types, Labels](r.iterableOf(record).toMap).result())
      }
    }
  }

  inline given decoder[R <: %](using
    r: RecordLike[R],
    nonTuple: NotGiven[R <:< Tuple],
  ): Decoder[R] = {
    type Types = r.ElemTypes
    type Labels = r.ElemLabels

    new Decoder[R] {
      final def apply(c: HCursor): Decoder.Result[R] =
        decodeFields[Types, Labels](c).map { b =>
          Record.newMapRecord[R](b.result())
        }
    }
  }
}
