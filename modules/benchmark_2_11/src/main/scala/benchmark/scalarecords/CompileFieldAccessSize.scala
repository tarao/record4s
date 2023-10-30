package benchmark
package scalarecords

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations._

import records._

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
class CompileFieldAccessSize {
  @Param(Array("1", "2", "4", "8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32"))
  var size: Int = 0

  var source: String = ""

  var compiler: Compiler = null

  @Setup(Level.Iteration)
  def setup(): Unit = {
    compiler = new Compiler

    source = s"""
      |import benchmark.scalarecords.CompileFieldAccessSize._
      |object A {
      |  r_${size}.f${size}
      |}
      |""".stripMargin
  }

  @Benchmark
  def access_fN =
    compiler.compile(source)
}

object CompileFieldAccessSize {
  val r_1 = Rec(
    f1 = 1
  )

  val r_2 = Rec(
    f1 = 1,
    f2 = 2
  )

  val r_4 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4
  )

  val r_6 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6
  )

  val r_8 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8
  )

  val r_10 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
    f10 = 10
  )

  val r_12 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
    f10 = 10,
    f11 = 11,
    f12 = 12
  )

  val r_14 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
    f10 = 10,
    f11 = 11,
    f12 = 12,
    f13 = 13,
    f14 = 14
  )

  val r_16 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
    f10 = 10,
    f11 = 11,
    f12 = 12,
    f13 = 13,
    f14 = 14,
    f15 = 15,
    f16 = 16
  )

  val r_18 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
    f10 = 10,
    f11 = 11,
    f12 = 12,
    f13 = 13,
    f14 = 14,
    f15 = 15,
    f16 = 16,
    f17 = 17,
    f18 = 18
  )

  val r_20 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f20 = 20
  )

  val r_22 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f22 = 22
  )

  val r_24 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f24 = 24
  )

  val r_26 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f26 = 26
  )

  val r_28 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f28 = 28
  )

  val r_30 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f30 = 30
  )

  val r_32 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8,
    f9 = 9,
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
    f32 = 32
  )
}
