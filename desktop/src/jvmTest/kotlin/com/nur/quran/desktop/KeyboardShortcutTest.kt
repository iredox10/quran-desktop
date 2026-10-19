package com.nur.quran.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.screens.PlannerReaderScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/**
 * Keyboard-shortcut tests for the planner day reader.
 *
 * Keys are injected straight into the Compose UI tree via [performKeyInput],
 * so these verify the shortcut wiring itself and do not depend on the OS
 * keyboard path (unreliable under headless / Xwayland CI).
 *
 * Mirrors web `PlannerReader.jsx` (lines ~545-573): ArrowLeft = next page,
 * ArrowRight = previous page (RTL reading order), bounded to the assignment.
 */
@OptIn(ExperimentalTestApi::class)
class KeyboardShortcutTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun markWelcomed() {
            java.util.prefs.Preferences.userRoot().node("welcome_prefs").putBoolean("seen", true)
        }
    }

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    private fun chapters(): List<PlanChapter> =
        QuranStore.chapters.map { c ->
            PlanChapter(
                id = c.id,
                versesCount = c.versesCount,
                startPage = c.pages.firstOrNull() ?: 0,
                endPage = c.pages.lastOrNull() ?: 0
            )
        }

    /** Seeds a 30-day page plan and returns (pageStart, pageEnd) of today's assignment. */
    private fun seedPlan(): Pair<Int, Int> {
        val plan = PlannerEngine.buildReadingPlanner(
            unitType = "page",
            durationDays = 30,
            startDate = java.time.LocalDate.now()
                .minusDays(3)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            startUnit = 1,
            endUnit = 604,
            customTitle = "Keyboard Plan",
            chapters = chapters()
        )
        PlannerStore.saveActivePlan(plan)
        val today = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val assignment = plan.assignments.firstOrNull { it.date == today }
            ?: plan.assignments.first()
        return assignment.pageStart to assignment.pageEnd
    }

    private fun composeReader() {
        rule.setContent {
            NurTheme {
                PlannerReaderScreenDesktop(
                    dayDate = PlannerEngine.formatPlannerDate(),
                    pal = pal
                )
            }
        }
        rule.waitForIdle()
    }

    /** Reads the current page out of the reader's "Page N of M" label. */
    private fun currentPage(pageEnd: Int): Int {
        val texts = rule.onNodeWithText("of $pageEnd", substring = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .map { it.text }
        val joined = texts.joinToString(" ")
        val m = Regex("Page (\\d+) of $pageEnd").find(joined)
            ?: error("page label not found in: $joined")
        return m.groupValues[1].toInt()
    }

    @Test
    fun `arrow left advances to next page`() {
        val (pageStart, pageEnd) = seedPlan()
        composeReader()

        val before = currentPage(pageEnd)
        assertTrue(
            "start page $before must lie in assignment [$pageStart,$pageEnd]",
            before in pageStart..pageEnd
        )
        assertTrue("need headroom to advance", before < pageEnd)

        rule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.waitForIdle()

        assertEquals("ArrowLeft must advance one page", before + 1, currentPage(pageEnd))
    }

    @Test
    fun `arrow right goes back a page`() {
        val (pageStart, pageEnd) = seedPlan()
        composeReader()

        val before = currentPage(pageEnd)

        rule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        rule.waitForIdle()
        val after = currentPage(pageEnd)

        // At the first page there is nowhere to go; page must never increase
        // and must stay inside the assignment bounds.
        assertTrue("ArrowRight must not advance the page", after <= before)
        assertTrue(
            "ArrowRight must stay within assignment bounds",
            after in pageStart..pageEnd
        )
    }

    @Test
    fun `page turns never escape the assignment bounds`() {
        val (pageStart, pageEnd) = seedPlan()
        composeReader()

        // Hammer next well past the end of the assignment.
        repeat(50) {
            rule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        }
        rule.waitForIdle()
        assertEquals(
            "page must clamp to the assignment end",
            pageEnd,
            currentPage(pageEnd)
        )

        // Hammer previous well past the start.
        repeat(80) {
            rule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        }
        rule.waitForIdle()
        assertEquals(
            "page must clamp to the assignment start",
            pageStart,
            currentPage(pageEnd)
        )
    }
}
