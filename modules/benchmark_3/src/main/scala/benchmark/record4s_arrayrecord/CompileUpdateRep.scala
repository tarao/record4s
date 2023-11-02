package benchmark
package record4s_arrayrecord

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*

import com.github.tarao.record4s.ArrayRecord

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
class CompileUpdateRep {
  @Param(Array("1", "5", "10", "15", "20", "25"))
  var repetitions: Int = 0

  val size: Int = 5

  var source: String = ""

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    compiler = new Compiler

    val update = (1 to repetitions).map(_ => s"  r + (f${size} = r.f${size} + 1)").mkString("\n")
    source = s"""
      |import benchmark.record4s_arrayrecord.CompileUpdateRep.r
      |object A {
      |${update}
      |}
      |""".stripMargin
  }

  @Benchmark
  def update_f5 =
    compiler.compile(source)
}

object CompileUpdateRep {
  val r = ArrayRecord(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
  )
}
