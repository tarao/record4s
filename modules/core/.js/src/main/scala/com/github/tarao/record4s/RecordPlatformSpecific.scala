package com.github.tarao.record4s

import org.getshaka.nativeconverter.{NativeConverter, ParseState}
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.scalajs.js

trait RecordPlatformSpecific {
  def fromJS[R <: %](obj: js.Any)(using nc: NativeConverter[R]): R =
    nc.fromNative(obj)

  def fromJSON[R <: %](json: String)(using nc: NativeConverter[R]): R =
    nc.fromJson(json)

  extension [R <: %](record: R) {
    def toJS(using NativeConverter[R]): js.Any = record.toNative

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
    res: Seq[(String, Any)] = Seq.empty,
  ): Seq[(String, Any)] =
    inline (erasedValue[Types], erasedValue[Labels]) match {
      case _: (EmptyTuple, EmptyTuple) =>
        res

      case _: (tpe *: types, label *: labels) =>
        val nc = summonInline[NativeConverter[tpe]]
        val labelStr = constValue[label & String]
        val jsElem = dict.getOrElse(labelStr, null)
        val elem = nc.fromNative(ps.atKey(labelStr, jsElem))
        nativeToFields[types, labels](dict, ps, res :+ (labelStr, elem))
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

    new NativeConverter[R] {
      extension (record: R) {
        def toNative: js.Any =
          fieldsToNative[Types, Labels](r.iterableOf(record).toMap)
      }

      def fromNative(ps: ParseState): R = {
        val iterable = nativeToFields[Types, Labels](asDict(ps), ps)
        Record.newMapRecord[R](iterable)
      }
    }
  }
}
