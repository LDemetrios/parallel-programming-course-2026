package org.ldemetrios.collectors

import org.ldemetrios.BUCKETS
import org.ldemetrios.BUCKET_WIDTH
import org.ldemetrios.api.Snapshot
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

@Suppress("ArrayInDataClass")
data class StateBuffer(
    val buckets: LongArray = LongArray(BUCKETS),
    var count: Long = 0,
    var sum: Long = 0,
    var min: Long = Long.MAX_VALUE,
    var max: Long = 0,
) {

    fun clear() {
        Arrays.fill(buckets, 0)
        count = 0
        sum = 0
        min = Long.MAX_VALUE
        max = 0
    }

    fun copy() = copy(buckets = buckets.copyOf())

    fun toSnapshot() = Snapshot.from(buckets, count, sum, min, max, BUCKET_WIDTH)

    operator fun plusAssign(another: StateBuffer) {
        for (i in 0..<BUCKETS) buckets[i] += another.buckets[i]
        count += another.count
        sum += another.sum
        min = min(min, another.min)
        max = max(max, another.max)
    }
}