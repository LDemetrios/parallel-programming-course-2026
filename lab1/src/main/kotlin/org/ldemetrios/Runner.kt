package org.ldemetrios

import org.ldemetrios.api.MetricsCollector
import java.util.concurrent.CountDownLatch

class StopFlag {
    @Volatile
    var value: Boolean = false
}

class Runner(
    val values: LongArray,
    val threads: Int,
    val collector: MetricsCollector,
) {
    private val ready = CountDownLatch(threads)
    private val start = CountDownLatch(1)
    private val stop = StopFlag()
    private val ops = LongArray(threads)
    private val list = Array(threads) {
        val thread = Thread {
            var local: Long = 0
            var valueIndex = (it * 1000) % values.size
            ready.countDown()
            start.await()
            while (!stop.value) {
                collector.record(values[valueIndex])
                local++
                valueIndex++
                if (valueIndex == values.size) valueIndex = 0
            }
            ops[it] = local
        }
        thread.name = "bench-$it"
        thread
    }

    fun launch() {
        list.forEach { it.start() }
        ready.await()
    }

    fun flush() {
        start.countDown()
    }

    fun stop() {
        stop.value = true
        for (thread in list) thread.join()
    }

    fun result() = ops.copyOf()
}

fun benchWrites(collector: MetricsCollector, values: LongArray, seconds: Int, threads: Int): Double {
    val runner = Runner(values, threads, collector)

    runner.launch()
    val time = time {
        runner.flush()
        Thread.sleep(seconds * 1000L)
        runner.stop()
    }

    return runner.result().sum() / time
}

data class RWBenchReport(val runs: Int, val bucketsLtCount: Int, val bucketsGtCount: Int, val actualSum: Long, val countSum: Long) {
    val broken get() = bucketsGtCount + bucketsLtCount
}

fun benchRW(collector: MetricsCollector, values: LongArray, snapshots: Int, threads: Int): RWBenchReport {
    val runner = Runner(values, threads, collector)

    runner.launch()
    runner.flush()

    var bucketsLessThanCount = 0
    var bucketsGreaterThanCount = 0
    repeat(snapshots) {
        val snapshot = collector.snapshot()
        val bucketSum = snapshot.buckets.sum()
        when {
            bucketSum < snapshot.count -> bucketsLessThanCount++
            bucketSum > snapshot.count -> bucketsGreaterThanCount++
        }
    }

    runner.stop()

    return RWBenchReport(snapshots, bucketsLessThanCount, bucketsGreaterThanCount, runner.result().sum(), collector.snapshot().count)
}
