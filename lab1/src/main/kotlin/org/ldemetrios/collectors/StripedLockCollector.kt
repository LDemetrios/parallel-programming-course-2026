package org.ldemetrios.collectors

import org.ldemetrios.BUCKETS
import org.ldemetrios.BUCKET_WIDTH
import org.ldemetrios.api.MetricsCollector
import org.ldemetrios.api.Snapshot
import org.ldemetrios.bucket
import org.ldemetrios.max
import org.ldemetrios.min
import java.util.concurrent.atomic.AtomicLong

class StripedLockCollector : MetricsCollector {
    private val locks = Array(STRIPES) { Any() }
    private val buckets = LongArray(BUCKETS)
    private val count = AtomicLong()
    private val sum = AtomicLong()
    private val min = AtomicLong(Long.MAX_VALUE)
    private val max = AtomicLong()

    override fun record(value: Long) {
        val bucket = bucket(value)
        synchronized(locks[bucket % STRIPES]) {
            buckets[bucket]++
        }
        count.incrementAndGet()
        sum.addAndGet(value)
        min.min(value)
        max.max(value)
    }

    override fun snapshot(): Snapshot {
        val out = LongArray(BUCKETS)
        for (stripe in 0..<STRIPES) {
            synchronized(locks[stripe]) {
                var bucket = stripe
                while (bucket < BUCKETS) {
                    out[bucket] = buckets[bucket]
                    bucket += STRIPES
                }
            }
        }
        return Snapshot.from(out, count.get(), sum.get(), min.get(), max.get(), BUCKET_WIDTH)
    }

    companion object {
        private const val STRIPES = 16
    }
}
