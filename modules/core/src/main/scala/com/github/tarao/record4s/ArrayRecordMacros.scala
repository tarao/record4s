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
    import quotes.reflect.*
    import internal.*

    requireApply(record, method) {
      val rec = '{ ${ record }.__fields }

      val fields = args match {
        case Varargs(args) => args
        case _ =>
          errorAndAbort("Expected explicit varargs sequence", Some(args))
      }
      val fieldTypes = fieldTypesOf(fields)

      val fieldTypesTuple =
        typeReprOfTupleFromSeq(fieldTypes.map { case (label, '[tpe]) =>
          ConstantType(StringConstant(label)).asType match {
            case '[label] => TypeRepr.of[(label, tpe)]
          }
        }).asType

      fieldTypesTuple match {
        case '[tpe] =>
          evidenceOf[typing.Concat[R, tpe]] match {
            case '{ ${ _ }: typing.Concat[R, tpe] { type Out = returnType } } =>
              '{
                new VectorRecord[returnType](
                  ${ rec }
                    .toVector
                    .concat(${ Expr.ofSeq(fields) })
                    .deduped
                    .iterator
                    .toVector,
                ): ArrayRecord[returnType]
              }
          }
      }
    }
  }

  private def typeReprOfTupleFromSeq(using Quotes)(
    typeReprs: Seq[quotes.reflect.TypeRepr],
  ): quotes.reflect.TypeRepr = {
    import quotes.reflect.*

    typeReprs.foldRight(TypeRepr.of[EmptyTuple]) { case (tpr, base) =>
      (base.asType, tpr.asType) match {
        case ('[head *: tail], '[tpe]) =>
          TypeRepr.of[tpe *: head *: tail]
        case ('[EmptyTuple], '[tpe]) =>
          TypeRepr.of[tpe *: EmptyTuple]
      }
    }
  }
}
