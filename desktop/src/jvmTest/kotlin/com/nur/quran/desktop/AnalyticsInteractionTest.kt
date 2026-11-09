package com.nur.quran.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.screens.AnalyticsScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLogActivityFormAndSessionStoreUpdate() {
        val initialSessionsCount = SessionStore.all().size

        rule.setContent {
            NurTheme {
                AnalyticsScreenDesktop(pal = pal)
            }
        }
        rule.waitForIdle()

        // 1. Click "+ Log activity" button
        rule.onNodeWithText("Log activity").performClick()
        rule.waitForIdle()

        // 2. Select quick chip for 15 minutes
        rule.onNodeWithText("15").performClick()
        rule.waitForIdle()

        // 3. Click "Log session" button
        rule.onNodeWithText("Log session").performClick()
        rule.waitForIdle()

        // 4. Assert SessionStore has the new session
        val updatedSessions = SessionStore.all()
        assertEquals(initialSessionsCount + 1, updatedSessions.size)
        val latest = updatedSessions.last()
        assertEquals(15 * 60L, latest.durationSec)
        assertEquals("reading", latest.type)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testToggleViewToHeatmapAndTabs() {
        rule.setContent {
            NurTheme {
                AnalyticsScreenDesktop(pal = pal)
            }
        }
        rule.waitForIdle()

        // Switch to Heatmap view
        rule.onNodeWithText("Heatmap").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("LESS").assertExists()
        rule.onNodeWithText("MORE").assertExists()

        // Switch back to Chart view
        rule.onNodeWithText("Chart").performClick()
        rule.waitForIdle()

        // Switch PeriodTabs to Month
        rule.onAllNodesWithText("Month")[0].performClick()
        rule.waitForIdle()

        // Achievements "Show all" toggle
        rule.onNodeWithText("Show all").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Show less").assertExists()
    }
}
