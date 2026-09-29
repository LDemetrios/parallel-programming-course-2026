package org.ldemetrios.api

interface MetricsCollector {
    fun record(value: Long)

    fun snapshot(): Snapshot
}
