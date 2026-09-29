package org.ldemetrios.collectors

import org.ldemetrios.TraversableThreadLocal
import org.ldemetrios.api.MetricsCollector
import org.ldemetrios.api.Snapshot
import org.ldemetrios.bucket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

class BrokenDoubleBufferedCollector : MetricsCollector {
    private val global = StateBuffer()

    @Volatile
    private var active = 0

    private val buffers = TraversableThreadLocal { ThreadState() }

    override fun record(value: Long) {
        val state = buffers.obtain()
        val writeBuffer = active
        state.inside.set(writeBuffer)

        val buffer = state.buffers[writeBuffer]
        buffer.buckets[bucket(value)]++
        buffer.count++
        buffer.sum += value
        if (value < buffer.min) buffer.min = value
        if (value > buffer.max) buffer.max = value

        state.inside.setRelease(NOWHERE)
    }

    override fun snapshot(): Snapshot {
        buffers.withLock {
            val old = active
            active = 1 - old

            buffers.traverseKeepingDead {
                while (it.inside.get() == old) Thread.onSpinWait()
            }

            buffers.traverse {
                global += it.buffers[old]
                it.buffers[old].clear()
            }

            return global.toSnapshot()
        }
    }

    internal class ThreadState {
        val buffers: Array<StateBuffer> = arrayOf(StateBuffer(), StateBuffer())
        val inside: AtomicInteger = AtomicInteger(NOWHERE)
    }
}

const val NOWHERE = -1
