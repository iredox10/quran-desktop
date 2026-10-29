package com.nur.quran.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.home.WeeklyHeatmapCard
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class WeeklyHeatmapTest {

    private val pal = NurPalette(false)

    @Test
    fun testWeeklyHeatmapCalculation() {
        val today = LocalDate.now()
        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Log sessions for testing
        SessionStore.log("reading", 1, 30) // 30 min today
        val sessions = SessionStore.all()

        val results = mutableListOf<Triple<String, Long, Boolean>>()
        for (i in 6 downTo 0) {
            val d = today.minusDays(i.toLong())
            val ds = d.format(dateFmt)
            val dayMins = sessions.filter { it.date == ds }.sumOf { it.durationSec } / 60
            val label = dayLabels[d.dayOfWeek.value % 7]
            val isToday = (i == 0)
            results.add(Triple(label, dayMins, isToday))
        }

        assertEquals(7, results.size)
        assertTrue("Last element should be isToday=true", results.last().third)
        assertTrue("Today's minutes should be at least 30", results.last().second >= 30)

        val weekMax = maxOf(results.maxOfOrNull { it.second } ?: 1L, 1L)
        assertTrue(weekMax >= 30)

        val todayPct = ((results.last().second * 100) / weekMax).toInt()
        assertEquals(100, todayPct)
    }

    @Test
    fun testWeeklyHeatmapComposableRenders() = runDesktopComposeUiTest(width = 800, height = 400) {
        setContent {
            NurTheme {
                WeeklyHeatmapCard(pal = pal)
            }
        }

        waitForIdle()

        // Verify "This Week" header exists
        onNodeWithText("This Week").assertExists()
    }
}
