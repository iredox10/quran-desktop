package com.nur.quran.desktop

import com.nur.quran.desktop.ui.planner.ArchivedPlan
import com.nur.quran.desktop.ui.planner.PlannerHistory
import com.nur.quran.shared.PlannerAssignment
import com.nur.quran.shared.PlannerItem
import com.nur.quran.shared.ReadingPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerHistoryTest {

    private fun item(range: String, pages: IntRange, route: String = "page"): PlannerItem =
        PlannerItem(
            id = pages.first,
            title = "Item $range",
            subtitle = "Pages ${pages.first}-${pages.last}",
            route = route,
            rangeValue = range,
            pageStart = pages.first,
            pageEnd = pages.last
        )

    private fun assignment(day: Int, date: String, pages: IntRange): PlannerAssignment =
        PlannerAssignment(
            dayNumber = day,
            date = date,
            unitType = "page",
            title = "Day $day",
            subtitle = "Pages ${pages.first}-${pages.last}",
            startUnit = pages.first,
            endUnit = pages.last,
            primaryRoute = "page",
            pageStart = pages.first,
            pageEnd = pages.last,
            items = listOf(item("$day", pages))
        )

    private fun plan(
        id: String,
        completedDays: List<Int> = emptyList(),
        progress: Map<Int, Int> = emptyMap(),
        readPages: Map<Int, List<Int>> = emptyMap(),
        reflections: Map<Int, String> = emptyMap(),
        completedAt: Map<Int, String> = emptyMap()
    ) = ReadingPlan(
        id = id,
        unitType = "page",
        title = "Plan $id",
        durationDays = 2,
        startDate = "2026-11-01",
        startUnit = 1,
        endUnit = 4,
        assignments = listOf(
            assignment(1, "2026-11-01", 1..2),
            assignment(2, "2026-11-02", 3..4)
        ),
        assignmentProgress = progress,
        assignmentReadPages = readPages,
        assignmentCompletedItems = emptyMap(),
        assignmentCompletedAt = completedAt,
        completedDays = completedDays,
        assignmentReflections = reflections
    )

    @Test
    fun summaryCountsDaysPagesAndReadingTime() {
        val done = plan(
            id = "done",
            progress = mapOf(1 to 1, 2 to 1),
            completedAt = mapOf(1 to "2026-11-01", 2 to "2026-11-02")
        )
        val summary = PlannerHistory.summarize(
            plan = done,
            studySessions = mapOf("2026-11-01" to 12, "2026-11-02" to 8, "2026-11-09" to 30)
        )
        assertEquals(2, summary.totalDays)
        assertEquals(2, summary.completedDays)
        assertEquals(4, summary.pagesRead)
        assertEquals(4, summary.totalPages)
        // Only scheduled dates count; the unrelated 2026-11-09 session is ignored.
        assertEquals(20L * 60L, summary.readingSeconds)
        assertTrue(summary.isComplete)
        assertEquals("Completed", summary.statusLabel)
        assertEquals("2026-11-02", summary.completedOn)
    }

    @Test
    fun endedEarlyPlanIsNotComplete() {
        val partial = plan(id = "partial", progress = mapOf(1 to 1), readPages = mapOf(1 to listOf(1, 2)))
        val summary = PlannerHistory.summarize(partial)
        assertEquals(1, summary.completedDays)
        assertEquals(2, summary.pagesRead)
        assertFalse(summary.isComplete)
        assertEquals("Ended early", summary.statusLabel)
        assertNull(summary.archivedAt)
    }

    @Test
    fun reflectionsCombineStoreAndLegacyValues() {
        val p = plan(
            id = "reflect",
            reflections = mapOf(1 to "Legacy note", 2 to "   ")
        )
        val summary = PlannerHistory.summarize(
            plan = p,
            reflectionsByDay = mapOf(1 to "Stored note")
        )
        assertEquals(1, summary.reflections.size)
        assertEquals("Stored note", summary.reflections.single().text)
        assertEquals(1, summary.reflections.single().dayNumber)
    }

    @Test
    fun buildOrdersActivePlanThenArchivedNewestFirst() {
        val active = plan(id = "active", progress = mapOf(1 to 1, 2 to 1))
        val other = plan(id = "other")
        val archived = listOf(
            ArchivedPlan(plan = plan(id = "old"), archivedAt = "2026-10-01"),
            ArchivedPlan(plan = plan(id = "new"), archivedAt = "2026-10-20")
        )
        val built = PlannerHistory.build(
            livePlans = listOf(other, active),
            archived = archived,
            activePlanId = "active"
        )
        assertEquals(listOf("active", "other", "new", "old"), built.map { it.plan.id })
        assertEquals("2026-10-20", built[2].archivedAt)
    }

    @Test
    fun completedFilterKeepsOnlyFinishedPlans() {
        val summaries = listOf(
            PlannerHistory.summarize(plan(id = "a", progress = mapOf(1 to 1, 2 to 1))),
            PlannerHistory.summarize(plan(id = "b"))
        )
        assertEquals(listOf("a"), PlannerHistory.filterCompleted(summaries, true).map { it.plan.id })
        assertEquals(2, PlannerHistory.filterCompleted(summaries, false).size)
    }

    @Test
    fun timeFormattingMatchesHistoryCopy() {
        assertEquals("No reading time recorded", PlannerHistory.formatTime(0))
        assertEquals("8m recorded", PlannerHistory.formatTime(8 * 60L))
        assertEquals("1h 5m recorded", PlannerHistory.formatTime(65 * 60L))
        assertEquals("2h recorded", PlannerHistory.formatTime(120 * 60L))
    }
}
