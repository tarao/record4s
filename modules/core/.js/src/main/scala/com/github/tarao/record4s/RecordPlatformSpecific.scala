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

import org.getshaka.nativeconverter.{NativeConverter, ParseState}

import scala.collection.mutable.Builder
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.scalajs.js

trait RecordPlatformSpecific {

  /** Construct a record from a JavaScript object.
    *
    * Any nested type `T` can be converted as long as a `NativeConverter[T]`
    * instance is given.
    *
    * This operation is of course unsafe. Missing primitive fields become zeros
    * or nulls and missing object fields yield `java.lang.RuntimeException`.
    *
    * @example
    *   ```
    *   import scala.scalajs.js
    *   val obj: js.Any = js.Dynamic.literal(name = "tarao", age = 3)
    *   val r = Record.fromJS[% { val name: String; val age: Int }](obj)
    *   // val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
    *   ```
    *
    * @param obj
    *   a JavaScript object
    * @param nc
    *   a conversion type class
    * @return
    *   a record
    */
  def fromJS[R <: %](obj: js.Any)(using nc: NativeConverter[R]): R =
    nc.fromNative(obj)

  /** Construct a record from a JSON string.
    *
    * Any nested type `T` can be converted as long as a `NativeConverter[T]`
    * instance is given.
    *
    * This operation is of course unsafe. Missing primitive fields become zeros
    * or nulls and missing object fields yield `java.lang.RuntimeException`.
    *
    * @example
    *   ```
    *   import scala.scalajs.js
    *   val json = """{"name":"tarao","age":3}"""
    *   val r = Record.fromJSON[% { val name: String; val age: Int }](json)
    *   // val r: com.github.tarao.record4s.%{val name: String; val age: Int} = %(name = tarao, age = 3)
    *   ```
    *
    * @param json
    *   a JSON string
    * @param nc
    *   a conversion type class
    * @return
    *   a record
    */
  def fromJSON[R <: %](json: String)(using nc: NativeConverter[R]): R =
    nc.fromJson(json)

  extension [R <: %](record: R) {

    /** Convert this record to a JavaScript object
      *
      * Any nested type `T` can be converted as long as a `NativeConverter[T]`
      * instance is given.
      *
      * @example
      *   ```
      *   val r = %(name = "tarao", age = 3)
      *   val obj = r.toJS
      *   // val obj: scala.scalajs.js.Any = [object Object]
      *   ```
      *
      * @return
      *   a JavaScript object
      */
    def toJS(using NativeConverter[R]): js.Any = record.toNative

    /** Convert this record to a JSON string
      *
      * Any nested type `T` can be converted as long as a `NativeConverter[T]`
      * instance is given.
      *
      * @example
      *   ```
      *   val r = %(name = "tarao", age = 3)
      *   val json = r.toJSON
      *   // val json: String = {"name":"tarao","age":3}
      *   ```
      *
      * @return
      *   a JSON string
      */
    def toJSON(using NativeConverter[R]): String = record.toJson
  }

  private type ImplicitlyJsAny =
    String | Boolean | Byte | Short | Int | Float | Double | Null | js.Any

  private inline def fieldsToNative[Types, Labels](
    record: Map[String, Any],
    res: js.Dynamic = js.Object().asInstanceOf[js.Dynamic],
  ): js.Any =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        res

      case _: (tpe *: types, label *: labels) =>
        val labelStr = constValue[label & String]
        val nativeElem =
          inline erasedValue[tpe] match {
            case _: ImplicitlyJsAny =>
              record(labelStr).asInstanceOf[js.Any]
            case _ =>
              val nc = summonInline[NativeConverter[tpe]]
              val elem = record(labelStr).asInstanceOf[tpe]
              nc.toNative(elem)
          }
        res.updateDynamic(labelStr)(nativeElem)

        fieldsToNative[types, labels](record, res)
    }

  private inline def nativeToFields[Types, Labels](
    dict: js.Dictionary[js.Any],
    ps: ParseState,
    res: Builder[(String, Any), Map[String, Any]] = Map.newBuilder[String, Any],
  ): Builder[(String, Any), Map[String, Any]] =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        res

      case _: (tpe *: types, label *: labels) =>
        val nc = summonInline[NativeConverter[tpe]]
        val labelStr = constValue[label & String]
        val jsElem = dict.getOrElse(labelStr, null)
        val elem = nc.fromNative(ps.atKey(labelStr, jsElem))
        res += (labelStr -> elem)
        nativeToFields[types, labels](dict, ps, res)
    }

  private def asDict(ps: ParseState): js.Dictionary[js.Any] =
    ps.json match {
      case o: js.Object => o.asInstanceOf[js.Dictionary[js.Any]]
      case _            => ps.fail("js.Object")
    }

  inline given nativeConverter[R <: %](using
    r: RecordLike[R],
  ): NativeConverter[R] = {
    type Types = r.ElemTypes
    type Labels = r.ElemLabels

    RecordPlatformSpecificJs.Converter[R](
      (record: R) => fieldsToNative[Types, Labels](r.iterableOf(record).toMap),
      (ps: ParseState) => {
        val iterable = nativeToFields[Types, Labels](asDict(ps), ps).result()
        Record.newMapRecord[R](iterable)
      },
    )
  }
}

object RecordPlatformSpecificJs {
  class Converter[R <: %](
    jsAnyOf: R => js.Any,
    fromParseState: ParseState => R,
  ) extends NativeConverter[R] {
    extension (record: R) {
      def toNative: js.Any = jsAnyOf(record)
    }

    def fromNative(ps: ParseState): R = fromParseState(ps)
  }
}
