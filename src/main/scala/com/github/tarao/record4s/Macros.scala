package com.github.tarao.record4s

object Macros {
  import scala.quoted.*

  /** Macro implementation of `%.apply` */
  def applyImpl[R: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    requireApply(record, method) {
      val (rec, _) = iterableOf(record)

      val fields = args match {
        case Varargs(args) => args
        case _ =>
          report.errorAndAbort("Expected explicit varargs sequence", args)
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
              newMapRecord[returnType]('{ ${ rec } ++ ${ Expr.ofSeq(fields) } })
          }
      }
    }
  }

  def derivedRecordLikeImpl[R <: `%`: Type](using
    Quotes,
  ): Expr[RecordLike[R]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema = schemaOfRecord[R]
    val base = (Type.of[EmptyTuple]: Type[_], Type.of[EmptyTuple]: Type[_])
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

    (elemLabels, elemTypes) match {
      case ('[elemLabels], '[elemTypes]) =>
        '{
          (new Record.RecordLikeRecord[R]).asInstanceOf[
            RecordLike[R] {
              type FieldTypes = R
              type ElemLabels = elemLabels
              type ElemTypes = elemTypes
            },
          ]
        }
    }
  }

  def derivedTypingConcatImpl[R1: Type, R2: Type](using
    Quotes,
  ): Expr[typing.Concat[R1, R2]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema1 = schemaOf[R1]
    val schema2 = schemaOf[R2]

    (schema1 ++ schema2).deduped.asType match {
      case '[tpe] =>
        '{
          typing
            .Concat
            .instance
            .asInstanceOf[
              typing.Concat[R1, R2] {
                type Out = tpe
              },
            ]
        }
    }
  }

  def derivedTypingLookupImpl[R: Type, Label: Type](using
    Quotes,
  ): Expr[typing.Lookup[R, Label]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val valueType = TypeRepr.of[Label] match {
      case ConstantType(StringConstant(label)) =>
        val schema = schemaOfRecord[R]
        schema.fieldTypes.find(_._1 == label).map(_._2).getOrElse {
          report
            .errorAndAbort(s"value ${label} is not a member of ${Type.show[R]}")
        }
      case _ =>
        report.errorAndAbort(
          s"""Found:    ${Type.show[Label]}
             |Required: (a literal string)
             |""".stripMargin,
        )
    }

    valueType match {
      case '[tpe] =>
        '{
          typing
            .Lookup
            .instance
            .asInstanceOf[
              typing.Lookup[R, Label] {
                type Out = tpe
              },
            ]
        }
    }
  }

  def derivedTypingSelectImpl[R: Type, S: Type](using
    Quotes,
  ): Expr[typing.Select[R, S]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema = schemaOf[R]
    val fieldTypes = fieldSelectionsOf[S](schema)

    val newSchema =
      schema.copy(fieldTypes = fieldTypes.map((_, label, tpe) => (label, tpe)))
    newSchema.deduped.asType match {
      case '[tpe] =>
        '{
          typing
            .Select
            .instance
            .asInstanceOf[
              typing.Select[R, S] {
                type Out = tpe
              },
            ]
        }
    }
  }

  def derivedTypingUnselectImpl[R: Type, U <: Tuple: Type](using
    Quotes,
  ): Expr[typing.Unselect[R, U]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema = schemaOf[R]
    val fieldTypes = fieldUnselectionsOf[U](schema)

    val newSchema = schema.copy(fieldTypes = fieldTypes)
    newSchema.deduped.asType match {
      case '[tpe] =>
        '{
          typing
            .Unselect
            .instance
            .asInstanceOf[
              typing.Unselect[R, U] {
                type Out = tpe
              },
            ]
        }
    }
  }

  def derivedProductProxyOfRecordImpl[R <: `%`: Type](using
    Quotes,
  ): Expr[ProductProxy.OfRecord[R]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    schemaOf[R].asType(Type.of[ProductProxy]) match {
      case '[tpe] =>
        '{
          (new ProductProxy.OfRecord).asInstanceOf[
            ProductProxy.OfRecord[R] {
              type Out = tpe
            },
          ]
        }
    }
  }

  private def typeReprOf(
    tpe: Type[_],
  )(using Quotes): quotes.reflect.TypeRepr = {
    import quotes.reflect.*

    tpe match {
      case '[tpe] => TypeRepr.of[tpe]
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
