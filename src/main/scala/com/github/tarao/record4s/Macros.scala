package com.github.tarao.record4s

import scala.annotation.tailrec

object Macros {
  import scala.compiletime.{codeOf, error}
  import scala.quoted.*

  def schemaOf[R: Type](using
    Quotes,
  ): Seq[(String, quotes.reflect.TypeRepr)] = {
    import quotes.reflect.*

    @tailrec def collectRefinements(
      reversed: List[TypeRepr],
      acc: Seq[(String, TypeRepr)],
    ): Seq[(String, TypeRepr)] = reversed match {
      case Refinement(base, label, valueType @ TypeRef(_, _)) :: rest =>
        collectRefinements(base :: rest, (label, valueType) +: acc)
      case AndType(tpr1, tpr2) :: rest =>
        collectRefinements(tpr2 :: tpr1 :: rest, acc)
      case _ :: rest =>
        collectRefinements(rest, acc)
      case Nil =>
        acc
    }

    collectRefinements(List(TypeRepr.of[R]), Seq.empty)
  }

  def fieldTypesOf(
    fields: Seq[Expr[(String, Any)]],
  )(using Quotes): Seq[(String, quotes.reflect.TypeRepr)] = {
    import quotes.reflect.*

    def fieldTypeOf(
      labelExpr: Expr[Any],
      valueExpr: Expr[Any],
    ): (String, TypeRepr) = {
      val label = labelExpr.asTerm match {
        case Literal(StringConstant(label)) => label
        case _ =>
          report.errorAndAbort(
            "Field label must be a literal string",
            labelExpr,
          )
      }
      val tpr = valueExpr match {
        case '{ ${ _ }: tp } => TypeRepr.of[tp].widen
      }
      (label, tpr)
    }

    fields.map {
      case '{ ($labelExpr, $valueExpr) } =>
        fieldTypeOf(labelExpr, valueExpr)
      case '{
          scala.Predef.ArrowAssoc(${ labelExpr }: String).->(${ valueExpr })
        } =>
        fieldTypeOf(labelExpr, valueExpr)
      case expr =>
        report.errorAndAbort(s"Invalid field", expr)
    }
  }

  case class DedupedSchema[TypeRepr](
    schema: Seq[(String, TypeRepr)],
    duplications: Seq[(String, TypeRepr)],
  )
  object DedupedSchema {
    extension (using Quotes)(schema: DedupedSchema[quotes.reflect.TypeRepr]) {
      def asType: Type[_] = {
        import quotes.reflect.*

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
      val (deduped, duplications) = schema.reverseIterator.partition {
        case (label, _) => seen.add(label)
      }

      DedupedSchema(
        schema       = deduped.toSeq.reverse,
        duplications = duplications.toSeq.reverse,
      )
    }
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

  def applyImpl[R: Type](
    record: Expr[R],
    method: Expr[String],
    args: Expr[Seq[(String, Any)]],
  )(using Quotes): Expr[Any] = {
    import quotes.reflect.*

    method.asTerm match {
      case Inlined(_, _, Literal(StringConstant(name))) if name == "apply" =>
        val ev: Expr[RecordLike[R]] = Expr.summon[RecordLike[R]].getOrElse {
          report.errorAndAbort(
            s"No given instance of ${TypeRepr.of[RecordLike[R]]}",
          )
        }

        val base = ev match {
          case '{ ${ _ }: RecordLike[R] { type FieldTypes = fieldTypes } } =>
            schemaOf[fieldTypes]
        }

        val fields = args match {
          case Varargs(args) => args
          case _ =>
            report.errorAndAbort("Expected explicit varargs sequence", args)
        }
        val fieldTypes = fieldTypesOf(fields)

        val newSchema = DedupedSchema(base ++ fieldTypes).asType

        extend('{ ${ ev }.toIterable($record) }, Expr.ofSeq(fields))(newSchema)

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
}
