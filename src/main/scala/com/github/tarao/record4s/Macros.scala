package com.github.tarao.record4s

import scala.annotation.tailrec

object Macros {
  import scala.compiletime.{codeOf, error}
  import scala.quoted.*

  private def evidenceOf[T: Type](using Quotes): Expr[T] = {
    import quotes.reflect.*

    Expr.summon[T].getOrElse {
      report.errorAndAbort(
        s"No given instance of ${Type.show[T]}",
      )
    }
  }

  private def schemaOf[R: Type](using
    Quotes,
  ): Seq[(String, quotes.reflect.TypeRepr)] = {
    import quotes.reflect.*

    @tailrec def collectTupledFieldTypes(
      tpe: Type[_],
      acc: Seq[(String, TypeRepr)],
    ): Seq[(String, TypeRepr)] = tpe match {
      case '[(labelType, valueType) *: rest] =>
        TypeRepr.of[labelType] match {
          case ConstantType(StringConstant(label)) =>
            collectTupledFieldTypes(
              Type.of[rest],
              acc :+ (label, TypeRepr.of[valueType]),
            )
          case _ =>
            collectTupledFieldTypes(Type.of[rest], acc)
        }
      case _ =>
        acc
    }

    @tailrec def collectFieldTypes(
      reversed: List[TypeRepr],
      acc: Seq[(String, TypeRepr)],
    ): Seq[(String, TypeRepr)] = reversed match {
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
        collectFieldTypes(base :: rest, (label, valueType) +: acc)

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

  private def fieldTypesOf(
    fields: Seq[Expr[(String, Any)]],
  )(using Quotes): Seq[(String, quotes.reflect.TypeRepr)] = {
    import quotes.reflect.*

    def fieldTypeOf(
      labelExpr: Expr[Any],
      valueExpr: Expr[Any],
    ): (String, TypeRepr) = {
      val label = labelExpr.asTerm match {
        case Literal(StringConstant(label)) if label.nonEmpty => label
        case _ =>
          report.errorAndAbort(
            "Field label must be a literal non-empty string",
            labelExpr,
          )
      }
      val tpr = valueExpr match {
        case '{ ${ _ }: tp } => TypeRepr.of[tp].widen
      }
      (label, tpr)
    }

    fields.map {
      // ("label", value)
      case '{ ($labelExpr, $valueExpr) } =>
        fieldTypeOf(labelExpr, valueExpr)

      // "label" -> value
      case '{
          scala.Predef.ArrowAssoc(${ labelExpr }: String).->(${ valueExpr })
        } =>
        fieldTypeOf(labelExpr, valueExpr)

      case expr =>
        report.errorAndAbort(s"Invalid field", expr)
    }
  }

  private def fieldTypesOf[R: Type](
    recordLike: Expr[RecordLike[R]],
  )(using Quotes): Seq[(String, quotes.reflect.TypeRepr)] = {
    import quotes.reflect.*

    recordLike match {
      case '{ ${ _ }: RecordLike[R] { type FieldTypes = fieldTypes } } =>
        schemaOf[fieldTypes]
    }
  }

  private case class DedupedSchema[TypeRepr](
    schema: Seq[(String, TypeRepr)],
    duplications: Seq[(String, TypeRepr)],
  )
  private object DedupedSchema {
    extension (using Quotes)(schema: DedupedSchema[quotes.reflect.TypeRepr]) {
      def asType: Type[_] = {
        import quotes.reflect.*

        // Generates:
        //   % {
        //     val ${schema(0)._1}: ${schema(0)._2}
        //     val ${schema(1)._1}: ${schema(1)._2}
        //     ...
        //   }
        // where it is actually
        //   (...((%
        //     & { val ${schema(0)._1}: ${schema(0)._2} })
        //     & { val ${schema(1)._1}: ${schema(1)._2} })
        //     ...)
        schema
          .schema
          .foldLeft(TypeRepr.of[%]) { case (base, (label, tpr)) =>
            Refinement(base, label, tpr)
          }
          .asType
      }
    }

    def apply(using Quotes)(
      schema: Seq[(String, quotes.reflect.TypeRepr)],
    ): DedupedSchema[quotes.reflect.TypeRepr] = {
      val seen = collection.mutable.HashSet[String]()
      val deduped =
        collection.mutable.ListBuffer.empty[(String, quotes.reflect.TypeRepr)]
      val duplications =
        collection.mutable.ListBuffer.empty[(String, quotes.reflect.TypeRepr)]
      schema.reverseIterator.foreach { case (label, tpr) =>
        if (seen.add(label)) deduped.prepend((label, tpr))
        else duplications.prepend((label, tpr))
      }

      DedupedSchema(
        schema       = deduped.toSeq,
        duplications = duplications.toSeq,
      )
    }
  }

  private def iterableOf[R: Type](record: Expr[R])(using
    Quotes,
  ): (Expr[Iterable[(String, Any)]], Seq[(String, quotes.reflect.TypeRepr)]) = {
    import quotes.reflect.*

    val ev = evidenceOf[RecordLike[R]]
    val schema = fieldTypesOf(ev)
    ('{ ${ ev }.iterableOf($record) }, schema)
  }

  private def tidiedIterableOf[R: Type](record: Expr[R])(using
    Quotes,
  ): (Expr[Iterable[(String, Any)]], Seq[(String, quotes.reflect.TypeRepr)]) = {
    import quotes.reflect.*

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

  private def extend(
    record: Expr[Iterable[(String, Any)]],
    fields: Expr[IterableOnce[(String, Any)]],
  )(newSchema: Type[_])(using Quotes): Expr[Any] = {
    import quotes.reflect.*

    newSchema match {
      case '[tpe] =>
        '{ new MapRecord((${ record } ++ ${ fields }).toMap).asInstanceOf[tpe] }
    }
  }

  def lookupImpl[R <: `%`: Type](
    record: Expr[R],
    label: Expr[String],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*

    label.asTerm match {
      case Inlined(_, _, Literal(StringConstant(label))) =>
        val schema = schemaOf[R]
        val valueType = schema.find(_._1 == label).map(_._2.asType).getOrElse {
          report
            .errorAndAbort(s"value ${label} is not a member of ${Type.show[R]}")
        }

        valueType match {
          case '[tpe] =>
            '{ ${ record }.__data(${ Expr(label) }).asInstanceOf[tpe] }
        }
      case _ =>
        report.errorAndAbort("label must be a literal string", label)
    }
  }

  def applyImpl[R: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*

    method.asTerm match {
      case Inlined(_, _, Literal(StringConstant(name))) if name == "apply" =>
        val (rec, base) = iterableOf(record)

        val fields = args match {
          case Varargs(args) => args
          case _ =>
            report.errorAndAbort("Expected explicit varargs sequence", args)
        }
        val fieldTypes = fieldTypesOf(fields)

        val newSchema = DedupedSchema(base ++ fieldTypes).asType
        extend(rec, Expr.ofSeq(fields))(newSchema)

      case Inlined(_, _, Literal(StringConstant(name))) =>
        report.errorAndAbort(
          s"'${name}' is not a member of ${record.asTerm.tpe.widen.show} constructor",
        )
      case _ =>
        report.errorAndAbort(
          s"Invalid method invocation on ${record.asTerm.tpe.widen.show} constructor",
        )
    }
  }

  def concatImpl[R1: Type, R2: Type](
    record: Expr[R1],
    other: Expr[R2],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*

    // `tidiedIterableOf` needed here otherwise hidden fields in an upcasted `other` may
    // break the field in `record`.
    //
    // Example:
    //   val record = %(name = "tarao", email = "tarao@example.com")
    //   val other: %{val age: Int} = %(age = 3, email = %(user = "ikura", domain = "example.com"))
    //   record ++ other
    //   // should be  %(name = tarao, age = 3, email = tarao@example.com)
    //   // instead of %(name = tarao, age = 3, email = %(user = ikura, domain = example.com))
    val (rec1, schema1) = iterableOf(record)
    val (rec2, schema2) = tidiedIterableOf(other)

    val newSchema = DedupedSchema(schema1 ++ schema2).asType
    extend(rec1, rec2)(newSchema)
  }

  def concatDirectlyImpl[R1 <: `%`: Type, R2 <: `%`: Type](
    record: Expr[R1],
    other: Expr[R2],
  )(using Quotes): Expr[R1 & R2] = {
    import quotes.reflect.*

    val (rec1, schema1) = iterableOf(record)
    val (rec2, schema2) = tidiedIterableOf(other)

    // The difference from `concatImpl`:
    //
    // 1. It doesn't allow duplicated fields.
    // 2. The result type is an intersection type.
    //    e.g. %{val name: String } & %{val age: Int}
    //         insted of %{val name: String; val age: Int}
    //
    // The second one make it possible to write this method as a blackbox macro.
    // (`inline` instead of `transparent inline`)
    val duplications = DedupedSchema(schema1 ++ schema2).duplications
    if (duplications.nonEmpty) {
      val dup = duplications
        .map(_._1)
        .distinct
        .reverse
        .map(label => s"'${label}'")
        .mkString(", ")
      report.errorAndAbort(
        s"Two records must be disjoint (${dup} are duplicated)",
      )
    }
    '{ new MapRecord((${ rec1 } ++ ${ rec2 }).toMap).asInstanceOf[R1 & R2] }
  }

  def upcastImpl[R1 <: `%`: Type, R2 >: R1: Type](
    record: Expr[R1],
  )(using Quotes): Expr[R2] = {
    import quotes.reflect.*

    val (tidied, _) = tidiedIterableOf('{ ${ record }: R2 })
    '{ new MapRecord(${ tidied }.toMap).asInstanceOf[R2] }
  }

  def toProductImpl[R <: `%`: Type, P <: Product: Type](
    record: Expr[R],
  )(using Quotes): Expr[P] = {
    import quotes.reflect.*

    val schema = schemaOf[R].toMap
    val fieldTypes = fieldTypesOf(evidenceOf[RecordLike[P]])

    // type check
    for ((label, tpr) <- fieldTypes) {
      val valueTpr = schema.getOrElse(
        label,
        report.errorAndAbort(s"Missing key '${label}'", record),
      )
      if (!(valueTpr <:< tpr)) {
        report.errorAndAbort(
          s"""Found:    (${record.show}.${label}: ${valueTpr.show})
             |Required: ${tpr.show}
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
}
