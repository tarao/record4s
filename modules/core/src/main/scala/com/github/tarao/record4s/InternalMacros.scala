/*
 * Copyright 2023 record4s authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  private def typeOf(tpr: TypeRepr): Type[?] =
    tpr.asType match { case '[tpe] => Type.of[tpe] }

  private def typeReprOf(tpe: Type[?]): TypeRepr =
    tpe match { case '[tpe] => TypeRepr.of[tpe] }

  case class Schema private[InternalMacros] (
    private[InternalMacros] val fieldTypes: Seq[(String, TypeRepr)],
    tags: Seq[Type[?]],
  ) {
    def size: Int = fieldTypes.size

    private[InternalMacros] def appended(label: String, tpe: TypeRepr): Schema =
      copy(fieldTypes = fieldTypes :+ (label, tpe))

    private[InternalMacros] def prepended(
      label: String,
      tpe: TypeRepr,
    ): Schema =
      copy(fieldTypes = (label, tpe) +: fieldTypes)

    def ++(other: Schema): Schema = copy(
      fieldTypes = fieldTypes ++ other.fieldTypes,
      tags       = tags ++ other.tags,
    )

    def find(label: String): Option[Type[?]] =
      fieldTypes.find(_._1 == label).map(f => typeOf(f._2))

    def findWithIndex(label: String): Option[(Type[?], Int)] =
      fieldTypes.zipWithIndex.find(_._1._1 == label).map {
        case ((_, tpr), index) =>
          (typeOf(tpr), index)
      }

    def filterByLabel(pred: String => Boolean): Schema =
      copy(fieldTypes = fieldTypes.filter(f => pred(f._1)))

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
        .foldLeft(baseRepr) { case (base, (label, tpr)) =>
          Refinement(base, label, tpr)
        }
      tagsWith(record).asType
    }

    def asTupleType: Type[?] = {
      val cons = TypeRepr.of[Nothing *: EmptyTuple] match {
        case AppliedType(c, _) => c
      }
      def makeCons(car: TypeRepr, cdr: TypeRepr): TypeRepr =
        AppliedType(cons, List(car, cdr))

      val tuple2 = TypeRepr.of[(Nothing, Nothing)] match {
        case AppliedType(c, _) => c
      }
      def makeTuple2(fst: TypeRepr, snd: TypeRepr): TypeRepr =
        AppliedType(tuple2, List(fst, snd))

      val record = fieldTypes.foldRight(TypeRepr.of[EmptyTuple]) {
        case ((label, tpr), rest) =>
          makeCons(makeTuple2(ConstantType(StringConstant(label)), tpr), rest)
      }
      tagsWith(record).asType
    }

    def asUnzippedTupleType: (Type[?], Type[?]) = {
      val base = (Type.of[EmptyTuple]: Type[?], Type.of[EmptyTuple]: Type[?])
      fieldTypes.foldRight(base) {
        case ((label, tpr), (baseLabels, baseTypes)) =>
          val tpe = typeOf(tpr)
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

  // Check if tpr represents Tag[T]: we need to check IsTag[Tag[T]] given instance
  // because representation of opaque type varies among different package names such as
  // Tag$package.Tag[T] or $proxyN.Tag[T].
  private def isTag(tpr: TypeRepr): Boolean =
    tpr match {
      case AppliedType(_, _) =>
        tpr.asType match {
          case '[tpe] => Expr.summon[Tag.IsTag[tpe]].nonEmpty
        }
      case _ =>
        false
    }

  private def isTuple(tpr: TypeRepr): Boolean =
    tpr.asType match {
      case '[_ *: _] => true
      case _         => false
    }

  private def isOpaqueAlias(tpr: TypeRepr): Boolean =
    tpr match {
      case ref @ TypeRef(_, _) => ref.isOpaqueAlias
      case _                   => false
    }

  private def fixupOpaqueAlias(tpr: TypeRepr): TypeRepr = {
    def rec(tpr: TypeRepr): TypeRepr =
      tpr match {
        case ref @ TypeRef(_, _) if ref.isOpaqueAlias =>
          // Resolve `$proxy1.SomeOpaqueAlias` to fully qualified RefType
          tpr.typeSymbol.typeRef
        case SuperType(thisTpr, superTpr) =>
          SuperType(rec(thisTpr), rec(superTpr))
        case Refinement(parent, name, info) =>
          Refinement(rec(parent), name, rec(info))
        case AppliedType(tycon, args) =>
          AppliedType(rec(tycon), args.map(rec(_)))
        case AnnotatedType(underlying, annot) =>
          AnnotatedType(rec(underlying), annot)
        case AndType(lhs, rhs) =>
          AndType(rec(lhs), rec(rhs))
        case OrType(lhs, rhs) =>
          OrType(rec(lhs), rec(rhs))
        case MatchType(bound, scrutinee, cases) =>
          MatchType(rec(bound), rec(scrutinee), cases.map(rec(_)))
        case ByNameType(underlying) =>
          ByNameType(rec(underlying))
        case MatchCase(pattern, rhs) =>
          MatchCase(rec(pattern), rec(rhs))
        case TypeBounds(low, hi) =>
          TypeBounds(rec(low), rec(hi))
        case _ =>
          tpr
      }

    rec(tpr)
  }

  private def isAlias(tpr: TypeRepr): Boolean =
    tpr.typeSymbol.isAliasType && !isOpaqueAlias(tpr)

  def traverse[R: Type, Acc](acc: Acc, f: (Acc, Type[?]) => Acc): Acc = {
    def safeDealias(tpr: TypeRepr): TypeRepr =
      if (isAlias(tpr)) tpr.dealias
      else tpr

    val nothing = TypeRepr.of[Nothing]

    @tailrec def traverseTuple(
      tpe: Type[?],
      acc: Acc,
    ): Acc = tpe match {
      case '[head *: rest]
        // Type variable or Nothing always matches with `Nothing *: Nothing`
        if TypeRepr.of[head] != nothing && TypeRepr.of[rest] != nothing =>
        traverseTuple(
          Type.of[rest],
          f(acc, Type.of[head]),
        )
      case _ =>
        f(acc, tpe)
    }

    @tailrec def traverse1(
      reversed: List[TypeRepr],
      acc: Acc,
    ): Acc = reversed match {
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
      case (tpr @ Refinement(base, _, _)) :: rest =>
        traverse1(
          safeDealias(base) :: rest,
          f(acc, tpr.asType),
        )

      // tpr1 & tpr2
      case AndType(tpr1, tpr2) :: rest =>
        traverse1(
          safeDealias(tpr2) :: safeDealias(tpr1) :: rest,
          acc,
        )

      // Tag[T]
      case head :: rest if isTag(head) =>
        traverse1(
          rest,
          f(acc, head.asType),
        )

      // typically `%` in `% { ... }` or
      // (tp1, ...)
      // tp1 *: ...
      case head :: rest =>
        traverse1(
          rest,
          traverseTuple(head.asType, acc),
        )

      // all done
      case Nil =>
        acc
    }

    traverse1(List(safeDealias(fixupOpaqueAlias(TypeRepr.of[R]))), acc)
  }

  def schemaOfRecord[R: Type]: Schema = {
    def unapplyTuple2(tpr: TypeRepr): Option[(TypeRepr, TypeRepr)] =
      // We can't do
      //
      // ```
      // tpr.asType match {
      //   case '[fst *: snd] =>
      //      Some((TypeRepr.of[fst], TypeRepr.of[snd]))
      // }
      // ```
      //
      // because that will dealiases opaque type aliases
      tpr match {
        case AppliedType(c, fst :: snd :: _)
          if c.typeSymbol.fullName == "scala.Tuple2" =>
          Some((fst, snd))
        case AppliedType(c, fst :: AppliedType(_, snd :: _) :: _)
          if c.typeSymbol.fullName == "scala.*:" =>
          Some((fst, snd))
        case _ =>
          None
      }

    traverse[R, Schema](
      Schema.empty,
      (acc: Schema, tpe: Type[?]) => {
        typeReprOf(tpe) match {
          case Refinement(_, label, valueType) =>
            acc.prepended(validatedLabel(label), valueType)

          case tpr if isTuple(tpr) =>
            unapplyTuple2(tpr) match {
              case Some((ConstantType(StringConstant(label)), valueType)) =>
                acc.appended(validatedLabel(label), valueType)
              case _ =>
                acc
            }

          // Tag[T]
          case tpr @ AppliedType(_, List(tag)) if isTag(tpr) =>
            acc.copy(tags = tag.asType +: acc.tags)

          case _ =>
            acc
        }
      },
    )
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
    field: Expr[Any],
  ): (String, Expr[Any], Type[?]) = {
    def fieldTypeOf(
      labelExpr: Expr[String],
      valueExpr: Expr[Any],
    ): (String, Expr[Any], Type[?]) = {
      val label = (labelExpr.value, valueExpr.asTerm) match {
        case (Some(""), Ident(label)) =>
          validatedLabel(label, Some(labelExpr))

        case (Some(""), Select(_, label)) =>
          validatedLabel(label, Some(labelExpr))

        case (Some(label), _) =>
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
      (label, valueExpr, tpe)
    }

    field match {
      // ("label", value)
      case '{ (${ labelExpr }: String, $valueExpr) } =>
        fieldTypeOf(labelExpr, valueExpr)

      // "label" -> value
      case '{ ArrowAssoc(${ labelExpr }: String).->(${ valueExpr }) } =>
        fieldTypeOf(labelExpr, valueExpr)

      case expr =>
        fieldTypeOf(Expr(""), expr)
    }
  }

  def fieldTypesOf(
    fields: Seq[Expr[Any]],
  ): Seq[(String, Expr[Any], Type[?])] = fields.map(fieldTypeOf(_))

  def extractFieldsFrom(
    varargs: Expr[Seq[Any]],
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
        case ((label, _, '[tpe]), base) =>
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

    val namedFields =
      fieldTypes.map((label, value, _) => Expr.ofTuple((Expr(label), value)))
    (Expr.ofSeq(namedFields), tupledFieldTypes)
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

  def selectedSchemaOf[R: Type, S: Type]: Schema = {
    val schema = schemaOf[R]
    val fieldTypeMap = schema.fieldTypes.toMap

    def normalize(t: Type[?]): (TypeRepr, TypeRepr) = t match {
      case '[(l1, l2)] => (TypeRepr.of[l1], TypeRepr.of[l2])
      case '[l]        => (TypeRepr.of[l], TypeRepr.of[l])
    }

    @tailrec def fieldTypes(
      t: Type[?],
      acc: Seq[(String, TypeRepr)],
    ): Seq[(String, TypeRepr)] =
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
              fieldTypes(Type.of[tail], acc :+ (renamed, fieldType))
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

    schema.copy(fieldTypes = fieldTypes(Type.of[S], Seq.empty))
  }

  def unselectedSchemaOf[R: Type, U <: Tuple: Type]: Schema = {
    val schema = schemaOf[R]

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

    schema.filterByLabel(label => !unselected.contains(label))
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
