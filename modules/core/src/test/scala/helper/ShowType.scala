package helper

import scala.quoted.*

def showTypeOfImpl[A: Type](x: Expr[A])(using Quotes): Expr[String] = {
  import quotes.reflect.*

  val tpe = TypeRepr.of[A].show(using Printer.TypeReprShortCode)
  Expr(tpe)
}

inline def showTypeOf[A](x: A): String =
  ${ showTypeOfImpl('x) }
