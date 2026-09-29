package org.ldemetrios.collectors

import org.ldemetrios.api.MetricsCollector

enum class CollectorFactory(val id: String, val generator: () -> MetricsCollector) {
    SINGLE("single", { SingleThreadCollector() }),
    NOOP("no-op", { NoopCollector() }),
    SYNC("sync", { SynchronizedCollector() }),
    STRIPED("striped", { StripedLockCollector() }),
    THREAD_LOCAL("thread-local", { ThreadLocalCollector() }),
    DOUBLE_BUFFERED("double-buffered", { DoubleBufferedCollector() }),
    BROKEN_DOUBLE_BUFFERED("broken-double-buffered", { BrokenDoubleBufferedCollector() });

    companion object {
        fun byId(id: String) = entries.find { it.id == id }
            ?: throw IllegalArgumentException("Unknown collector '$id'. Available: $entries")
    }
}