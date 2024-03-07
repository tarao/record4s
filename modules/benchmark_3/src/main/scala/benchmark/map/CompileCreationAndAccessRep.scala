package benchmark
package map

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
class CompileCreationAndAccessRep {
  val repetitions = 10

  @Param(Array("1", "50", "100", "150", "200", "250"))
  var size: Int = 0

  var source: String = ""

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    compiler = new Compiler

    val fields = (1 to size).map(i => s""""f${i}" -> ${i}""").mkString(",")
    val access =
      (1 to repetitions).map(_ => s"""  r("f${size}")""").mkString("\n")
    source = s"""
                |object A {
                |  val r = Map(${fields})
                |${access}
                |}
                |""".stripMargin
  }

  @Benchmark
  def create_fN =
    compiler.compile(source)
}
