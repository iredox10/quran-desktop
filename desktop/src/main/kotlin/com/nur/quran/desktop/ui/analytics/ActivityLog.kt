package com.nur.quran.desktop.ui.analytics

import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.shared.AnalyticsStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Search, range filtering and day grouping for the Activity History page.
 * Ported 1:1 from the web app's `src/utils/historyFilter.js`; `now` is
 * injected everywhere so it can be pinned in tests.
 */
object ActivityLog {

    const val MAX_ROWS = 100

    /** Web `HISTORY_TABS` (activity.js). */
    val HISTORY_TABS: List<Pair<String, String>> = listOf(
        "today" to "Today",
        "yesterday" to "Yesterday",
        "week" to "This Week",
        "month" to "This Month",
        "all" to "All Time"
    )

    fun normalizeQuery(query: String?): String = query?.trim()?.lowercase(Locale.US).orEmpty()

    /** Resolve a chapter id to its searchable names. Null ids resolve to none. */
    fun chapterNames(chapterId: Int?): List<String> {
        if (chapterId == null) return emptyList()
        val chapter = QuranStore.chapter(chapterId) ?: return emptyList()
        return listOf(chapter.nameSimple, chapter.nameArabic).filter { it.isNotBlank() }
    }

    /**
     * Case-insensitive match against activity type (raw id + display label),
     * surah name, and "surah {id}". An empty query always matches.
     */
    fun matchesQuery(session: AnalyticsStats.Session, query: String?): Boolean {
        val q = normalizeQuery(query)
        if (q.isEmpty()) return true

        val typeWords = listOfNotNull(
            session.type,
            AnalyticsEngine.TYPE_META[session.type]?.label
        )
        if (typeWords.any { it.lowercase(Locale.US).contains(q) }) return true

        val chapterId = session.chapterId
        if (chapterId != null && chapterId > 0) {
            if (chapterNames(chapterId).any { it.lowercase(Locale.US).contains(q) }) return true
            if ("surah $chapterId".contains(q)) return true
        }
        return false
    }

    /** Range + query filtering; order is untouched. */
    fun filterSessions(
        sessions: List<AnalyticsStats.Session>,
        range: String,
        query: String? = "",
        now: Date = Date()
    ): List<AnalyticsStats.Session> {
        val inRange = AnalyticsEngine.filterByRange(sessions, range, now)
        if (normalizeQuery(query).isEmpty()) return inRange
        return inRange.filter { matchesQuery(it, query) }
    }

    /** Sort timestamp: explicit `timestamp`, else the session's date at UTC midnight. */
    fun sessionTime(session: AnalyticsStats.Session): Long =
        if (session.timestamp > 0L) session.timestamp
        else AnalyticsEngine.keyToDate(session.date).time

    /** "Today" / "Yesterday" / "Mon, Oct 5" heading for a date key. */
    fun dayLabel(key: String, now: Date = Date()): String {
        val today = AnalyticsEngine.dateKey(now)
        if (key == today) return "Today"
        if (key == AnalyticsEngine.addDays(today, -1)) return "Yesterday"
        val fmt = SimpleDateFormat("EEE, MMM d", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(AnalyticsEngine.keyToDate(key))
    }

    data class DayGroup(
        val key: String,
        val label: String,
        val sessions: List<AnalyticsStats.Session>,
        val totalSeconds: Long
    )

    /** Newest day first, newest session first inside each day. */
    fun groupSessionsByDay(
        sessions: List<AnalyticsStats.Session>,
        now: Date = Date()
    ): List<DayGroup> {
        val byDay = LinkedHashMap<String, MutableList<AnalyticsStats.Session>>()
        sessions.forEach { session ->
            if (session.date.isBlank()) return@forEach
            byDay.getOrPut(session.date) { mutableListOf() }.add(session)
        }
        return byDay.map { (key, list) ->
            val ordered = list.sortedByDescending { sessionTime(it) }
            DayGroup(
                key = key,
                label = dayLabel(key, now),
                sessions = ordered,
                totalSeconds = ordered.sumOf { it.durationSec }
            )
        }.sortedByDescending { it.key }
    }

    /** Keep the newest [limit] rows across day groups, mirroring the web cap. */
    fun capRows(groups: List<DayGroup>, limit: Int = MAX_ROWS): List<DayGroup> {
        if (groups.sumOf { it.sessions.size } <= limit) return groups
        var remaining = limit
        val out = mutableListOf<DayGroup>()
        for (group in groups) {
            if (remaining <= 0) break
            val take = minOf(group.sessions.size, remaining)
            remaining -= take
            out.add(
                if (take == group.sessions.size) group
                else group.copy(sessions = group.sessions.take(take))
            )
        }
        return out
    }
}
