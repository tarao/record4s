package benchmark

import dotty.tools.dotc.core.Contexts.inContext
import dotty.tools.repl.{
  Command,
  ParseResult,
  Parsed,
  ReplDriver => DottyReplDriver,
  State,
  SyntaxErrors,
}
import java.io.{ByteArrayOutputStream, PrintStream}

class Compiler {
  val out = new ByteArrayOutputStream()

  val driver = new Compiler.ReplDriver(new PrintStream(out))

  given initialState: State =
    driver.initialState

  def compile(parsed: Parsed): State = {
    val state = driver.interpret(parsed)

    if (state.context.reporter.hasErrors) {
      inContext(state.context) {
        state.context.reporter.allErrors.foreach { err =>
          state.context.reporter.report(err)
        }
        throw Compiler.TypeError(out.toString())
      }
    }

    state
  }

  def compile(source: String): State =
    ParseResult.complete(source) match {
      case parsed: Parsed =>
        compile(parsed)
      case s: SyntaxErrors =>
        inContext(initialState.context) {
          s.errors.foreach { err =>
            initialState.context.reporter.report(err)
          }
          throw Compiler.SyntaxError(out.toString())
        }
      case _: Command =>
        throw new UnsupportedOperationException("Command is not supported")
      case _ =>
        initialState
    }
}

object Compiler {
  class ReplDriver(out: PrintStream)
      extends DottyReplDriver(
        Array(
          "-classpath",
          "",
          "-usejavacp",
          "-color:never",
          "-Xrepl-disable-display",
        ),
        out,
        None,
      ) {
    // make this public
    override def interpret(res: ParseResult, quiet: Boolean = false)(using
      state: State,
    ): State =
      super.interpret(res, quiet)
  }

  class TypeError(msg: String)   extends Exception(msg)
  class SyntaxError(msg: String) extends Exception(msg)
}
