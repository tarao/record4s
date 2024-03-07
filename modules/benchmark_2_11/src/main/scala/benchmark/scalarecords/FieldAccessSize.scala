package benchmark
package scalarecords

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations._

import records._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class FieldAccessSize {
  // Use `var` to prevent constant folding
  var r_1 = Rec(
    f1 = 1
  )

  var r_2 = Rec(
    f1 = 1,
    f2 = 2
  )

  var r_4 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4
  )

  var r_6 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6
  )

  var r_8 = Rec(
    f1 = 1,
    f2 = 2,
    f3 = 3,
    f4 = 4,
    f5 = 5,
    f6 = 6,
    f7 = 7,
    f8 = 8
  )

  var r_10 = Rec(
    f1  = 1,
    f2  = 2,
    f3  = 3,
    f4  = 4,
    f5  = 5,
    f6  = 6,
    f7  = 7,
    f8  = 8,
    f9  = 9,
    f10 = 10
  )

  var r_12 = Rec(
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
    f12 = 12
  )

  var r_14 = Rec(
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
    f14 = 14
  )

  var r_16 = Rec(
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
    f16 = 16
  )

  var r_18 = Rec(
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
    f18 = 18
  )

  var r_20 = Rec(
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
    f20 = 20
  )

  var r_22 = Rec(
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
    f22 = 22
  )

  var r_24 = Rec(
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
    f24 = 24
  )

  var r_26 = Rec(
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
    f26 = 26
  )

  var r_28 = Rec(
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
    f28 = 28
  )

  var r_30 = Rec(
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
    f30 = 30
  )

  var r_32 = Rec(
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
    f32 = 32
  )

  @Benchmark
  def access_f1 = r_1.f1

  @Benchmark
  def access_f2 = r_2.f2

  @Benchmark
  def access_f4 = r_4.f4

  @Benchmark
  def access_f6 = r_6.f6

  @Benchmark
  def access_f8 = r_8.f8

  @Benchmark
  def access_f10 = r_10.f10

  @Benchmark
  def access_f12 = r_12.f12

  @Benchmark
  def access_f14 = r_14.f14

  @Benchmark
  def access_f16 = r_16.f16

  @Benchmark
  def access_f18 = r_18.f18

  @Benchmark
  def access_f20 = r_20.f20

  @Benchmark
  def access_f22 = r_22.f22

  @Benchmark
  def access_f24 = r_24.f24

  @Benchmark
  def access_f26 = r_26.f26

  @Benchmark
  def access_f28 = r_28.f28

  @Benchmark
  def access_f30 = r_30.f30

  @Benchmark
  def access_f32 = r_32.f32
}
