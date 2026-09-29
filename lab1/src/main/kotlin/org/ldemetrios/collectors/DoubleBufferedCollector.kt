package org.ldemetrios.collectors

import org.ldemetrios.TraversableThreadLocal
import org.ldemetrios.api.MetricsCollector
import org.ldemetrios.api.Snapshot
import org.ldemetrios.bucket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

class DoubleBufferedCollector : MetricsCollector {
    private val global = StateBuffer()

    @Volatile
    private var active = 0

    private val buffers = TraversableThreadLocal { ThreadState() }

    override fun record(value: Long) {
        val buffers = buffers.obtain()
        var writeBuffer: Int
        while (true) {
            writeBuffer = active
            buffers.inside.set(writeBuffer)
            if (active == writeBuffer) break
            buffers.inside.setRelease(NOWHERE)
            Thread.onSpinWait()
        }

        val buffer = buffers.buffers[writeBuffer]

        buffer.buckets[bucket(value)]++
        buffer.count++
        buffer.sum += value
        if (value < buffer.min) buffer.min = value
        if (value > buffer.max) buffer.max = value

        buffers.inside.setRelease(NOWHERE)
    }

    override fun snapshot(): Snapshot {
        buffers.withLock {
            val old = active
            active = 1 - old

            buffers.traverseKeepingDead {
                while (it.inside.get() == old) Thread.onSpinWait()
            }

            buffers.traverse(
                onEach = {
                    global += it.buffers[old]
                    it.buffers[old].clear()
                },
                onDelete = {
                    val other = 1 - old
                    global += it.buffers[other]
                    it.buffers[other].clear()
                },
            )
            return global.toSnapshot()
        }
    }

    internal class ThreadState() {
        val buffers: Array<StateBuffer> = arrayOf(StateBuffer(), StateBuffer())
        val inside: AtomicInteger = AtomicInteger(NOWHERE)
    }
}
