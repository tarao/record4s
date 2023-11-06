package com.github.tarao.record4s

import scala.annotation.tailrec
import util.SeqOps.deduped

private[record4s] class InternalMacros(using
  scala.quoted.Quotes,
  InternalMacros.MacroContext,
) {
  import scala.quoted.*
  import quotes.reflect.*

  case class TypingResult(
    resultType: Type[?],
    error: Type[?],
  )

  object TypingResult {
    def success(tpe: Type[?]): TypingResult = TypingResult(
      resultType = tpe,
      error      = Type.of[Nothing],
    )

    def error(msg: String): TypingResult = TypingResult(
      resultType = Type.of[Nothing],
      error      = ConstantType(StringConstant(msg)).asType,
    )
  }

  def errorAndAbort(msg: String, expr: Option[Expr[Any]] = None): Nothing =
    summon[InternalMacros.MacroContext].reporter.errorAndAbort(msg, expr)

  def catching(block: => Type[?]): TypingResult =
    try
      TypingResult.success(block)
    catch {
      case e: InternalMacros.TypingError =>
        TypingResult.error(e.getMessage())
    }

  case class Schema(
    fieldTypes: Seq[(String, Type[?])],
    tags: Seq[Type[?]],
  ) {
    def size: Int = fieldTypes.size

    def ++(other: Schema): Schema = copy(
      fieldTypes = fieldTypes ++ other.fieldTypes,
      tags       = tags ++ other.tags,
    )

    def ++(other: Seq[(String, Type[?])]): Schema = copy(
      fieldTypes = fieldTypes ++ other,
    )

    def deduped: Schema =
      copy(fieldTypes = fieldTypes.deduped.iterator.toSeq)

    def asType: Type[?] = asType(Type.of[%])

    def asType(base: Type[?]): Type[?] = {
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
      tagsWith(record).asType
    }

    def asTupleType: Type[?] = {
      val record = fieldTypes.foldRight(TypeRepr.of[EmptyTuple]) {
        case ((label, '[tpe]), rest) =>
          (ConstantType(StringConstant(label)).asType, rest.asType) match {
            case ('[labelType], '[head *: tail]) =>
              TypeRepr.of[(labelType, tpe) *: head *: tail]
            case ('[labelType], '[EmptyTuple]) =>
              TypeRepr.of[(labelType, tpe) *: EmptyTuple]
          }
      }
      tagsWith(record).asType
    }

    def tagsAsType: Type[?] = tagsWith(TypeRepr.of[Any]).asType

    private def tagsWith(tpr: TypeRepr): TypeRepr =
      tags
        .foldLeft(tpr) { case (base, '[tag]) =>
          AndType(base, TypeRepr.of[Tag[tag]])
        }
  }

  object Schema {
    val empty = apply(Seq.empty, Seq.empty)
  }

  def validatedLabel(
    label: String,
    context: Option[Expr[Any]] = None,
  ): String = {
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
        "'$' cannot be used in a field label",
        context,
      )
    else
      label
  }

  def evidenceOf[T: Type]: Expr[T] =
    Expr.summon[T].getOrElse {
      errorAndAbort(
        s"No given instance of ${Type.show[T]}",
      )
    }

  def schemaOfRecord[R: Type]: Schema = {
    // Check if tpr represents Tag[T]: we need to check IsTag[Tag[T]] given instance
    // because representation of opaque type varies among different package names such as
    // Tag$package.Tag[T] or $proxyN.Tag[T].
    def isTag(tpr: TypeRepr): Boolean =
      tpr match {
        case AppliedType(t, _) =>
          tpr.asType match {
            case '[tpe] => Expr.summon[Tag.IsTag[tpe]].nonEmpty
          }
        case _ =>
          false
      }

    def safeDealias(tpr: TypeRepr): TypeRepr =
      if (isTag(tpr)) tpr
      else tpr.dealias

    val nothing = TypeRepr.of[Nothing]

    @tailrec def collectTupledFieldTypes(
      tpe: Type[?],
      acc: Seq[(String, Type[?])],
    ): Seq[(String, Type[?])] = tpe match {
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
          safeDealias(base) :: rest,
          acc.copy(fieldTypes =
            (validatedLabel(label), valueType.asType) +: acc.fieldTypes,
          ),
        )

      // tpr1 & tpr2
      case AndType(tpr1, tpr2) :: rest =>
        collectFieldTypesAndTags(
          safeDealias(tpr2) :: safeDealias(tpr1) :: rest,
          acc,
        )

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

    collectFieldTypesAndTags(List(safeDealias(TypeRepr.of[R])), Schema.empty)
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

  def schemaOf[R: Type]: Schema = {
    def isTuple[T: Type]: Boolean = Type.of[T] match {
      case '[_ *: _]     => true
      case '[EmptyTuple] => true
      case _             => false
    }

    if (TypeRepr.of[R] <:< TypeRepr.of[%])
      schemaOfRecord[R] // optimize
    else if (isTuple[R])
      schemaOfRecord[R] // optimize
    else
      schemaOf(evidenceOf[RecordLike[R]])
  }

  def fieldTypeOf(
    field: Expr[(String, Any)],
  ): (String, Type[?]) = {
    def fieldTypeOf(
      labelExpr: Expr[Any],
      valueExpr: Expr[Any],
    ): (String, Type[?]) = {
      val label = labelExpr.asTerm match {
        case Literal(StringConstant(label)) =>
          validatedLabel(label, Some(labelExpr))
        case _ =>
          errorAndAbort(
            "Field label must be a literal string",
            Some(labelExpr),
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
        errorAndAbort(s"Invalid field", Some(expr))
    }
  }

  def fieldTypesOf(
    fields: Seq[Expr[(String, Any)]],
  ): Seq[(String, Type[?])] = fields.map(fieldTypeOf(_))

  def extractFieldsFrom(
    varargs: Expr[Seq[(String, Any)]],
  ): (Expr[Seq[(String, Any)]], Type[?]) = {
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

    val tupledFieldTypes =
      fieldTypes.foldRight(Type.of[EmptyTuple]: Type[?]) {
        case ((label, '[tpe]), base) =>
          val pair = ConstantType(StringConstant(label)).asType match {
            case '[label] => Type.of[(label, tpe)]
          }
          (pair, base) match {
            case ('[tpe], '[head *: tail]) =>
              Type.of[tpe *: head *: tail]
            case ('[tpe], '[EmptyTuple]) =>
              Type.of[tpe *: EmptyTuple]
          }
      }

    (Expr.ofSeq(fields), tupledFieldTypes)
  }

  def requireApply[C, T](context: Expr[C], method: Expr[String])(
    block: => T,
  ): T = {
    method.asTerm match {
      case Inlined(_, _, Literal(StringConstant(name))) if name == "apply" =>
        block
      case Inlined(_, _, Literal(StringConstant(name))) =>
        errorAndAbort(
          s"'${name}' is not a member of ${context.asTerm.tpe.widen.show} constructor",
        )
      case _ =>
        errorAndAbort(
          s"Invalid method invocation on ${context.asTerm.tpe.widen.show} constructor",
        )
    }
  }

  def fieldSelectionsOf[S: Type](
    schema: Schema,
  ): Seq[(String, String, Type[?])] = {
    val fieldTypeMap = schema.fieldTypes.toMap

    def normalize(t: Type[?]): (TypeRepr, TypeRepr) = t match {
      case '[(l1, l2)] => (TypeRepr.of[l1], TypeRepr.of[l2])
      case '[l]        => (TypeRepr.of[l], TypeRepr.of[l])
    }

    @tailrec def fieldTypes(
      t: Type[?],
      acc: Seq[(String, String, Type[?])],
    ): Seq[(String, String, Type[?])] =
      t match {
        case '[head *: tail] =>
          normalize(Type.of[head]) match {
            case (
                ConstantType(StringConstant(label)),
                ConstantType(StringConstant(renamed)),
              ) =>
              val fieldType = fieldTypeMap.getOrElse(
                label,
                errorAndAbort(s"Missing key '${label}'"),
              )
              fieldTypes(Type.of[tail], acc :+ (label, renamed, fieldType))
            case _ =>
              errorAndAbort(
                "Selector type element must be a literal (possibly paired) label",
              )
          }
        case '[EmptyTuple] =>
          acc
        case _ =>
          errorAndAbort("Selector type must be a Tuple")
      }

    fieldTypes(Type.of[S], Seq.empty)
  }

  def fieldUnselectionsOf[U <: Tuple: Type](
    schema: Schema,
  ): Seq[(String, Type[?])] = {
    @tailrec def unselectedLabelsOf[U <: Tuple: Type](
      acc: Set[String],
    ): Set[String] =
      Type.of[U] match {
        case '[head *: tail] =>
          TypeRepr.of[head] match {
            case ConstantType(StringConstant(label)) =>
              unselectedLabelsOf[tail](acc + label)
            case _ =>
              errorAndAbort(
                "Unselector type element must be a literal label",
              )
          }
        case '[EmptyTuple] =>
          acc
      }
    val unselected = unselectedLabelsOf[U](Set.empty)

    schema.fieldTypes.filterNot((label, _) => unselected.contains(label))
  }
}

private[record4s] object InternalMacros {
  import scala.quoted.*

  given (using Quotes, MacroContext): InternalMacros = new InternalMacros

  transparent inline def internal(using i: InternalMacros): i.type = i

  inline def withInternal[T](using Quotes, InternalMacros)(
    inline block: InternalMacros ?=> T,
  ): T =
    block(using summon[InternalMacros])

  inline def withTyping[T](using Quotes)(
    inline block: (MacroContext, InternalMacros) ?=> T,
  ): T = {
    given MacroContext = MacroContext.Typing
    block(using summon[MacroContext], summon[InternalMacros])
  }

  class TypingError(message: String) extends Error(message)

  sealed trait MacroContext {
    def reporter: Reporter
  }

  object MacroContext {
    case class Default()(using Quotes) extends MacroContext {
      override val reporter: Reporter = MacroReporter()
    }

    case object Typing extends MacroContext {
      override val reporter: Reporter = TypingReporter
    }
  }

  sealed trait Reporter {
    def errorAndAbort(msg: String, expr: Option[Expr[Any]] = None): Nothing
  }

  case class MacroReporter()(using Quotes) extends Reporter {
    def errorAndAbort(msg: String, expr: Option[Expr[Any]] = None): Nothing =
      expr match {
        case Some(expr) =>
          quotes.reflect.report.errorAndAbort(msg, expr)
        case None =>
          quotes.reflect.report.errorAndAbort(msg)
      }
  }

  case object TypingReporter extends Reporter {
    def errorAndAbort(msg: String, expr: Option[Expr[Any]] = None): Nothing =
      throw TypingError(msg)
  }

  given (using Quotes): MacroContext = MacroContext.Default()
}
