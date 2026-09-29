package org.ldemetrios

import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow

inline fun time(crossinline body: () -> Unit): Double {
    val t0 = System.nanoTime()
    body()
    val t1 = System.nanoTime()
    return (t1 - t0) / 1e9
}

fun DoubleArray.median(): Double {
    val sortedValues = sorted()
    val middle = sortedValues.size / 2
    return when {
        sortedValues.size % 2 == 1 -> sortedValues[middle]
        else -> (sortedValues[middle - 1] + sortedValues[middle]) / 2.0
    }
}

fun Double.format(string: String) = "%${string}f".format(this)

private const val MAX_VALUE = 1023
private const val ZIPF_S = 1.15

fun zipf(seed: Long, size: Int = 1 shl 20): LongArray {
    val cumulative = DoubleArray(MAX_VALUE + 1)
    var total = 0.0
    for (k in 1..MAX_VALUE) {
        total += 1.0 / k.toDouble().pow(ZIPF_S)
        cumulative[k] = total
    }
    for (k in 1..MAX_VALUE) {
        cumulative[k] /= total
    }

    val random = Random(seed)
    val values = LongArray(size)
    for (i in values.indices) {
        val needle = random.nextDouble()
        var low = 1
        var high = MAX_VALUE
        while (low < high) {
            val mid = (low + high) ushr 1
            if (cumulative[mid] >= needle) {
                high = mid
            } else {
                low = mid + 1
            }
        }
        values[i] = low.toLong()
    }
    return values
}

const val BUCKETS: Int = 256
const val BUCKET_WIDTH: Int = 4

fun bucket(value: Long): Int {
    if (value <= 0) {
        return 0
    }
    val bucket: Long = value / BUCKET_WIDTH
    return if (bucket >= BUCKETS) BUCKETS - 1 else bucket.toInt()
}

inline fun AtomicLong.update(func: (Long) -> Long) {
    do {
        val current = get()
        val new = func(current)
    } while (!compareAndSet(current, new))
}

@Suppress("NOTHING_TO_INLINE")
inline fun AtomicLong.min(x: Long): Unit = update { if (x < it) x else return@min }

@Suppress("NOTHING_TO_INLINE")
inline fun AtomicLong.max(x: Long): Unit = update { if (x > it) x else return@max }