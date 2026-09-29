package org.ldemetrios.collectors

import org.ldemetrios.BUCKETS
import org.ldemetrios.BUCKET_WIDTH
import org.ldemetrios.api.MetricsCollector
import org.ldemetrios.api.Snapshot
import org.ldemetrios.bucket

class SynchronizedCollector : MetricsCollector {
    private val buckets = LongArray(BUCKETS)
    private var count: Long = 0
    private var sum: Long = 0
    private var min: Long = Long.MAX_VALUE
    private var max: Long = 0

    override fun record(value: Long) = synchronized(this) {
        buckets[bucket(value)]++
        count++
        sum += value
        if (value < min) {
            min = value
        }
        if (value > max) {
            max = value
        }
    }

    override fun snapshot(): Snapshot = synchronized(this) {
        return Snapshot.from(buckets, count, sum, min, max, BUCKET_WIDTH)
    }
}
