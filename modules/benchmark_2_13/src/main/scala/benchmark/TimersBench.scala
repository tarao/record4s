package benchmark

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@State(Scope.Thread)
class TimersBench {
  var lastValue: Long = 0

  @Benchmark
  def latencyNanotime(): Long = System.nanoTime()

  @Benchmark
  def granularityNanotime(): Long = {
    var cur: Long = 0
    while ({
      cur = System.nanoTime()
      cur == lastValue
    }) ()
    lastValue = cur
    cur
  }
}
