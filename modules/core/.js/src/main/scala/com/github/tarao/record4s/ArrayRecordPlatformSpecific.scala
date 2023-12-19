package com.github.tarao.record4s

import org.getshaka.nativeconverter.NativeConverter
import scala.scalajs.js

trait ArrayRecordPlatformSpecific {
  def fromJS[T <: Tuple](obj: js.Any)(using
    nc: NativeConverter[ArrayRecord[T]],
  ): ArrayRecord[T] = nc.fromNative(obj)

  def fromJSON[T <: Tuple](json: String)(using
    nc: NativeConverter[ArrayRecord[T]],
  ): ArrayRecord[T] = nc.fromJson(json)

  extension [R](record: ArrayRecord[R]) {
    def toJS(using NativeConverter[ArrayRecord[R]]): js.Any = record.toNative

    def toJSON(using NativeConverter[ArrayRecord[R]]): String = record.toJson
  }

  inline given nativeConverter[R]: NativeConverter[ArrayRecord[R]] =
    NativeConverter.derived
}
