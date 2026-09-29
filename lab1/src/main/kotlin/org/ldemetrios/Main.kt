package org.ldemetrios

import org.ldemetrios.collectors.CollectorFactory
import kotlin.random.Random

fun main(vararg args: String) {
    val test = args[0]
    val collectorType = CollectorFactory.byId(args[1])
    val threads = args[2].toInt()
    val skipWarmup = args.getOrNull(3) == "--skip-warmup"

    println("Available processors: ${Runtime.getRuntime().availableProcessors()}")

    val rnd = Random(System.nanoTime())
    if (!skipWarmup) {
        benchWrites(collectorType.generator(), zipf(rnd.nextLong()), 5, threads)
        benchRW(collectorType.generator(), zipf(rnd.nextLong()), 10_000, threads)
    }

    when (test) {
        "w" -> {
            val collector = collectorType.generator()
            val results = DoubleArray(5) {

                benchWrites(collector, zipf(rnd.nextLong()), 5, threads)
            }
            val snapshot = collector.snapshot()
            println(
                "$threads threads: ${results.contentToString()}, median ${results.median().let { it / 1e6 }.format(".2")} MOp/s," +
                    " total ${snapshot.count}, p50 ${snapshot.p50}, p99 ${snapshot.p99}",
            )
        }

        "rw" -> {
            val snapshots = 10000
            val collector = collectorType.generator()
            val report = benchRW(collector, zipf(rnd.nextLong()), snapshots, threads)

            println(
                "broken = ${report.broken}, which is ${(100.0 * report.broken / snapshots).format(".2")}%; " +
                    "less = ${report.bucketsLtCount}, greater = ${report.bucketsGtCount}",
            )
            println(
                "actual records = ${report.actualSum}, count sum = ${report.countSum}, delta ${report.countSum - report.actualSum}",
            )
        }

        else -> {
            println("Unknown test")
        }
    }
}
