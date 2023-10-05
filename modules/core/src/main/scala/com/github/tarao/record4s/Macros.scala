package com.github.tarao.record4s

object Macros {
  import scala.quoted.*
  import InternalMacros.{internal, withInternal, withTyping}

  /** Macro implementation of `%.apply` */
  def applyImpl[R: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = withInternal {
    import quotes.reflect.*
    import internal.*

    requireApply(record, method) {
      val (rec, _) = iterableOf(record)

      // We have no way to write this without transparent inline macro.  Literal string
      // types are subject to widening and they become `String`s at the type level.  A
      // `transparent inline given` also doesn't work since it can only depend on
      // type-level information.
      //
      // See the discussion here for the details about attempts to suppress widening:
      // https://contributors.scala-lang.org/t/pre-sip-exact-type-annotation/5835/22
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
              newMapRecord[returnType]('{ ${ rec } ++ ${ Expr.ofSeq(fields) } })
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

  def derivedProductProxyOfRecordImpl[R <: `%`: Type](using
    Quotes,
  ): Expr[ProductProxy.OfRecord[R]] = withInternal {
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

  def derivedTypingConcatImpl[R1: Type, R2: Type](using
    Quotes,
  ): Expr[typing.Concat[R1, R2]] = withTyping {
    import internal.*

    val result = catching {
      val schema1 = schemaOf[R1]
      val schema2 = schemaOf[R2]

      (schema1 ++ schema2).deduped.asType
    }

    result match {
      case TypingResult('[tpe], '[err]) =>
        '{
          typing
            .Concat
            .instance
            .asInstanceOf[
              typing.Concat[R1, R2] {
                type Out = tpe
                type Msg = err
              },
            ]
        }
    }
  }

  def derivedTypingLookupImpl[R: Type, Label: Type](using
    Quotes,
  ): Expr[typing.Lookup[R, Label]] = withTyping {
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
          typing
            .Lookup
            .instance
            .asInstanceOf[
              typing.Lookup[R, Label] {
                type Out = tpe
                type Msg = err
              },
            ]
        }
    }
  }

  def derivedTypingSelectImpl[R: Type, S: Type](using
    Quotes,
  ): Expr[typing.Select[R, S]] = withTyping {
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
          typing
            .Select
            .instance
            .asInstanceOf[
              typing.Select[R, S] {
                type Out = tpe
                type Msg = err
              },
            ]
        }
    }
  }

  def derivedTypingUnselectImpl[R: Type, U <: Tuple: Type](using
    Quotes,
  ): Expr[typing.Unselect[R, U]] = withTyping {
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
          typing
            .Unselect
            .instance
            .asInstanceOf[
              typing.Unselect[R, U] {
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
