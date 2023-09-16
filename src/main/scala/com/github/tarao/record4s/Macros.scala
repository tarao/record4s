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
        val schema = schemaOf[R]
        val valueType = schema.find(_._1 == label).map(_._2).getOrElse {
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

  /** Macro implementation of `%.apply` */
  def applyImpl[R: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

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

  /** Macro implementation of `%.++` */
  def concatImpl[R1: Type, R2: Type](
    record: Expr[R1],
    other: Expr[R2],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

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

  /** Macro implementation of `%.|+|` */
  def concatDirectlyImpl[R1 <: `%`: Type, R2 <: `%`: Type](
    record: Expr[R1],
    other: Expr[R2],
  )(using Quotes): Expr[R1 & R2] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

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
    newMapRecord[R1 & R2]('{ ${ rec1 } ++ ${ rec2 } })
  }

  /** Macro implementation of `%.as` */
  def upcastImpl[R1 <: `%`: Type, R2 >: R1: Type](
    record: Expr[R1],
  )(using Quotes): Expr[R2] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val (tidied, _) = tidiedIterableOf('{ ${ record }: R2 })
    newMapRecord[R2](tidied)
  }

  /** Macro implementation of `%.to` */
  def toProductImpl[R <: `%`: Type, P <: Product: Type](
    record: Expr[R],
  )(using Quotes): Expr[P] = {
    import quotes.reflect.*
    val internal = summon[InternalMacros]
    import internal.*

    val schema = schemaOf[R].toMap
    val fieldTypes = fieldTypesOf(evidenceOf[RecordLike[P]])

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

  private def typeReprOf(
    tpe: Type[_],
  )(using Quotes): quotes.reflect.TypeRepr = {
    import quotes.reflect.*

    tpe match {
      case '[tpe] => TypeRepr.of[tpe]
    }
  }

  private case class DedupedSchema[TypeRepr](
    schema: Seq[(String, TypeRepr)],
    duplications: Seq[(String, TypeRepr)],
  )

  private object DedupedSchema {
    extension (using Quotes)(schema: DedupedSchema[Type[_]]) {
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
          .foldLeft(TypeRepr.of[%]) { case (base, (label, tpe)) =>
            Refinement(base, label, typeReprOf(tpe))
          }
          .asType
      }
    }

    def apply(using Quotes)(
      schema: Seq[(String, Type[_])],
    ): DedupedSchema[Type[_]] = {
      val seen = collection.mutable.HashSet[String]()
      val deduped = collection.mutable.ListBuffer.empty[(String, Type[_])]
      val duplications = collection.mutable.ListBuffer.empty[(String, Type[_])]
      schema.reverseIterator.foreach { case (label, tpe) =>
        if (seen.add(label)) deduped.prepend((label, tpe))
        else duplications.prepend((label, tpe))
      }

      DedupedSchema(
        schema       = deduped.toSeq,
        duplications = duplications.toSeq,
      )
    }
  }
}
