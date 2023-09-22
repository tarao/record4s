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
          evidenceOf[Typing.Concat[R, tpe]] match {
            case '{ ${ _ }: Typing.Concat[R, tpe] { type Out = returnType } } =>
              newMapRecord[returnType]('{ ${ rec } ++ ${ Expr.ofSeq(fields) } })
          }
      }
    }
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
    val elemLabels = schema
      .fieldTypes
      .foldRight(Type.of[EmptyTuple]: Type[_]) { case ((label, _), base) =>
        (base, ConstantType(StringConstant(label)).asType) match {
          case ('[EmptyTuple], '[label])   => Type.of[label *: EmptyTuple]
          case ('[head *: tail], '[label]) => Type.of[label *: head *: tail]
        }
      }

    elemLabels match {
      case '[elemLabels] =>
        '{
          (new Record.RecordLikeRecord[R]).asInstanceOf[
            RecordLike[R] {
              type FieldTypes = R
              type ElemLabels = elemLabels
            },
          ]
        }
    }
  }

  def derivedTypingConcatImpl[R1: Type, R2: Type](using
    Quotes,
  ): Expr[Typing.Concat[R1, R2]] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema1 = schemaOf[R1]
    val schema2 = schemaOf[R2]

    (schema1 ++ schema2).deduped._1.asType match {
      case '[tpe] =>
        '{
          (new Typing.Concat).asInstanceOf[
            Typing.Concat[R1, R2] {
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
