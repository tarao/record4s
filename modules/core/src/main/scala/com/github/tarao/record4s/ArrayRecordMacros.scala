package com.github.tarao.record4s

import typing.ArrayRecord.Concat
import util.SeqOps.deduped

object ArrayRecordMacros {
  import scala.quoted.*
  import InternalMacros.{internal, withInternal, withTyping}

  /** Macro implementation of `ArrayRecord.lookup` */
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

  /** Macro implementation of `ArrayRecord.apply` */
  def applyImpl[R: Type](
    record: Expr[ArrayRecord[R]],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = withInternal {
    import internal.*

    requireApply(record, method) {
      val rec = '{ ${ record }.__fields }
      val (fields, tpe) = Macros.extractFieldsFrom(args)

      tpe match {
        case '[tpe] =>
          evidenceOf[Concat[R, tpe]] match {
            case '{ ${ _ }: Concat[R, tpe] { type Out = returnType } } =>
              '{
                new VectorRecord(
                  ${ rec }
                    .toVector
                    .concat(${ fields })
                    .deduped
                    .iterator
                    .toVector,
                ).asInstanceOf[returnType]
              }
          }
      }
    }
  }

  /** Macro implementation of `ArrayRecord.upcast` */
  def upcastImpl[From: Type, To: Type](
    record: Expr[ArrayRecord[From]],
  )(using Quotes): Expr[Any] = withInternal {
    import quotes.reflect.*
    import internal.*

    // This can be written without macro but that will drop tags.

    val t1 = evidenceOf[typing.Record.Concat[%, From]]
    val t2 = evidenceOf[typing.Record.Concat[%, To]]

    (t1, t2) match {
      case (
          '{ ${ _ }: typing.Record.Concat[%, From] { type Out = r1 } },
          '{ ${ _ }: typing.Record.Concat[%, To] { type Out = r2 } },
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

  def derivedTypingConcatImpl[R1: Type, R2: Type](using
    Quotes,
  ): Expr[Concat[R1, R2]] = withTyping {
    import internal.*

    val result = catching {
      val schema1 = schemaOf[R1]
      val schema2 = schemaOf[R2]

      (schema1 ++ schema2).deduped.asTupleType
    }

    result match {
      case TypingResult('[Nothing], '[err]) =>
        '{
          Concat
            .instance
            .asInstanceOf[
              Concat[R1, R2] {
                type Out = Nothing
                type Msg = err
              },
            ]
        }
      case TypingResult('[tpe], '[err]) =>
        '{
          Concat
            .instance
            .asInstanceOf[
              Concat[R1, R2] {
                type Out = ArrayRecord[tpe]
                type Msg = err
              },
            ]
        }
    }
  }
}
