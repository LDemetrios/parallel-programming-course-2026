package org.ldemetrios.collectors

import org.ldemetrios.BUCKETS
import org.ldemetrios.BUCKET_WIDTH
import org.ldemetrios.TraversableThreadLocal
import org.ldemetrios.api.MetricsCollector
import org.ldemetrios.api.Snapshot
import org.ldemetrios.bucket
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.math.max
import kotlin.math.min

class ThreadLocalCollector : MetricsCollector {
    private val graveyard = StateBuffer()
    private val state = TraversableThreadLocal { ThreadState() }

    override fun record(value: Long) {
        val state = state.obtain()
        val bucket = bucket(value)

        state.buckets.setRelease(bucket, state.buckets.getPlain(bucket) + 1)
        state.count.setRelease(state.count.getPlain() + 1)
        state.sum.setRelease(state.sum.getPlain() + value)

        if (value < state.min.getPlain()) state.min.setRelease(value)
        if (value > state.max.getPlain()) state.max.setRelease(value)
    }

    override fun snapshot(): Snapshot {
        val (result, states) = state.withLock {
            val states = mutableListOf<ThreadState>()
            val graveyardCopy = graveyard.copy()
            state.traverse(
                { states.add(it) },
                onDelete = { sepulchre(it) },
            )
            graveyardCopy to states
        }

        for (state in states) {
            for (i in 0..<BUCKETS) {
                result.buckets[i] += state.buckets.get(i)
            }
            val stateCount = state.count.get()
            result.count += stateCount
            result.sum += state.sum.get()
            if (stateCount > 0) {
                result.min = min(result.min, state.min.get())
                result.max = max(result.max, state.max.get())
            }
        }

        return Snapshot.from(result.buckets, result.count, result.sum, result.min, result.max, BUCKET_WIDTH)
    }

    private fun sepulchre(state: ThreadState) {
        for (i in 0..<BUCKETS) {
            graveyard.buckets[i] += state.buckets.get(i)
        }
        graveyard.count += state.count.get()
        graveyard.sum += state.sum.get()
        val stateCount = state.count.get()
        if (stateCount > 0) {
            val stateMin = state.min.get()
            if (stateMin < graveyard.min) graveyard.min = stateMin
            val stateMax = state.max.get()
            if (stateMax > graveyard.max) graveyard.max = stateMax
        }
    }

    internal class ThreadState() {
        val buckets: AtomicLongArray = AtomicLongArray(BUCKETS)
        val count: AtomicLong = AtomicLong()
        val sum: AtomicLong = AtomicLong()
        val min: AtomicLong = AtomicLong(Long.MAX_VALUE)
        val max: AtomicLong = AtomicLong()
    }
}
