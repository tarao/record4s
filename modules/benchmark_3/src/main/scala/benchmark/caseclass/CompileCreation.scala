package benchmark
package caseclass

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
class CompileCreation {
  @Param(Array("1", "50", "100", "150", "200", "250"))
  var size: Int = 0

  var source: String = ""

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    compiler = new Compiler

    val fields = (1 to size).map(i => s"f${i} = ${i}").mkString(",")
    source = s"""
                |import benchmark.caseclass.*
                |object A {
                |  val r = Record${size}(${fields})
                |}
                |""".stripMargin
  }

  @Benchmark
  def create_fN =
    compiler.compile(source)
}
