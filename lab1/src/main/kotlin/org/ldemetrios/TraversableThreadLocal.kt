package org.ldemetrios

class TraversableThreadLocal<T>(supplier: () -> T) {
    @PublishedApi
    internal data class Entry<T>(val value: T, val owner: Thread) {
        override fun hashCode(): Int = System.identityHashCode(value) * 31 + owner.hashCode()
        override fun equals(other: Any?): Boolean =
            this === other ||
                other is Entry<*> && this.value === other.value && this.owner == other.owner
    }

    private val holder = ThreadLocal.withInitial {
        val entry = Entry(supplier(), Thread.currentThread())
        synchronized(snapLock) { set[entry] = Unit }
        entry
    }

    @PublishedApi
    internal val set = HashMap<Entry<T>, Unit>()

    @PublishedApi
    internal val snapLock = Any()

    inline fun <R> withLock(body: () -> R) = synchronized(snapLock) { body() }

    inline fun traverse(
        crossinline onEach: (T) -> Unit,
        crossinline onDelete: (T) -> Unit,
    ) = synchronized(snapLock) {
        set.keys.removeIf {
            onEach(it.value)
            val remove = !it.owner.isAlive
            if (remove) onDelete(it.value)
            remove
        }
    }

    inline fun traverse(crossinline onEach: (T) -> Unit) = traverse(onEach, {})

    inline fun traverseKeepingDead(crossinline onEach: (T) -> Unit) = synchronized(snapLock) {
        set.keys.forEach { onEach(it.value) }
    }

    fun obtain() = holder.get()!!.value
}
