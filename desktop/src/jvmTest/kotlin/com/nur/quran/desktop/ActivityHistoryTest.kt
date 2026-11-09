package com.nur.quran.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nur.quran.desktop.ui.analytics.ActivityLog
import com.nur.quran.desktop.ui.screens.ActivityHistoryScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.AnalyticsStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Parity tests for the Activity History filter/group engine (`historyFilter.js`). */
class ActivityHistoryTest {

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    private val now: Date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse("2026-11-09")!!

    private fun session(
        date: String,
        minutes: Int,
        type: String = "reading",
        chapterId: Int? = null,
        timestamp: Long = 0L
    ) = AnalyticsStats.Session(
        date = date,
        durationSec = minutes * 60L,
        type = type,
        chapterId = chapterId,
        timestamp = timestamp
    )

    private val sessions = listOf(
        session("2026-11-09", 25, "reading", 1, 1_800_000_000_000),
        session("2026-11-09", 15, "pomodoro", null, 1_800_000_100_000),
        session("2026-11-08", 30, "memorizing", 2),
        session("2026-10-30", 10, "listening", 18),
        session("2026-09-01", 5, "reading", 114)
    )

    @Test
    fun rangeFilteringNarrowsToPeriod() {
        assertEquals(2, ActivityLog.filterSessions(sessions, "today", "", now).size)
        assertEquals(1, ActivityLog.filterSessions(sessions, "yesterday", "", now).size)
        assertEquals(5, ActivityLog.filterSessions(sessions, "all", "", now).size)
        assertTrue(ActivityLog.filterSessions(sessions, "today", "", now).all { it.date == "2026-11-09" })
    }

    @Test
    fun queryMatchesTypeLabelAndRawType() {
        val focus = ActivityLog.filterSessions(sessions, "all", "focus", now)
        assertEquals(1, focus.size)
        assertEquals("pomodoro", focus.single().type)

        assertEquals(1, ActivityLog.filterSessions(sessions, "all", "listening", now).size)
    }

    @Test
    fun queryMatchesSurahNameAndSurahId() {
        val byName = ActivityLog.filterSessions(sessions, "all", "baqarah", now)
        assertEquals(1, byName.size)
        assertEquals(2, byName.single().chapterId)

        val byId = ActivityLog.filterSessions(sessions, "all", "surah 18", now)
        assertEquals(1, byId.size)
        assertEquals(18, byId.single().chapterId)
    }

    @Test
    fun emptyQueryAndWhitespaceMatchEverything() {
        assertEquals(sessions.size, ActivityLog.filterSessions(sessions, "all", "   ", now).size)
        assertTrue(ActivityLog.matchesQuery(sessions.first(), ""))
        assertFalse(ActivityLog.matchesQuery(sessions.first(), "zzzz"))
    }

    @Test
    fun groupingIsNewestDayFirstAndNewestSessionFirst() {
        val groups = ActivityLog.groupSessionsByDay(sessions, now)
        assertEquals(listOf("2026-11-09", "2026-11-08", "2026-10-30", "2026-09-01"), groups.map { it.key })
        assertEquals("Today", groups.first().label)
        assertEquals("Yesterday", groups[1].label)
        assertEquals(40L * 60L, groups.first().totalSeconds)
        // Newest timestamp first inside the day.
        assertEquals(1_800_000_100_000, groups.first().sessions.first().timestamp)
    }

    @Test
    fun dayLabelFallsBackToWeekdayDate() {
        val label = ActivityLog.dayLabel("2026-10-30", now)
        assertTrue(label.contains("Oct 30"))
    }

    @Test
    fun sessionTimeFallsBackToUtcMidnight() {
        val s = session("2026-11-08", 30)
        assertEquals(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse("2026-11-08")!!.time,
            ActivityLog.sessionTime(s)
        )
    }

    @Test
    fun capRowsKeepsNewestHundred() {
        val many = (1..150).map { session("2026-11-09", 5, timestamp = it.toLong()) }
        val capped = ActivityLog.capRows(ActivityLog.groupSessionsByDay(many, now))
        assertEquals(ActivityLog.MAX_ROWS, capped.sumOf { it.sessions.size })
        // Newest timestamps survive the cap.
        assertEquals(150L, capped.single().sessions.first().timestamp)
        assertEquals(51L, capped.single().sessions.last().timestamp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun screenRendersFiltersAndSwitchesRange() {
        rule.setContent {
            NurTheme { ActivityHistoryScreenDesktop(pal = pal) }
        }
        rule.waitForIdle()

        rule.onNodeWithText("Activity History").assertExists()
        rule.onNodeWithText("Total time").assertExists()
        rule.onNodeWithText("Active days").assertExists()

        rule.onNodeWithText("All Time").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("SESSIONS · All time").assertExists()
    }
}
