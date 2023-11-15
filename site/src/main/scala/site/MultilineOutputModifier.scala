package site

import mdoc.{PostModifier, PostModifierContext}

class MultilineOutputModifier extends PostModifier {
  val name = "mline"

  def process(ctx: PostModifierContext): String = {
    val code = ctx.variables.map { v =>
      val s = v.toString
      (s, linesAsComment(s))
    }.foldLeft(ctx.outputCode) { case (code, (from, to)) =>
        code.replace(from, to)
    }

    s"""```scala
    |${code}
    |```
    |""".stripMargin
  }

  private def linesAsComment(lines: String): String =
    lines.split("\n").map(lineAsComment(_)).mkString("\n")

  private def lineAsComment(line: String): String =
    if (line.isEmpty())
      line
    else if (line.startsWith("//"))
      line
    else
      "// " + line
}
