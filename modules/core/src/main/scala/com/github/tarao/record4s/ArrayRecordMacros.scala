package com.github.tarao.record4s

import util.SeqOps.deduped

object ArrayRecordMacros {
  import scala.quoted.*
  import InternalMacros.{internal, withInternal}

  def selectImpl[R: Type](
    record: Expr[ArrayRecord[R]],
    labelExpr: Expr[String],
  )(using Quotes): Expr[Any] = withInternal {
    import quotes.reflect.*
    import internal.*

    val label = labelExpr.asTerm match {
      case Inlined(_, _, Literal(StringConstant(label))) =>
        label
      case _ =>
        errorAndAbort("Field label must be a literal string", Some(labelExpr))
    }

    val schema = schemaOf[R]
    schema.fieldTypes.zipWithIndex.find { case ((key, _), _) =>
      key == label
    } match {
      case Some((_, '[tpe]), index) =>
        '{ ${ record }.__fields(${ Expr(index) })._2.asInstanceOf[tpe] }
      case _ =>
        errorAndAbort(
          s"Value '${label}' is not a member of ArrayRecord[${Type.show[R]}]",
          Some(labelExpr),
        )
    }
  }

  def applyImpl[R: Type](
    record: Expr[ArrayRecord[R]],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = withInternal {
    val rec = '{ ${ record }.__fields }

    Macros.genericApplyImpl(record, method, args) {
      [Out] =>
        (tpe: Type[Out]) =>
          (fields: Expr[Seq[(String, Any)]]) => {
            given Type[Out] = tpe
            '{
              new VectorRecord[Out](
                ${ rec }
                  .toVector
                  .concat(${ fields })
                  .deduped
                  .iterator
                  .toVector,
              ): ArrayRecord[Out]
            }
        }
    }
  }
}
