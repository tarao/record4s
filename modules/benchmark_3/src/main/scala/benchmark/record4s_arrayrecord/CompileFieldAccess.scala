package benchmark
package record4s_arrayrecord

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*

import com.github.tarao.record4s.ArrayRecord

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
class CompileFieldAccess {
  @Param(
    Array(
      "1",
      "2",
      "4",
      "8",
      "10",
      "12",
      "14",
      "16",
      "18",
      "20",
      "22",
      "24",
      "26",
      "28",
      "30",
      "32",
    ),
  )
  var index: Int = 0

  var source: String = ""

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    compiler = new Compiler

    source = s"""
                |import benchmark.record4s_arrayrecord.CompileFieldAccess.r
                |object A {
                |  r.f${index}
                |}
                |""".stripMargin
  }

  @Benchmark
  def access_fN =
    compiler.compile(source)
}

object CompileFieldAccess {
  val r = ArrayRecord(
    f1  = 1,
    f2  = 2,
    f3  = 3,
    f4  = 4,
    f5  = 5,
    f6  = 6,
    f7  = 7,
    f8  = 8,
    f9  = 9,
    f10 = 10,
    f11 = 11,
    f12 = 12,
    f13 = 13,
    f14 = 14,
    f15 = 15,
    f16 = 16,
    f17 = 17,
    f18 = 18,
    f19 = 19,
    f20 = 20,
    f21 = 21,
    f22 = 22,
    f23 = 23,
    f24 = 24,
    f25 = 25,
    f26 = 26,
    f27 = 27,
    f28 = 28,
    f29 = 29,
    f30 = 30,
    f31 = 31,
    f32 = 32,
  )
}
