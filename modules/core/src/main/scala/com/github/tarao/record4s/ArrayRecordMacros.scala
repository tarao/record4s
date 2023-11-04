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
    import internal.*

    val rec = '{ ${ record }.__fields }
    val schema = schemaOf[R]

    Macros.genericApplyImpl(record, method, args) {
      [Out] =>
        (tpe: Type[Out]) =>
          (fields: Expr[Seq[(String, Any)]]) => {
            val newSchema = schemaOfRecord[Out](using tpe).copy(
              // `Out` somehow drops tags in `R`
              tags = schema.tags,
            )
            newSchema.asTupleType match {
              case '[recType] =>
                '{
                  new VectorRecord(
                    ${ rec }
                      .toVector
                      .concat(${ fields })
                      .deduped
                      .iterator
                      .toVector,
                  ).asInstanceOf[ArrayRecord[recType]]
                }
            }
        }
    }
  }

  def upcastImpl[From: Type, To: Type](
    record: Expr[ArrayRecord[From]],
  )(using Quotes): Expr[Any] = withInternal {
    import quotes.reflect.*
    import internal.*

    // This can be written without macro but that will drop tags.

    val t1 = evidenceOf[typing.Concat[%, From]]
    val t2 = evidenceOf[typing.Concat[%, To]]

    (t1, t2) match {
      case (
          '{ ${ _ }: typing.Concat[%, From] { type Out = r1 } },
          '{ ${ _ }: typing.Concat[%, To] { type Out = r2 } },
        ) =>
        val tpr1 = TypeRepr.of[r1]
        val tpr2 = TypeRepr.of[r2]

        if (!(tpr1 <:< tpr2))
          errorAndAbort(
            s"${Type.show[To]}' is not a subset of ${Type.show[From]}",
          )

        val schema = schemaOf[To]
        schema.asTupleType match {
          case '[recType] =>
            val recordLike = evidenceOf[RecordLike[ArrayRecord[recType]]]
            '{
              new VectorRecord(
                ${ recordLike }
                  .orderedIterableOf(${ record }
                    .asInstanceOf[ArrayRecord[recType]])
                  .toVector,
              ).asInstanceOf[ArrayRecord[recType]]
            }
        }
    }
  }

  def derivedRecordLikeImpl[R: Type](using
    Quotes,
  ): Expr[RecordLike[ArrayRecord[R]]] = withInternal {
    import quotes.reflect.*
    import internal.*

    val schema = schemaOfRecord[R]
    val base = (Type.of[EmptyTuple]: Type[?], Type.of[EmptyTuple]: Type[?])
    val (elemLabels, elemTypes) = schema
      .fieldTypes
      .foldRight(base) { case ((label, tpe), (baseLabels, baseTypes)) =>
        val labels =
          (baseLabels, ConstantType(StringConstant(label)).asType) match {
            case ('[EmptyTuple], '[label])   => Type.of[label *: EmptyTuple]
            case ('[head *: tail], '[label]) => Type.of[label *: head *: tail]
          }
        val types = (baseTypes, tpe) match {
          case ('[EmptyTuple], '[tpe])   => Type.of[tpe *: EmptyTuple]
          case ('[head *: tail], '[tpe]) => Type.of[tpe *: head *: tail]
        }
        (labels, types)
      }

    (elemLabels, elemTypes, schema.tagsAsType, schema.asTupleType) match {
      case ('[elemLabels], '[elemTypes], '[tagsType], '[tupleType]) =>
        '{
          (new ArrayRecord.RecordLikeArrayRecord[R]).asInstanceOf[
            RecordLike[ArrayRecord[R]] {
              type FieldTypes = R
              type ElemLabels = elemLabels
              type ElemTypes = elemTypes
              type Tags = tagsType
              type TupledFieldTypes = tupleType
            },
          ]
        }
    }
  }
}
