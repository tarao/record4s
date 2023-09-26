package com.github.tarao.record4s

import scala.annotation.tailrec

private[record4s] class InternalMacros(using scala.quoted.Quotes) {
  import scala.quoted.*
  import quotes.reflect.*

  case class Schema(
    fieldTypes: Seq[(String, Type[_])],
    tags: Seq[Type[_]],
  ) {
    def ++(other: Schema): Schema = copy(
      fieldTypes = fieldTypes ++ other.fieldTypes,
      tags       = tags ++ other.tags,
    )

    def ++(other: Seq[(String, Type[_])]): Schema = copy(
      fieldTypes = fieldTypes ++ other,
    )

    def deduped: Schema = {
      val seen = collection.mutable.HashSet[String]()
      val deduped = collection.mutable.ListBuffer.empty[(String, Type[_])]
      fieldTypes.reverseIterator.foreach { case (label, tpe) =>
        if (seen.add(label)) deduped.prepend((label, tpe))
      }

      copy(fieldTypes = deduped.toSeq)
    }

    def asType: Type[_] = asType(Type.of[%])

    def asType(base: Type[_]): Type[_] = {
      val baseRepr = base match { case '[tpe] => TypeRepr.of[tpe] }

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
      val record = fieldTypes
        .foldLeft(baseRepr) { case (base, (label, '[tpe])) =>
          Refinement(base, label, TypeRepr.of[tpe])
        }

      tags
        .foldLeft(record) { case (base, '[tag]) =>
          AndType(base, TypeRepr.of[Tag[tag]])
        }
        .asType
    }
  }
  object Schema {
    val empty = apply(Seq.empty, Seq.empty)
  }

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

  def schemaOfRecord[R: Type]: Schema = {
    // Check if tpr represents Tag[T]: we need to check IsTag[Tag[T]] given instance
    // because representation of opaque type varies among different package names such as
    // Tag$package.Tag[T] or $proxyN.Tag[T].
    def isTag(tpr: TypeRepr): Boolean =
      tpr.asType match {
        case '[tpe] => Expr.summon[Tag.IsTag[tpe]].nonEmpty
        case _      => false
      }

    def dealias(tpr: TypeRepr): TypeRepr =
      if (isTag(tpr)) tpr
      else tpr.dealias

    val nothing = TypeRepr.of[Nothing]

    @tailrec def collectTupledFieldTypes(
      tpe: Type[_],
      acc: Seq[(String, Type[_])],
    ): Seq[(String, Type[_])] = tpe match {
      case '[(labelType, valueType) *: rest]
        // Type variable or Nothing always matches with `Nothing *: Nothing`
        if TypeRepr.of[labelType] != nothing
          && TypeRepr.of[valueType] != nothing
          && TypeRepr.of[rest] != nothing =>
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

    @tailrec def collectFieldTypesAndTags(
      reversed: List[TypeRepr],
      acc: Schema,
    ): Schema = reversed match {
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
        collectFieldTypesAndTags(
          dealias(base) :: rest,
          acc.copy(fieldTypes =
            (validatedLabel(label), valueType.asType) +: acc.fieldTypes,
          ),
        )

      // tpr1 & tpr2
      case AndType(tpr1, tpr2) :: rest =>
        collectFieldTypesAndTags(dealias(tpr2) :: dealias(tpr1) :: rest, acc)

      // Tag[T]
      case (head @ AppliedType(_, List(tpr))) :: rest if isTag(head) =>
        collectFieldTypesAndTags(
          rest,
          acc.copy(tags = tpr.asType +: acc.tags),
        )

      // typically `%` in `% { ... }` or
      // (tp1, ...)
      // tp1 *: ...
      case head :: rest =>
        collectFieldTypesAndTags(
          rest,
          acc.copy(fieldTypes =
            collectTupledFieldTypes(head.asType, Seq.empty) ++ acc.fieldTypes,
          ),
        )

      // all done
      case Nil =>
        acc
    }

    collectFieldTypesAndTags(List(dealias(TypeRepr.of[R])), Schema.empty)
  }

  def schemaOf[R: Type](
    recordLike: Expr[RecordLike[R]],
  ): Schema =
    if (TypeRepr.of[R] <:< TypeRepr.of[%])
      // Use R directly for % types: it in theroy should work fine with
      // RecordLike[R]#FieldTypes for R <: %, but it sometimes drops Tag[T] in R.
      schemaOfRecord[R]
    else
      recordLike match {
        case '{ ${ _ }: RecordLike[R] { type FieldTypes = fieldTypes } } =>
          schemaOfRecord[fieldTypes]
      }

  def schemaOf[R: Type]: Schema =
    schemaOf(evidenceOf[RecordLike[R]])

  def fieldTypeOf(
    field: Expr[(String, Any)],
  ): (String, Type[_]) = {
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

    field match {
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

  def fieldTypesOf(
    fields: Seq[Expr[(String, Any)]],
  ): Seq[(String, Type[_])] = fields.map(fieldTypeOf(_))

  def iterableOf[R: Type](
    record: Expr[R],
  ): (Expr[Iterable[(String, Any)]], Schema) = {
    val ev = evidenceOf[RecordLike[R]]
    val schema = schemaOf(ev)
    ('{ ${ ev }.iterableOf($record) }, schema)
  }

  def fieldSelectionsOf[S: Type](
    schema: Schema,
  ): Seq[(String, String, Type[_])] = {
    val fieldTypeMap = schema.fieldTypes.toMap

    def normalize(t: Type[_]): (TypeRepr, TypeRepr) = t match {
      case '[(l1, l2)] => (TypeRepr.of[l1], TypeRepr.of[l2])
      case '[l]        => (TypeRepr.of[l], TypeRepr.of[l])
    }

    @tailrec def fieldTypes(
      t: Type[_],
      acc: Seq[(String, String, Type[_])],
    ): Seq[(String, String, Type[_])] =
      t match {
        case '[head *: tail] =>
          normalize(Type.of[head]) match {
            case (
                ConstantType(StringConstant(label)),
                ConstantType(StringConstant(renamed)),
              ) =>
              val fieldType = fieldTypeMap.getOrElse(
                label,
                report.errorAndAbort(s"Missing key ${label}"),
              )
              fieldTypes(Type.of[tail], acc :+ (label, renamed, fieldType))
            case _ =>
              report.errorAndAbort(
                "Selector type element must be a literal (possibly paired) label",
              )
          }
        case '[EmptyTuple] =>
          acc
        case _ =>
          report.errorAndAbort("Selector type must be a Tuple")
      }

    fieldTypes(Type.of[S], Seq.empty)
  }

  def fieldUnselectionsOf[U <: Tuple: Type](
    schema: Schema,
  ): Seq[(String, Type[_])] = {
    @tailrec def unselectedLabelsOf[U <: Tuple: Type](
      acc: Set[String],
    ): Set[String] =
      Type.of[U] match {
        case '[head *: tail] =>
          TypeRepr.of[head] match {
            case ConstantType(StringConstant(label)) =>
              unselectedLabelsOf[tail](acc + label)
            case _ =>
              report.errorAndAbort(
                "Selector type element must be a literal label",
              )
          }
        case '[EmptyTuple] =>
          acc
      }
    val unselected = unselectedLabelsOf[U](Set.empty)

    schema.fieldTypes.filterNot((label, _) => unselected.contains(label))
  }

  def newMapRecord[R: Type](record: Expr[Iterable[(String, Any)]]): Expr[R] =
    '{ new MapRecord(${ record }.toMap).asInstanceOf[R] }

  def requireApply[C, T](context: Expr[C], method: Expr[String])(
    block: => T,
  ): T = {
    method.asTerm match {
      case Inlined(_, _, Literal(StringConstant(name))) if name == "apply" =>
        block
      case Inlined(_, _, Literal(StringConstant(name))) =>
        report.errorAndAbort(
          s"'${name}' is not a member of ${context.asTerm.tpe.widen.show} constructor",
        )
      case _ =>
        report.errorAndAbort(
          s"Invalid method invocation on ${context.asTerm.tpe.widen.show} constructor",
        )
    }
  }
}

private[record4s] object InternalMacros {
  given (using scala.quoted.Quotes): InternalMacros = new InternalMacros
}
