package com.nur.quran.desktop.data

/**
 * Background warm-up for the mushaf page cache, ported from the web
 * `utils/pagePrefetch.js`: nearby pages first, then the rest of the range.
 *
 * The Mushaf view used to require a network round-trip the first time a page
 * was opened. The page reader now warms the neighbouring pages of its range in
 * the background so paging stays instant (and works offline afterwards).
 */
object PagePrefetch {

    /**
     * Page order for prefetching: neighbours of [current], then outward through
     * the assignment window, then the remaining pages up to [total].
     * Returns an empty list for an out-of-range [current].
     */
    fun order(
        current: Int,
        start: Int = 1,
        end: Int = 604,
        total: Int = 604
    ): List<Int> {
        if (current < 1 || current > total) return emptyList()
        val seen = mutableSetOf(current)
        val ordered = mutableListOf<Int>()
        fun add(page: Int) {
            if (page in 1..total && seen.add(page)) ordered.add(page)
        }
        add(current + 1)
        add(current - 1)
        val first = start.coerceIn(1, total)
        val last = end.coerceIn(1, total)
        var page = maxOf(current + 2, first)
        while (page <= last) {
            add(page)
            page++
        }
        page = minOf(current - 2, last)
        while (page >= first) {
            add(page)
            page--
        }
        for (p in 1..total) add(p)
        return ordered
    }

    /** Handle for a running prefetch job; [join] blocks until workers finish. */
    class Job internal constructor(
        val stop: () -> Unit,
        val join: () -> Unit
    )

    /**
     * Bounded worker pool (max two threads, like the web util). Stops starting
     * new work when [stop] is called or after three consecutive fetch
     * failures, so an offline machine is not hammered.
     */
    fun start(
        pages: List<Int>,
        fetch: (Int) -> Unit,
        concurrency: Int = 2,
        canContinue: () -> Boolean = { true }
    ): Job {
        val lock = Any()
        var stopped = false
        var index = 0
        var failures = 0

        fun nextPage(): Int? = synchronized(lock) {
            if (stopped || index >= pages.size || !canContinue()) return null
            pages[index++]
        }

        val workerCount = concurrency.coerceIn(1, 2)
        val workers = (1..workerCount).map {
            Thread({
                while (true) {
                    val page = nextPage() ?: break
                    try {
                        fetch(page)
                        synchronized(lock) { failures = 0 }
                    } catch (_: Throwable) {
                        synchronized(lock) {
                            failures++
                            if (failures >= 3) stopped = true
                        }
                    }
                }
            }, "quran-page-prefetch").apply {
                isDaemon = true
                start()
            }
        }.toList()

        return Job(
            stop = { synchronized(lock) { stopped = true } },
            join = { workers.forEach { runCatching { it.join() } } }
        )
    }
}
