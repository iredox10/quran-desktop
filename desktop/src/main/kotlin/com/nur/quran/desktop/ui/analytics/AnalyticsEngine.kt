package com.nur.quran.desktop.ui.analytics

import com.nur.quran.shared.AnalyticsStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure date, aggregation, and achievement evaluation engine ported 1:1
 * from the web app's `src/utils/activity.js`, `src/utils/activityRecord.js`,
 * and `src/utils/achievements.js`.
 */
object AnalyticsEngine {

    val ACTIVITY_TYPES = listOf("reading", "memorizing", "listening", "pomodoro")

    data class TypeMeta(val label: String, val colorHex: Long)
    val TYPE_META = mapOf(
        "reading" to TypeMeta("Reading", 0xFF10B981),
        "memorizing" to TypeMeta("Memorizing", 0xFF3B82F6),
        "listening" to TypeMeta("Listening", 0xFFF59E0B),
        "pomodoro" to TypeMeta("Focus", 0xFF8B5CF6),
        "focus" to TypeMeta("Focus", 0xFF8B5CF6)
    )

    private fun utcCalendar(timeMs: Long = System.currentTimeMillis()): Calendar {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.timeInMillis = timeMs
        return cal
    }

    private val UTC_DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun dateKey(date: Date = Date()): String = synchronized(UTC_DATE_FMT) {
        UTC_DATE_FMT.format(date)
    }

    fun keyToDate(key: String): Date = synchronized(UTC_DATE_FMT) {
        runCatching { UTC_DATE_FMT.parse(key) }.getOrNull() ?: Date()
    }

    fun addDays(key: String, n: Int): String {
        val cal = utcCalendar(keyToDate(key).time)
        cal.add(Calendar.DATE, n)
        return dateKey(cal.time)
    }

    fun lastNDayKeys(n: Int, endKey: String = dateKey()): List<String> {
        val keys = mutableListOf<String>()
        for (i in (n - 1) downTo 0) {
            keys.add(addDays(endKey, -i))
        }
        return keys
    }

    fun weekdayIndex(key: String): Int {
        val cal = utcCalendar(keyToDate(key).time)
        return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0 = Sun .. 6 = Sat
    }

    fun weekKeys(now: Date = Date(), weekStart: Int = 0): List<String> {
        val today = dateKey(now)
        val diff = (weekdayIndex(today) - weekStart + 7) % 7
        val start = addDays(today, -diff)
        return (0..6).map { addDays(start, it) }
    }

    fun monthKeys(now: Date = Date()): List<String> {
        val cal = utcCalendar(now.time)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        cal.set(Calendar.DATE, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..maxDays).map { d ->
            val dayCal = utcCalendar()
            dayCal.set(year, month, d, 0, 0, 0)
            dateKey(dayCal.time)
        }
    }

    fun rangeKeys(range: String, now: Date = Date()): List<String>? {
        val today = dateKey(now)
        return when (range) {
            "today" -> listOf(today)
            "yesterday" -> listOf(addDays(today, -1))
            "week" -> weekKeys(now)
            "month" -> monthKeys(now)
            "last7" -> lastNDayKeys(7, today)
            "last30" -> lastNDayKeys(30, today)
            "all" -> null
            else -> null
        }
    }

    fun previousRangeKeys(range: String, now: Date = Date()): List<String>? {
        val keys = rangeKeys(range, now) ?: return null
        if (keys.isEmpty()) return null
        val first = keys.first()
        val span = keys.size
        return (0 until span).map { addDays(first, -span + it) }
    }

    fun filterByKeys(sessions: List<AnalyticsStats.Session>, keys: List<String>?): List<AnalyticsStats.Session> {
        if (keys == null) return sessions
        val set = keys.toSet()
        return sessions.filter { it.date in set }
    }

    fun filterByRange(sessions: List<AnalyticsStats.Session>, range: String, now: Date = Date()): List<AnalyticsStats.Session> {
        val keys = rangeKeys(range, now) ?: return sessions
        val set = keys.toSet()
        return sessions.filter { it.date in set }
    }

    data class RangeSummary(
        val seconds: Long,
        val minutes: Int,
        val count: Int,
        val activeDays: Int,
        val avgPerActiveDay: Int,
        val byType: Map<String, Long>
    )

    fun summarize(sessions: List<AnalyticsStats.Session>): RangeSummary {
        val byType = mutableMapOf<String, Long>(
            "reading" to 0L,
            "memorizing" to 0L,
            "listening" to 0L,
            "pomodoro" to 0L
        )
        var totalSec = 0L
        val activeDaysSet = mutableSetOf<String>()

        sessions.forEach { s ->
            val sec = s.durationSec.coerceAtLeast(0L)
            totalSec += sec
            val mappedType = if (s.type == "focus") "pomodoro" else s.type
            if (byType.containsKey(mappedType)) {
                byType[mappedType] = (byType[mappedType] ?: 0L) + sec
            } else {
                byType["reading"] = (byType["reading"] ?: 0L) + sec
            }
            if (sec > 0L && s.date.isNotBlank()) {
                activeDaysSet.add(s.date)
            }
        }

        val mins = Math.round(totalSec / 60.0f)
        val activeDays = activeDaysSet.size
        val avgPerDay = if (activeDays > 0) Math.round(totalSec.toFloat() / activeDays.toFloat() / 60.0f) else 0

        return RangeSummary(
            seconds = totalSec,
            minutes = mins,
            count = sessions.size,
            activeDays = activeDays,
            avgPerActiveDay = avgPerDay,
            byType = byType
        )
    }

    fun deltaPercent(currentSec: Long, prevSec: Long): Int? {
        if (prevSec <= 0L) return if (currentSec > 0L) null else 0
        return Math.round(((currentSec - prevSec).toFloat() / prevSec.toFloat()) * 100f)
    }

    fun formatDuration(seconds: Long): String {
        val mins = Math.round(seconds / 60.0f)
        if (mins < 60) return "${mins}m"
        val hrs = mins / 60
        val rem = mins % 60
        return if (rem > 0) "${hrs}h ${rem}m" else "${hrs}h"
    }

    private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun rangeTitle(range: String, now: Date = Date()): String {
        val keys = rangeKeys(range, now) ?: return "All time"
        val fmt = SimpleDateFormat("MMM d", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        if (keys.size == 1) return fmt.format(keyToDate(keys[0]))
        val firstDate = keyToDate(keys.first())
        val lastDate = keyToDate(keys.last())
        val cal1 = utcCalendar(firstDate.time)
        val cal2 = utcCalendar(lastDate.time)
        return if (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)) {
            val dFmt = SimpleDateFormat("d", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            "${fmt.format(firstDate)} – ${dFmt.format(lastDate)}"
        } else {
            "${fmt.format(firstDate)} – ${fmt.format(lastDate)}"
        }
    }

    data class DayBucket(
        val key: String,
        val date: String,
        val name: String,
        val label: String,
        val seconds: Long,
        val minutes: Int,
        val count: Int
    )

    fun bucketByDay(sessions: List<AnalyticsStats.Session>, keys: List<String>): List<DayBucket> {
        val byKey = keys.associateWith { mutableListOf<AnalyticsStats.Session>() }
        sessions.forEach { s ->
            byKey[s.date]?.add(s)
        }
        return keys.map { key ->
            val list = byKey[key] ?: emptyList()
            val sec = list.sumOf { it.durationSec }
            val dayIdx = (weekdayIndex(key) + 7) % 7
            val label = DAY_LABELS[dayIdx]
            DayBucket(
                key = key,
                date = key,
                name = label,
                label = label,
                seconds = sec,
                minutes = Math.round(sec / 60.0f),
                count = list.size
            )
        }
    }

    data class HourBucket(
        val hour: Int,
        val name: String,
        val seconds: Long,
        val minutes: Int,
        val count: Int
    )

    fun bucketByHour(sessions: List<AnalyticsStats.Session>, dayKey: String = dateKey()): List<HourBucket> {
        val daySessions = sessions.filter { it.date == dayKey }
        val hoursSec = LongArray(24)
        val hoursCount = IntArray(24)

        daySessions.forEach { s ->
            val ts = if (s.timestamp > 0L) s.timestamp else keyToDate(s.date).time
            val cal = Calendar.getInstance() // local timezone for hours
            cal.timeInMillis = ts
            val h = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hoursSec[h] += s.durationSec
            hoursCount[h] += 1
        }

        return (0..23).map { hour ->
            val name = when {
                hour == 0 -> "12a"
                hour < 12 -> "${hour}a"
                hour == 12 -> "12p"
                else -> "${hour - 12}p"
            }
            HourBucket(
                hour = hour,
                name = name,
                seconds = hoursSec[hour],
                minutes = Math.round(hoursSec[hour] / 60.0f),
                count = hoursCount[hour]
            )
        }
    }

    fun getHeatmapLevel(active: Boolean, durationSec: Long): Int {
        if (!active || durationSec <= 0L) return 0
        val mins = Math.round(durationSec / 60.0f)
        return when {
            mins < 10 -> 1
            mins < 30 -> 2
            else -> 3
        }
    }

    // --- ACHIEVEMENTS ENGINE ---

    data class CatalogBadge(
        val id: String,
        val icon: String,
        val title: String,
        val desc: String,
        val metric: String,
        val target: Float
    )

    val ACHIEVEMENT_CATALOG = listOf(
        CatalogBadge("streak-3", "🔥", "Kindled", "Active 3 days in a row.", "currentStreak", 3f),
        CatalogBadge("streak-7", "🔥", "Steady Flame", "A full week without a gap.", "currentStreak", 7f),
        CatalogBadge("streak-14", "🔥", "Two Weeks of Light", "14 days unbroken.", "currentStreak", 14f),
        CatalogBadge("streak-30", "🔥", "A Month Steadfast", "30 consecutive days in the Quran.", "currentStreak", 30f),
        CatalogBadge("streak-100", "🔥", "Hundred Days of Nur", "100 days without slipping.", "currentStreak", 100f),

        CatalogBadge("minutes-100", "⏱️", "First 100 Minutes", "100 minutes of quiet effort.", "totalMinutes", 100f),
        CatalogBadge("minutes-500", "⏱️", "500 Minutes", "A real habit taking root.", "totalMinutes", 500f),
        CatalogBadge("minutes-1000", "⏱️", "1,000 Minutes", "Over 16 hours with the Book.", "totalMinutes", 1000f),
        CatalogBadge("minutes-5000", "⏱️", "5,000 Minutes", "A deep well of time invested.", "totalMinutes", 5000f),

        CatalogBadge("surahs-5", "🗺️", "Curious Traveler", "Opened 5 different surahs.", "uniqueSurahs", 5f),
        CatalogBadge("surahs-30", "🗺️", "Many Paths", "Explored 30 different surahs.", "uniqueSurahs", 30f),
        CatalogBadge("khatm", "👑", "Khatm", "All 114 surahs — a complete journey.", "uniqueSurahs", 114f),

        CatalogBadge("memorize-500", "📖", "Carried in the Heart", "500 minutes of memorization.", "memorizingMinutes", 500f),
        CatalogBadge("listening-100", "🎧", "Listening Ears", "100 minutes of recitation listened to.", "listeningMinutes", 100f),
        CatalogBadge("focus-10", "🎯", "Deep Focus", "10 focus sessions completed.", "focusSessions", 10f),
        CatalogBadge("focus-50", "🎯", "Unshaken Attention", "50 focus sessions completed.", "focusSessions", 50f),

        CatalogBadge("active-7", "📅", "Seven Days Present", "Active on 7 different days.", "activeDays", 7f),
        CatalogBadge("active-30", "📅", "Regular Rhythm", "Active on 30 different days.", "activeDays", 30f),
        CatalogBadge("active-100", "📅", "Centurion of Days", "Active on 100 different days.", "activeDays", 100f),
        CatalogBadge("perfect-week", "🌟", "Perfect Week", "Active every day for the last 7 days.", "activeDaysLast7", 7f)
    )

    data class AchievementStats(
        val totalSeconds: Long,
        val totalMinutes: Float,
        val sessionCount: Int,
        val activeDays: Float,
        val activeDaysLast7: Float,
        val currentStreak: Float,
        val readingMinutes: Float,
        val memorizingMinutes: Float,
        val listeningMinutes: Float,
        val focusMinutes: Float,
        val focusSessions: Float,
        val uniqueSurahs: Float,
        val todaySessions: Int
    )

    fun computeAchievementStats(
        sessions: List<AnalyticsStats.Session>,
        recentlyReadIds: Collection<Int>,
        now: Date = Date()
    ): AchievementStats {
        val activeDates = mutableSetOf<String>()
        val surahIds = mutableSetOf<Int>()
        var totalSec = 0L
        var readingSec = 0L
        var memorizingSec = 0L
        var listeningSec = 0L
        var focusSec = 0L
        var focusSessions = 0
        var todaySessions = 0
        val todayKey = dateKey(now)

        sessions.forEach { s ->
            val sec = s.durationSec.coerceAtLeast(0L)
            totalSec += sec
            when (s.type) {
                "reading" -> readingSec += sec
                "memorizing" -> memorizingSec += sec
                "listening" -> listeningSec += sec
                "pomodoro", "focus" -> {
                    focusSec += sec
                    focusSessions += 1
                }
                else -> readingSec += sec
            }
            if (s.date.isNotBlank()) {
                activeDates.add(s.date)
                if (s.date == todayKey) todaySessions += 1
            }
            s.chapterId?.let { if (it in 1..114) surahIds.add(it) }
        }

        recentlyReadIds.forEach { if (it in 1..114) surahIds.add(it) }

        val last7 = lastNDayKeys(7, todayKey)
        val activeDaysLast7 = last7.count { it in activeDates }

        val streak = AnalyticsStats.streak(activeDates, now)

        fun toMins(s: Long) = Math.round(s / 60.0f).toFloat()

        return AchievementStats(
            totalSeconds = totalSec,
            totalMinutes = toMins(totalSec),
            sessionCount = sessions.size,
            activeDays = activeDates.size.toFloat(),
            activeDaysLast7 = activeDaysLast7.toFloat(),
            currentStreak = streak.toFloat(),
            readingMinutes = toMins(readingSec),
            memorizingMinutes = toMins(memorizingSec),
            listeningMinutes = toMins(listeningSec),
            focusMinutes = toMins(focusSec),
            focusSessions = focusSessions.toFloat(),
            uniqueSurahs = surahIds.size.toFloat(),
            todaySessions = todaySessions
        )
    }

    data class EvaluatedBadge(
        val id: String,
        val icon: String,
        val title: String,
        val desc: String,
        val unlocked: Boolean,
        val current: Float,
        val target: Float,
        val progress: Float
    )

    fun evaluateAchievements(stats: AchievementStats): List<EvaluatedBadge> {
        return ACHIEVEMENT_CATALOG.map { b ->
            val current = when (b.metric) {
                "currentStreak" -> stats.currentStreak
                "totalMinutes" -> stats.totalMinutes
                "uniqueSurahs" -> stats.uniqueSurahs
                "memorizingMinutes" -> stats.memorizingMinutes
                "listeningMinutes" -> stats.listeningMinutes
                "focusSessions" -> stats.focusSessions
                "activeDays" -> stats.activeDays
                "activeDaysLast7" -> stats.activeDaysLast7
                else -> 0f
            }
            val target = b.target
            val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
            EvaluatedBadge(
                id = b.id,
                icon = b.icon,
                title = b.title,
                desc = b.desc,
                unlocked = current >= target,
                current = current,
                target = target,
                progress = progress
            )
        }
    }

    fun orderBadges(badges: List<EvaluatedBadge>): List<EvaluatedBadge> {
        val unlocked = badges.filter { it.unlocked }.reversed()
        val locked = badges.filter { !it.unlocked }
            .sortedWith(compareByDescending<EvaluatedBadge> { it.progress }.thenBy { it.target })
        return unlocked + locked
    }
}
