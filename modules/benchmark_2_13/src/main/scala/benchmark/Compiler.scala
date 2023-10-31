package benchmark

import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

class Compiler {
  private val toolBox =
    currentMirror.mkToolBox()

  def compile(source: String): () => Any = {
    val tree = toolBox.parse(source)
    toolBox.compile(tree)
  }
}
