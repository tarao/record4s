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
package ujson

import upickle.core.ObjVisitor

import upickle.core.ArrVisitor
import upickle.default.Writer
import upickle.core.Visitor
// import _root_.ujson.Value
// import _root_.ujson.AstTransformer

// import upickle.default.{ReadWriter, Reader, reader}

object ReadWriter {
  given reader[R <: ArrayRecord[_]]
  // (using
  //   ar: typing.ArrayRecord.Aux[R, RR],
  //   enc: upickle.default.Reader[RR],
  // )
  : upickle.default.Reader[R] =

    new upickle.default.Reader[R] {

      override def visitArray(length: Int, index: Int): ArrVisitor[Any, R] = ???

      override def visitFloat32(d: Float, index: Int): R = ???

      override def visitUInt64(i: Long, index: Int): R = ???

      override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): R = ???

      override def visitInt32(i: Int, index: Int): R = ???

      override def visitFloat64(d: Double, index: Int): R = ???

      override def visitChar(s: Char, index: Int): R = ???

      override def visitString(s: CharSequence, index: Int): R = ???

      override def visitObject(length: Int, jsonableKeys: Boolean, index: Int): ObjVisitor[Any, R] = ???

      override def visitFalse(index: Int): R = ???

      override def visitFloat64String(s: String, index: Int): R = ???

      override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): R = ???

      override def visitInt64(i: Long, index: Int): R = ???

      override def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): R = ???

      override def visitNull(index: Int): R = ???

      override def visitTrue(index: Int): R = ???


    }
    // reader[RR].map(x => ArrayRecord.from(x)(enc))

  given writer[R <: ArrayRecord[_]]
  // (using
  //   r: RecordLike[R],
  //   dec: Writer[ArrayRecord[r.TupledFieldTypes]],
  //   c: typing.Record.Concat[%, ArrayRecord[r.TupledFieldTypes]],
  //   ev: c.Out =:= R,
  // )
  : Writer[R] = new Writer[R] {
    override def write0[V](out: Visitor[?, V], v: R): V = ???
  }
}
