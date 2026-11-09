package com.nur.quran.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nur.quran.desktop.data.PomodoroStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.pomodoro.PomodoroController
import com.nur.quran.desktop.ui.pomodoro.PomodoroDock
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The pomodoro timer used to be unreachable: the widget existed but no screen
 * mounted it. These tests cover the shared controller and the floating dock.
 */
class PomodoroDockTest {

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)
    private var phases = mutableListOf<String>()

    @Before
    fun setUp() {
        phases = mutableListOf()
        PomodoroController.beep = {}
        PomodoroController.onPhaseEnd = { phases.add(it) }
        PomodoroController.dockExpanded = false
        PomodoroController.dockVisible = true
        PomodoroController.selectProfile(PomodoroStore.getActiveId())
    }

    @After
    fun tearDown() {
        PomodoroController.beep = {}
        PomodoroController.onPhaseEnd = {}
        PomodoroController.dockExpanded = false
        PomodoroController.dockVisible = true
    }

    @Test
    fun idleTimerDoesNotAdvance() {
        val before = PomodoroController.secondsLeft
        PomodoroController.advance(30)
        assertEquals(before, PomodoroController.secondsLeft)
    }

    @Test
    fun finishingFocusLogsSessionAndSwitchesToBreak() {
        val profile = PomodoroController.activeProfile()
        val pomodoroBefore = PomodoroStore.history().size
        val sessionsBefore = SessionStore.all().size

        PomodoroController.toggleRunning()
        PomodoroController.advance(profile.focusMinutes * 60L)

        assertEquals(listOf("focus"), phases)
        assertEquals("break", PomodoroController.mode)
        assertFalse(PomodoroController.running)
        assertEquals(profile.breakMinutes * 60, PomodoroController.secondsLeft)
        assertEquals(pomodoroBefore + 1, PomodoroStore.history().size)
        assertEquals(sessionsBefore + 1, SessionStore.all().size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dockShowsPillAndExpandsIntoConfigCard() {
        rule.setContent {
            NurTheme { PomodoroDock(pal = pal) }
        }
        rule.waitForIdle()

        // Collapsed: play button plus settings affordance are visible.
        rule.onNodeWithContentDescription("Start pomodoro").assertExists()
        rule.onNodeWithContentDescription("Pomodoro settings").performClick()
        rule.waitForIdle()

        // Expanded: full config card with the profile picker and goal row.
        rule.onNodeWithText("Pomodoro").assertExists()
        rule.onNodeWithText("New profile").assertExists()

        rule.onNodeWithContentDescription("Minimize pomodoro").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Start pomodoro").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dockStopButtonPausesSharedTimer() {
        PomodoroController.toggleRunning()
        assertTrue(PomodoroController.running)

        rule.setContent {
            NurTheme { PomodoroDock(pal = pal) }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Pause pomodoro").performClick()
        rule.waitForIdle()

        assertFalse(PomodoroController.running)
    }
}
