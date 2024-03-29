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

import scala.annotation.nowarn

import ArrayRecord.{newArrayRecord, unsafeConcat}
import typing.ArrayRecord.{Concat, Lookup}

@nowarn("msg=unused local")
object ArrayRecordMacros {
  import scala.quoted.*
  import InternalMacros.{internal, withInternal, withTyping}

  /** Macro implementation of `ArrayRecord.selectDynamic` */
  def selectImpl[R: Type, L <: String & Singleton: Type](
    record: Expr[ArrayRecord[R]],
    labelExpr: Expr[L],
  )(using Quotes): Expr[Any] = withInternal {
    import internal.*

    evidenceOf[Lookup[R, L]] match {
      case '{
          type index <: Int
          ${ ev }: Lookup[R, L] {
            type Index = `index`
            type Out = out
          }
        } =>
        '{ ArrayRecord.lookup(${ record }, ${ labelExpr })(using ${ ev }) }
    }
  }

  /** Macro implementation of `ArrayRecord.apply` */
  def applyImpl[R: Type](
    record: Expr[ArrayRecord[R]],
    method: Expr[String],
    args: Expr[Seq[Any]],
  )(using Quotes): Expr[Any] = withInternal {
    import internal.*

    requireApply(record, method) {
      val rec = '{ ${ record }.__fields }
      val (fields, schema) = extractFieldsFrom(args)
      val vec = '{ ${ rec }.toVector }

      def tryFieldTypes(tpe: Type[?]): Option[Expr[Any]] =
        tpe match {
          case '[tpe] =>
            Expr.summon[Concat[R, tpe]].map {
              case concat @ '{
                  ${ _ }: Concat[R, tpe] { type Out = returnType }
                } =>
                concat match {
                  // We have to do `match` independently because `NeedDedup` is
                  // not necessarily supplied
                  case '{ ${ _ }: Concat[R, tpe] { type NeedDedup = false } } =>
                    '{
                      newArrayRecord[returnType](${ vec }.concat(${ fields }))
                    }
                  case _ =>
                    '{
                      newArrayRecord[returnType](
                        unsafeConcat(${ vec }, ${ fields }),
                      )
                    }
                }
            }
        }

      tryFieldTypes(schema.asTupleType)
        .orElse(tryFieldTypes(schema.asType))
        .getOrElse {
          schema.asTupleType match {
            case '[tpe] =>
              errorAndAbort(
                s"No given instance of ${Type.show[Concat[R, tpe]]}",
              )
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
            '{ ${ record }.shrinkTo[recType] }
        }
    }
  }

  def derivedRecordLikeImpl[R: Type](using
    Quotes,
  ): Expr[RecordLike[ArrayRecord[R]]] = withInternal {
    import internal.*

    val schema = schemaOfRecord[R]
    val (elemLabels, elemTypes) = schema.asUnzippedTupleType

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
              type Ordered = true
            },
          ]
        }
    }
  }

  def derivedTypingConcatImpl[R1: Type, R2: Type](using
    Quotes,
  ): Expr[Concat[R1, R2]] = withTyping {
    import internal.*

    requireConcreteType[R1]
    requireConcreteType[R2]

    var deduped = false

    val result = catching {
      val schema1 = schemaOf[R1]
      val schema2 = schemaOf[R2]

      val newSchema = (schema1 ++ schema2).deduped
      if (schema1.size + schema2.size != newSchema.size)
        deduped = true
      newSchema.asTupleType
    }

    val needDedupType =
      if (deduped) Type.of[true] else Type.of[false]

    (result, needDedupType) match {
      case (TypingResult('[Nothing], '[err]), _) =>
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
      case (TypingResult('[tpe], '[err]), '[needDedup]) =>
        '{
          Concat
            .instance
            .asInstanceOf[
              Concat[R1, R2] {
                type NeedDedup = needDedup
                type Out = ArrayRecord[tpe]
                type Msg = err
              },
            ]
        }
    }
  }

  def derivedTypingLookupImpl[R: Type, Label: Type](using
    Quotes,
  ): Expr[Lookup[R, Label]] = withInternal {
    import quotes.reflect.*
    import internal.*

    val label =
      TypeRepr.of[Label] match {
        case ConstantType(StringConstant(label)) =>
          label
        case _ =>
          errorAndAbort(
            s"""Invalid type of key.
               |Found:    ${Type.show[Label]}
               |Required: (a literal string)
               |""".stripMargin,
          )
      }

    val schema = schemaOfRecord[R]
    val (tpe, index) = schema.findWithIndex(label).getOrElse {
      errorAndAbort(
        s"Value '${label}' is not a member of ${Type.show[R]}",
      )
    }
    val indexType = ConstantType(IntConstant(index)).asType

    (tpe, indexType) match {
      case ('[tpe], '[index]) =>
        '{
          Lookup
            .instance
            .asInstanceOf[
              Lookup[R, Label] {
                type Out = tpe
                type Index = index
              },
            ]
        }
    }
  }
}
