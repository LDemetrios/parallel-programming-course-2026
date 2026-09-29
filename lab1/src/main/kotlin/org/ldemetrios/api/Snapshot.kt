package org.ldemetrios.api

@JvmRecord
data class Snapshot(
    val buckets: LongArray,
    val count: Long,
    val sum: Long,
    val min: Long,
    val max: Long,
    val p50: Long,
    val p99: Long,
) {
    companion object {
        fun from(buckets: LongArray, count: Long, sum: Long, min: Long, max: Long, bucketWidth: Int): Snapshot {
            val copy = buckets.copyOf(buckets.size)
            return Snapshot(
                copy, count, sum, min, max,
                percentile(copy, count, 50, bucketWidth),
                percentile(copy, count, 99, bucketWidth),
            )
        }

        private fun percentile(buckets: LongArray, count: Long, percentile: Int, bucketWidth: Int): Long {
            val threshold = (count * percentile + 99) / 100
            var seen: Long = 0
            for (i in buckets.indices) {
                seen += buckets[i]
                if (seen >= threshold) {
                    return i.toLong() * bucketWidth
                }
            }
            return (buckets.size - 1).toLong() * bucketWidth
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Snapshot) return false

        if (count != other.count) return false
        if (sum != other.sum) return false
        if (min != other.min) return false
        if (max != other.max) return false
        if (p50 != other.p50) return false
        if (p99 != other.p99) return false
        if (!buckets.contentEquals(other.buckets)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = count.hashCode()
        result = 31 * result + sum.hashCode()
        result = 31 * result + min.hashCode()
        result = 31 * result + max.hashCode()
        result = 31 * result + p50.hashCode()
        result = 31 * result + p99.hashCode()
        result = 31 * result + buckets.contentHashCode()
        return result
    }
}
