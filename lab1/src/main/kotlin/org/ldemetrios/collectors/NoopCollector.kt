package org.ldemetrios.collectors

import org.ldemetrios.BUCKETS
import org.ldemetrios.BUCKET_WIDTH
import org.ldemetrios.api.MetricsCollector
import org.ldemetrios.api.Snapshot

class NoopCollector : MetricsCollector {
    private val lock = Any()
    override fun record(value: Long) = synchronized(lock) {}
    override fun snapshot(): Snapshot = Snapshot.from(LongArray(BUCKETS), 0, 0, 0, 0, BUCKET_WIDTH)
}
