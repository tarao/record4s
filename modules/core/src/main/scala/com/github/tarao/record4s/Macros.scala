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

import Record.newMapRecord
import typing.Concrete
import typing.Record.{Concat, Lookup, Select, Unselect}

@nowarn("msg=unused local")
object Macros {
  import scala.quoted.*
  import InternalMacros.{internal, withInternal, withTyping}

  /** Macro implementation of `%.apply` */
  def applyImpl[R <: `%`: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[Any]],
  )(using Quotes): Expr[Any] = withInternal {
    import internal.*

    requireApply(record, method) {
      val rec = '{ ${ record }.__iterable }
      val (fields, schema) = extractFieldsFrom(args)

      def tryFieldTypes(tpe: Type[?]): Option[Expr[Any]] =
        tpe match {
          case '[tpe] =>
            Expr.summon[Concat[R, tpe]].map {
              case '{
                  type returnType <: %
                  ${ _ }: Concat[R, tpe] { type Out = `returnType` }
                } =>
                '{
                  newMapRecord[returnType](${ rec }.toMap.concat(${ fields }))
                }
            }
        }

      tryFieldTypes(schema.asType)
        .orElse(tryFieldTypes(schema.asTupleType))
        .getOrElse {
          schema.asType match {
            case '[tpe] =>
              errorAndAbort(
                s"No given instance of ${Type.show[Concat[R, tpe]]}",
              )
          }
        }
    }
  }

  def derivedRecordLikeImpl[R <: `%`: Type](using
    Quotes,
  ): Expr[RecordLike[R]] = withInternal {
    import internal.*

    val schema = schemaOfRecord[R]
    val (elemLabels, elemTypes) = schema.asUnzippedTupleType

    (elemLabels, elemTypes, schema.tagsAsType, schema.asTupleType) match {
      case ('[elemLabels], '[elemTypes], '[tagsType], '[tupleType]) =>
        '{
          (new Record.RecordLikeRecord[R]).asInstanceOf[
            RecordLike[R] {
              type FieldTypes = R
              type ElemLabels = elemLabels
              type ElemTypes = elemTypes
              type Tags = tagsType
              type TupledFieldTypes = tupleType
              type Ordered = false
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

    val result = catching {
      val schema1 = schemaOf[R1]
      val schema2 = schemaOf[R2]

      (schema1 ++ schema2).deduped.asType
    }

    result match {
      case TypingResult('[tpe], '[err]) =>
        '{
          Concat
            .instance
            .asInstanceOf[
              Concat[R1, R2] {
                type Out = tpe
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

    val result =
      TypeRepr.of[Label] match {
        case ConstantType(StringConstant(label)) =>
          val schema = schemaOfRecord[R]
          schema.find(label).getOrElse {
            errorAndAbort(
              s"Value '${label}' is not a member of ${Type.show[R]}",
            )
          }
        case _ =>
          errorAndAbort(
            s"""Invalid type of key.
               |Found:    ${Type.show[Label]}
               |Required: (a literal string)
               |""".stripMargin,
          )
      }

    result match {
      case '[tpe] =>
        '{
          Lookup
            .instance
            .asInstanceOf[
              Lookup[R, Label] {
                type Out = tpe
              },
            ]
        }
    }
  }

  def derivedTypingSelectImpl[R: Type, S: Type](using
    Quotes,
  ): Expr[Select[R, S]] = withTyping {
    import internal.*

    val result = catching {
      val schema = selectedSchemaOf[R, S]
      schema.deduped.asType
    }

    result match {
      case TypingResult('[tpe], '[err]) =>
        '{
          Select
            .instance
            .asInstanceOf[
              Select[R, S] {
                type Out = tpe
                type Msg = err
              },
            ]
        }
    }
  }

  def derivedTypingUnselectImpl[R <: `%`: Type, U <: Tuple: Type](using
    Quotes,
  ): Expr[Unselect[R, U]] = withTyping {
    import internal.*

    val result = catching {
      val schema = unselectedSchemaOf[R, U]
      schema.deduped.asType
    }

    result match {
      case TypingResult('[tpe], '[err]) =>
        '{
          Unselect
            .instance
            .asInstanceOf[
              Unselect[R, U] {
                type Out = tpe
                type Msg = err
              },
            ]
        }
    }
  }

  def derivedTypingConcreteImple[T: Type](using
    Quotes,
  ): Expr[Concrete[T]] = withInternal {
    import internal.*

    requireConcreteType[T]

    '{
      Concrete
        .instance
        .asInstanceOf[Concrete[T]]
    }
  }

  private def typeNameOfImpl[T: Type](using Quotes): Expr[String] = {
    import quotes.reflect.*

    val typeName = TypeRepr.of[T].show(using Printer.TypeReprShortCode)
    Expr(typeName)
  }

  inline def typeNameOf[T]: String = ${ typeNameOfImpl[T] }
}
