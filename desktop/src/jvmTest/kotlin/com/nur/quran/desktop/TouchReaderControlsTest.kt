package com.nur.quran.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nur.quran.desktop.ui.components.TouchReaderOverlay
import com.nur.quran.desktop.ui.theme.NurPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TouchReaderControlsTest {

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    @Test
    fun testPrefsCacheTouchMode() {
        val original = PrefsCache.getTouchMode()
        try {
            PrefsCache.putTouchMode(true)
            assertTrue(PrefsCache.getTouchMode())
            PrefsCache.putTouchMode(false)
            assertFalse(PrefsCache.getTouchMode())
        } finally {
            PrefsCache.putTouchMode(original)
        }
    }

    @Test
    fun testTouchReaderOverlayInTouchMode() {
        var prevClicked = false
        var nextClicked = false
        var touchModeToggled = false
        var currentScale by androidx.compose.runtime.mutableStateOf(1.0f)
        var scrollToTopClicked = false

        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                TouchReaderOverlay(
                    pal = pal,
                    touchMode = true,
                    onToggleTouchMode = { touchModeToggled = true },
                    currentScale = currentScale,
                    onScaleChange = { currentScale = it },
                    onPrevious = { prevClicked = true },
                    previousLabel = "Surah 1",
                    onNext = { nextClicked = true },
                    nextLabel = "Surah 3",
                    showScrollToTop = true,
                    onScrollToTop = { scrollToTopClicked = true }
                )
            }
        }

        // Verify edge navigation tabs
        rule.onNodeWithContentDescription("Surah 1").performClick()
        assertTrue("Previous edge tab should trigger callback", prevClicked)

        rule.onNodeWithContentDescription("Surah 3").performClick()
        assertTrue("Next edge tab should trigger callback", nextClicked)

        // Verify font size steppers
        rule.onNodeWithText("100%").assertExists()
        rule.onNodeWithContentDescription("Increase font size").performClick()
        rule.waitForIdle()
        assertEquals(1.1f, currentScale, 0.01f)
        rule.onNodeWithText("110%").assertExists()

        rule.onNodeWithContentDescription("Decrease font size").performClick()
        rule.waitForIdle()
        assertEquals(1.0f, currentScale, 0.01f)
        rule.onNodeWithText("100%").assertExists()

        // Verify scroll to top FAB
        rule.onNodeWithContentDescription("Scroll to top").performClick()
        assertTrue("Scroll to top FAB should trigger callback", scrollToTopClicked)

        // Verify tablet mode toggle in HUD
        rule.onNodeWithContentDescription("Tablet mode active. Tap to toggle.").performClick()
        assertTrue("Touch mode toggle should trigger callback", touchModeToggled)
    }

    @Test
    fun testTouchReaderOverlayCollapsedWhenTouchModeOff() {
        var touchModeToggled = false

        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                TouchReaderOverlay(
                    pal = pal,
                    touchMode = false,
                    onToggleTouchMode = { touchModeToggled = true },
                    currentScale = 1.0f,
                    onScaleChange = {},
                    showScrollToTop = false
                )
            }
        }

        // Discreet Touch pill is shown
        rule.onNodeWithText("Touch").performClick()
        assertTrue("Tapping touch pill should toggle touch mode", touchModeToggled)
    }
}
