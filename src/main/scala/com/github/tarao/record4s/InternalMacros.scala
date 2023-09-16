package com.github.tarao.record4s

import scala.annotation.tailrec

private[record4s] class InternalMacros(using scala.quoted.Quotes) {
  import scala.quoted.*
  import quotes.reflect.*

  def validatedLabel(
    label: String,
    context: Option[Expr[Any]] = None,
  ): String = {
    def errorAndAbort(msg: String, context: Option[Expr[Any]]): Nothing =
      context match {
        case Some(expr) =>
          report.errorAndAbort(msg, expr)
        case None =>
          report.errorAndAbort(msg)
      }

    if (label.isEmpty)
      errorAndAbort(
        "Field label must be a non-empty string",
        context,
      )
    else if (label.contains("$"))
      // We can't allow "$" because
      // - (1) Scala compiler passes encoded name to `selectDynamic`, and
      // - (2) "$" itself never gets encoded i.e. we can't distinguish for example between
      //       "$minus-" and "--" (both are encoded to "$minus$minus").
      errorAndAbort(
        "'$' cannot be used as field label",
        context,
      )
    else
      label
  }

  def evidenceOf[T: Type]: Expr[T] =
    Expr.summon[T].getOrElse {
      report.errorAndAbort(
        s"No given instance of ${Type.show[T]}",
      )
    }

  def schemaOf[R: Type]: Seq[(String, Type[_])] = {
    @tailrec def collectTupledFieldTypes(
      tpe: Type[_],
      acc: Seq[(String, Type[_])],
    ): Seq[(String, Type[_])] = tpe match {
      case '[(labelType, valueType) *: rest] =>
        TypeRepr.of[labelType] match {
          case ConstantType(StringConstant(label)) =>
            collectTupledFieldTypes(
              Type.of[rest],
              acc :+ (validatedLabel(label), Type.of[valueType]),
            )
          case _ =>
            collectTupledFieldTypes(Type.of[rest], acc)
        }
      case _ =>
        acc
    }

    @tailrec def collectFieldTypes(
      reversed: List[TypeRepr],
      acc: Seq[(String, Type[_])],
    ): Seq[(String, Type[_])] = reversed match {
      // base { label: valueType }
      // For example
      //   TypeRepr.of[%{val name: String; val age: Int}]
      // is
      //   Refinement(
      //     Refinement(
      //       TypeRepr.of[%],
      //       "name",
      //       TypeRepr.of[String]
      //     ),
      //     "age",
      //     TypeRepr.of[Int]
      //   )
      case Refinement(base, label, valueType) :: rest =>
        collectFieldTypes(
          base :: rest,
          (validatedLabel(label), valueType.asType) +: acc,
        )

      // tpr1 & tpr2
      case AndType(tpr1, tpr2) :: rest =>
        collectFieldTypes(tpr2 :: tpr1 :: rest, acc)

      // typically `%` in `% { ... }` or
      // (tp1, ...)
      // tp1 *: ...
      case head :: rest =>
        collectFieldTypes(
          rest,
          collectTupledFieldTypes(head.asType, Seq.empty) ++ acc,
        )

      // all done
      case Nil =>
        acc
    }
    // Non-tailrec equivalent:
    //   def collectFieldTypesNonTailRec(tpr: TypeRepr): Seq[(String, TypeRepr)] =
    //     tpr match {
    //       case Refinement(base, label, valueType) =>
    //         (label, valueType) +: collectFieldTypesNonTailRec(base)
    //       case AndType(tpr1, tpr2) =>
    //         collectFieldTypesNonTailRec(tpr1) ++ collectFieldTypesNonTailRec(tpr2)
    //       case _ =>
    //         Seq.empty
    //     }

    collectFieldTypes(List(TypeRepr.of[R]), Seq.empty)
  }

  def fieldTypesOf(
    fields: Seq[Expr[(String, Any)]],
  ): Seq[(String, Type[_])] = {
    def fieldTypeOf(
      labelExpr: Expr[Any],
      valueExpr: Expr[Any],
    ): (String, Type[_]) = {
      val label = labelExpr.asTerm match {
        case Literal(StringConstant(label)) =>
          validatedLabel(label, Some(labelExpr))
        case _ =>
          report.errorAndAbort(
            "Field label must be a literal string",
            labelExpr,
          )
      }
      val tpe = valueExpr match {
        case '{ ${ _ }: tp } => TypeRepr.of[tp].widen.asType
      }
      (label, tpe)
    }

    fields.map {
      // ("label", value)
      case '{ ($labelExpr, $valueExpr) } =>
        fieldTypeOf(labelExpr, valueExpr)

      // "label" -> value
      case '{ ArrowAssoc(${ labelExpr }: String).->(${ valueExpr }) } =>
        fieldTypeOf(labelExpr, valueExpr)

      case expr =>
        report.errorAndAbort(s"Invalid field", expr)
    }
  }

  def fieldTypesOf[R: Type](
    recordLike: Expr[RecordLike[R]],
  ): Seq[(String, Type[_])] =
    recordLike match {
      case '{ ${ _ }: RecordLike[R] { type FieldTypes = fieldTypes } } =>
        schemaOf[fieldTypes]
    }

  def iterableOf[R: Type](
    record: Expr[R],
  ): (Expr[Iterable[(String, Any)]], Seq[(String, Type[_])]) = {
    val ev = evidenceOf[RecordLike[R]]
    val schema = fieldTypesOf(ev)
    ('{ ${ ev }.iterableOf($record) }, schema)
  }

  def tidiedIterableOf[R: Type](
    record: Expr[R],
  ): (Expr[Iterable[(String, Any)]], Seq[(String, Type[_])]) = {
    val (rec, schema) = iterableOf(record)

    // Generates:
    //   {
    //     val keys = Set(${schema(0)._1}, ${schema(1)._1}, ...)
    //     ${rec}.filter { case (key, _) => keys.contains(key) }
    //   }
    val keysExpr = schema.map(field => Expr(field._1))
    val setExpr = '{ Set(${ Expr.ofSeq(keysExpr) }: _*) }
    val iterableExpr = '{
      val keys = $setExpr
      ${ rec }.filter { case (key, _) => keys.contains(key) }
    }
    (iterableExpr, schema)
  }

  def newMapRecord[R: Type](record: Expr[Iterable[(String, Any)]]): Expr[R] =
    '{ new MapRecord(${ record }.toMap).asInstanceOf[R] }

  def extend(
    record: Expr[Iterable[(String, Any)]],
    fields: Expr[IterableOnce[(String, Any)]],
  )(newSchema: Type[_]): Expr[Any] =
    newSchema match {
      case '[tpe] =>
        newMapRecord[tpe]('{ ${ record } ++ ${ fields } })
    }
}

private[record4s] object InternalMacros {
  given (using scala.quoted.Quotes): InternalMacros = new InternalMacros
}
