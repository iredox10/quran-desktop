package com.nur.quran.desktop.ui.planner

import com.nur.quran.desktop.data.PlanVerseMark
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan

/**
 * Summaries for the planner history screen, ported from the web
 * `utils/plannerHistory.js` + `components/planner/PlannerHistory.jsx`.
 *
 * Desktop study time is stored as a global `date -> minutes` map (there are no
 * per-plan timers), so a plan's recorded time is the sum of the minutes logged
 * on its scheduled assignment dates.
 */
object PlannerHistory {

    data class ReflectionEntry(val dayNumber: Int, val dayTitle: String, val text: String)

    data class PlanSummary(
        val plan: ReadingPlan,
        val archivedAt: String?,
        val totalDays: Int,
        val completedDays: Int,
        val pagesRead: Int,
        val totalPages: Int,
        val readingSeconds: Long,
        val isComplete: Boolean,
        val completedOn: String?,
        val reflections: List<ReflectionEntry>,
        val bookmarkCount: Int
    ) {
        val statusLabel: String get() = if (isComplete) "Completed" else "Ended early"
    }

    /**
     * Summarize one plan. [reflectionsByDay] takes precedence over the legacy
     * reflections embedded in the plan; [studySessions] is `date -> minutes`.
     */
    fun summarize(
        plan: ReadingPlan,
        archivedAt: String? = null,
        studySessions: Map<String, Int> = emptyMap(),
        reflectionsByDay: Map<Int, String> = emptyMap(),
        bookmarks: List<PlanVerseMark> = emptyList()
    ): PlanSummary {
        var completedDays = 0
        var pagesRead = 0
        var totalPages = 0
        val reflections = mutableListOf<ReflectionEntry>()

        plan.assignments.forEach { assignment ->
            val progress = PlannerEngine.getAssignmentProgress(plan, assignment)
            if (progress.isComplete) completedDays++
            pagesRead += progress.readPagesCount
            totalPages += progress.totalPagesCount

            val text = reflectionsByDay[assignment.dayNumber]
                ?.takeIf { it.isNotBlank() }
                ?: plan.assignmentReflections[assignment.dayNumber].orEmpty()
            if (text.isNotBlank()) {
                reflections.add(ReflectionEntry(assignment.dayNumber, assignment.title, text))
            }
        }

        val recordedSeconds = plan.assignments
            .mapNotNull { it.date.takeIf { d -> d.isNotBlank() } }
            .distinct()
            .sumOf { (studySessions[it] ?: 0).toLong() * 60L }

        val totalDays = plan.assignments.size
        val completedOn = plan.assignmentCompletedAt.values
            .filter { it.isNotBlank() }
            .maxOrNull()

        return PlanSummary(
            plan = plan,
            archivedAt = archivedAt,
            totalDays = totalDays,
            completedDays = completedDays,
            pagesRead = pagesRead,
            totalPages = totalPages,
            readingSeconds = recordedSeconds,
            isComplete = totalDays > 0 && completedDays >= totalDays,
            completedOn = completedOn,
            reflections = reflections,
            bookmarkCount = bookmarks.size
        )
    }

    /**
     * Live plans first (active plan first), then archived plans newest-first.
     * [activePlanId] pins the active plan to the top of the live list.
     */
    fun build(
        livePlans: List<ReadingPlan>,
        archived: List<ArchivedPlan>,
        activePlanId: String? = null,
        studySessions: Map<String, Int> = emptyMap(),
        reflectionsFor: (String) -> Map<Int, String> = { emptyMap() },
        bookmarksFor: (String) -> List<PlanVerseMark> = { emptyList() }
    ): List<PlanSummary> {
        val orderedLive = livePlans.sortedWith(
            compareByDescending<ReadingPlan> { it.id == activePlanId }
                .thenByDescending { it.startDate }
                .thenByDescending { it.createdAt }
        )
        val live = orderedLive.map {
            summarize(
                plan = it,
                studySessions = studySessions,
                reflectionsByDay = reflectionsFor(it.id),
                bookmarks = bookmarksFor(it.id)
            )
        }
        val archivedSummaries = archived
            .sortedByDescending { it.archivedAt }
            .filter { entry -> livePlans.none { it.id == entry.plan.id } }
            .map {
                summarize(
                    plan = it.plan,
                    archivedAt = it.archivedAt.ifBlank { null },
                    studySessions = studySessions,
                    reflectionsByDay = reflectionsFor(it.plan.id),
                    bookmarks = bookmarksFor(it.plan.id)
                )
            }
        return live + archivedSummaries
    }

    fun filterCompleted(summaries: List<PlanSummary>, completedOnly: Boolean): List<PlanSummary> =
        if (completedOnly) summaries.filter { it.isComplete } else summaries

    fun formatTime(seconds: Long): String {
        if (seconds <= 0L) return "No reading time recorded"
        val minutes = Math.round(seconds / 60.0f).toInt()
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours > 0 && rest > 0 -> "${hours}h ${rest}m recorded"
            hours > 0 -> "${hours}h recorded"
            else -> "${minutes}m recorded"
        }
    }
}
