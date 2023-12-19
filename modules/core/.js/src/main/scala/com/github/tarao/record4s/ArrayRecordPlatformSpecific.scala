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

import org.getshaka.nativeconverter.NativeConverter

import scala.scalajs.js

trait ArrayRecordPlatformSpecific {

  /** Construct an array record from a JavaScript object.
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
    *   val r = ArrayRecord.fromJS[(("name", String), ("age", Int))](obj)
    *   // val r: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
    *   ```
    *
    * @param obj
    *   a JavaScript object
    * @param nc
    *   a conversion type class
    * @return
    *   an array record
    */
  def fromJS[T <: Tuple](obj: js.Any)(using
    nc: NativeConverter[ArrayRecord[T]],
  ): ArrayRecord[T] = nc.fromNative(obj)

  /** Construct an array record from a JSON string.
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
    *   val r = ArrayRecord.fromJSON[(("name", String), ("age", Int))](json)
    *   // val r: com.github.tarao.record4s.ArrayRecord[(("name", String), ("age", Int))] = ArrayRecord(name = tarao, age = 3)
    *   ```
    *
    * @param json
    *   a JSON string
    * @param nc
    *   a conversion type class
    * @return
    *   an array record
    */
  def fromJSON[T <: Tuple](json: String)(using
    nc: NativeConverter[ArrayRecord[T]],
  ): ArrayRecord[T] = nc.fromJson(json)

  extension [R](record: ArrayRecord[R]) {

    /** Convert this array record to a JavaScript object
      *
      * Any nested type `T` can be converted as long as a `NativeConverter[T]`
      * instance is given.
      *
      * @example
      *   ```
      *   val r = ArrayRecord(name = "tarao", age = 3)
      *   val obj = r.toJS
      *   // val obj: scala.scalajs.js.Any = [object Object]
      *   ```
      *
      * @return
      *   a JavaScript object
      */
    def toJS(using NativeConverter[ArrayRecord[R]]): js.Any = record.toNative

    /** Convert this array record to a JSON string
      *
      * Any nested type `T` can be converted as long as a `NativeConverter[T]`
      * instance is given.
      *
      * @example
      *   ```
      *   val r = ArrayRecord(name = "tarao", age = 3)
      *   val json = r.toJSON
      *   // val json: String = {"name":"tarao","age":3}
      *   ```
      *
      * @return
      *   a JSON string
      */
    def toJSON(using NativeConverter[ArrayRecord[R]]): String = record.toJson
  }

  inline given nativeConverter[R]: NativeConverter[ArrayRecord[R]] =
    NativeConverter.derived
}
