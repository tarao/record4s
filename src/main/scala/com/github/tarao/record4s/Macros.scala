package com.github.tarao.record4s

object Macros {
  import scala.quoted.*

  /** Macro implementation of `Record.lookup` */
  def lookupImpl[R <: `%`: Type](
    record: Expr[R],
    label: Expr[String],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    label.asTerm match {
      case Inlined(_, _, Literal(StringConstant(label))) =>
        val schema = schemaOfRecord[R]
        val valueType =
          schema.fieldTypes.find(_._1 == label).map(_._2).getOrElse {
            report
              .errorAndAbort(
                s"value ${label} is not a member of ${Type.show[R]}",
              )
          }

        valueType match {
          case '[tpe] =>
            '{ ${ record }.__data(${ Expr(label) }).asInstanceOf[tpe] }
        }
      case _ =>
        report.errorAndAbort("label must be a literal string", label)
    }
  }

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

  /** Macro implementation of `%().apply` */
  def selectImpl[R <: `%`: Type, S <: Tuple: Type, RR <: `%`: Type](
    record: Expr[R],
    s: Expr[Selector[S]],
  )(using Quotes): Expr[RR] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema = schemaOf[R]
    val fieldTypes = fieldSelectionsOf[S](schema)

    val args = fieldTypes.map { (label, newLabel, _) =>
      '{ (${ Expr(newLabel) }, Record.lookup(${ record }, ${ Expr(label) })) }
    }

    '{ %(${ Expr.ofSeq(args) }: _*).asInstanceOf[RR] }
  }

  /** Macro implementation of `%.to` */
  def toProductImpl[R <: `%`: Type, P <: Product: Type](
    record: Expr[R],
  )(using Quotes): Expr[P] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema = schemaOfRecord[R].fieldTypes.toMap
    val fieldTypes = schemaOf(evidenceOf[RecordLike[P]]).fieldTypes

    // type check
    for ((label, tpe) <- fieldTypes) {
      val valueTpe = schema.getOrElse(
        label,
        report.errorAndAbort(s"Missing key '${label}'", record),
      )
      if (!(typeReprOf(valueTpe) <:< typeReprOf(tpe))) {
        report.errorAndAbort(
          s"""Found:    (${record.show}.${label}: ${typeReprOf(valueTpe).show})
             |Required: ${typeReprOf(tpe).show}
             |""".stripMargin,
          record,
        )
      }
    }

    val term = record.asTerm
    val args = fieldTypes.map { case (label, _) =>
      '{ Record.lookup(${ record }, ${ Expr(label) }) }.asTerm
    }

    val sym = TypeRepr.of[P].typeSymbol
    val companion = sym.companionClass
    val method = companion.declarations.find(_.name == "apply").getOrElse {
      report.errorAndAbort(s"${Type.show[P]} has no `apply` method")
    }

    Ref(sym.companionModule)
      .select(method)
      .appliedToArgs(args.toList)
      .asExprOf[P]
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
