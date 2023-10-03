package com.github.tarao.record4s

import scala.deriving.Mirror

final class ProductProxy(
  override private[record4s] val __iterable: (String, Any)*,
) extends %
    with Product {
  private lazy val __data: Map[String, Any] = __iterable.toMap

  override private[record4s] def __lookup(key: String): Any = __data(key)

  override def productArity: Int = __iterable.size

  override def productElement(n: Int): Any = __iterable(n)._2

  override def productElementName(n: Int): String = __iterable(n)._1

  override def canEqual(that: Any): Boolean = that.isInstanceOf[ProductProxy]

  override def equals(that: Any): Boolean =
    that match {
      case that: ProductProxy => __iterable == that.__iterable
      case _                  => false
    }
}

object ProductProxy {
  final class ProductProxyMirror[
    P <: ProductProxy,
    ElemTypes,
    ElemLabels <: Tuple,
  ](elemLabels: Seq[String])
      extends Mirror.Product {
    type MirroredMonoType = P
    type MirroredType = P
    type MirroredLabel = "ProductProxy"
    type MirroredElemTypes = ElemTypes
    type MirroredElemLabels = ElemLabels

    def fromProduct(p: scala.Product): P =
      new ProductProxy(elemLabels.zip(p.productIterator): _*).asInstanceOf[P]
  }

  inline given [P <: ProductProxy](using
    r: RecordLike[P],
  ): ProductProxyMirror[P, r.ElemTypes, r.ElemLabels] =
    new ProductProxyMirror(r.elemLabels)

  final class OfRecord[R <: %] {
    type Out <: ProductProxy
  }

  object OfRecord {
    transparent inline given [R <: %]: OfRecord[R] =
      ${ Macros.derivedProductProxyOfRecordImpl }
  }

  inline def from[R <: %](
    record: R,
  )(using typer: OfRecord[R], r: RecordLike[R]): typer.Out =
    new ProductProxy(r.elemLabels.map { label =>
      (label, record.__lookup(label))
    }: _*).asInstanceOf[typer.Out]
}
