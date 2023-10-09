package benchmark.shapeless

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations._
import scala.annotation.nowarn

import shapeless.record._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@nowarn // suppress deprecation of symbols
class FieldAccess {
  // Use `var` to prevent constant folding
  var r = Record(
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
    f32 = 32,
  )

  @Benchmark
  def access_f1 = r.get('f1)

  @Benchmark
  def access_f2 = r.get('f2)

  @Benchmark
  def access_f4 = r.get('f4)

  @Benchmark
  def access_f6 = r.get('f6)

  @Benchmark
  def access_f8 = r.get('f8)

  @Benchmark
  def access_f10 = r.get('f10)

  @Benchmark
  def access_f12 = r.get('f12)

  @Benchmark
  def access_f14 = r.get('f14)

  @Benchmark
  def access_f16 = r.get('f16)

  @Benchmark
  def access_f18 = r.get('f18)

  @Benchmark
  def access_f20 = r.get('f20)

  @Benchmark
  def access_f22 = r.get('f22)

  @Benchmark
  def access_f24 = r.get('f24)

  @Benchmark
  def access_f26 = r.get('f26)

  @Benchmark
  def access_f28 = r.get('f28)

  @Benchmark
  def access_f30 = r.get('f30)

  @Benchmark
  def access_f32 = r.get('f32)
}
