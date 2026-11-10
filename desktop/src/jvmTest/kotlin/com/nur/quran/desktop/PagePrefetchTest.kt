package com.nur.quran.desktop

import com.nur.quran.desktop.data.PagePrefetch
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagePrefetchTest {

    @Test
    fun orderingPrioritizesNeighboursThenAssignmentThenRest() {
        assertEquals(listOf(6, 4, 7, 8, 3, 1, 2, 9, 10), PagePrefetch.order(5, 3, 8, 10))
        assertEquals(603, PagePrefetch.order(1, 1, 604).size)
        assertEquals(603, PagePrefetch.order(604, 1, 604).distinct().size)
        assertTrue(PagePrefetch.order(0, 1, 604).isEmpty())
        assertTrue(PagePrefetch.order(605, 1, 604).isEmpty())
    }

    @Test
    fun workersStayBoundedAndStartAdjacentPagesFirst() {
        val started = Collections.synchronizedList(mutableListOf<Int>())
        val active = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val pages = listOf(6, 4, 7, 8)
        val job = PagePrefetch.start(
            pages = pages,
            concurrency = 2,
            fetch = { page ->
                started.add(page)
                peak.updateAndGet { maxOf(it, active.incrementAndGet()) }
                Thread.sleep(5)
                active.decrementAndGet()
            }
        )
        job.join()
        assertEquals(pages.toSet(), started.toSet())
        assertEquals(pages.size, started.size)
        assertTrue("peak concurrency was ${peak.get()}", peak.get() <= 2)
        assertTrue("expected a neighbour of 5 first, got $started", started.first() in listOf(6, 4))
    }

    @Test
    fun stopPreventsFurtherFetches() {
        val calls = AtomicInteger(0)
        val gate = Object()
        val job = PagePrefetch.start(
            pages = (1..6).toList(),
            concurrency = 1,
            fetch = {
                calls.incrementAndGet()
                synchronized(gate) { gate.wait(2000) }
            }
        )
        // Let the single worker start its first fetch, then stop.
        val deadline = System.currentTimeMillis() + 1000
        while (calls.get() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(5)
        job.stop()
        synchronized(gate) { gate.notifyAll() }
        job.join()
        assertEquals(1, calls.get())
    }

    @Test
    fun stopsAfterThreeConsecutiveFailures() {
        val calls = AtomicInteger(0)
        PagePrefetch.start(
            pages = (1..10).toList(),
            concurrency = 1,
            fetch = {
                calls.incrementAndGet()
                throw IllegalStateException("offline")
            }
        ).join()
        assertEquals(3, calls.get())
    }
}
