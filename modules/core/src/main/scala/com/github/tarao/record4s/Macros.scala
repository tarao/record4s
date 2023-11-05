package com.github.tarao.record4s

import typing.Record.{Concat, Lookup, Select, Unselect}

object Macros {
  import scala.quoted.*
  import InternalMacros.{internal, withInternal, withTyping}

  def extractFieldsFrom(
    varargs: Expr[Seq[(String, Any)]],
  )(using Quotes): (Expr[Seq[(String, Any)]], Type[?]) = withInternal {
    import quotes.reflect.*
    import internal.*

    // We have no way to write this without transparent inline macro.  Literal string
    // types are subject to widening and they become `String`s at the type level.  A
    // `transparent inline given` also doesn't work since it can only depend on type-level
    // information.
    //
    // See the discussion here for the details about attempts to suppress widening:
    // https://contributors.scala-lang.org/t/pre-sip-exact-type-annotation/5835/22
    val fields = varargs match {
      case Varargs(args) => args
      case _ =>
        errorAndAbort("Expected explicit varargs sequence", Some(varargs))
    }
    val fieldTypes = fieldTypesOf(fields)

    val fieldTypesTuple =
      typeReprOfTupleFromSeq(fieldTypes.map { case (label, '[tpe]) =>
        ConstantType(StringConstant(label)).asType match {
          case '[label] => TypeRepr.of[(label, tpe)]
        }
      }).asType

    (Expr.ofSeq(fields), fieldTypesTuple)
  }

  /** Macro implementation of `%.apply` */
  def applyImpl[R <: `%`: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = withInternal {
    import internal.*

    requireApply(record, method) {
      val rec = '{ ${ record }.__iterable }
      val (fields, tpe) = extractFieldsFrom(args)

      tpe match {
        case '[tpe] =>
          evidenceOf[Concat[R, tpe]] match {
            case '{ ${ _ }: Concat[R, tpe] { type Out = returnType } } =>
              newMapRecord[returnType]('{
                ${ rec }.toMap.concat(${ fields })
              })
          }
      }
    }
  }

  def derivedRecordLikeImpl[R <: `%`: Type](using
    Quotes,
  ): Expr[RecordLike[R]] = withInternal {
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
  ): Expr[Lookup[R, Label]] = withTyping {
    import quotes.reflect.*
    import internal.*

    val result = catching {
      TypeRepr.of[Label] match {
        case ConstantType(StringConstant(label)) =>
          val schema = schemaOfRecord[R]
          schema.fieldTypes.find(_._1 == label).map(_._2).getOrElse {
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
    }

    result match {
      case TypingResult('[tpe], '[err]) =>
        '{
          Lookup
            .instance
            .asInstanceOf[
              Lookup[R, Label] {
                type Out = tpe
                type Msg = err
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
      val schema = schemaOf[R]
      val fieldTypes = fieldSelectionsOf[S](schema)

      val newSchema =
        schema.copy(fieldTypes =
          fieldTypes.map((_, label, tpe) => (label, tpe)),
        )
      newSchema.deduped.asType
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

  def derivedTypingUnselectImpl[R: Type, U <: Tuple: Type](using
    Quotes,
  ): Expr[Unselect[R, U]] = withTyping {
    import internal.*

    val result = catching {
      val schema = schemaOf[R]
      val fieldTypes = fieldUnselectionsOf[U](schema)

      val newSchema = schema.copy(fieldTypes = fieldTypes)
      newSchema.deduped.asType
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

  private def typeNameOfImpl[T: Type](using Quotes): Expr[String] =
    Expr(Type.show[T])

  inline def typeNameOf[T]: String = ${ typeNameOfImpl[T] }

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
