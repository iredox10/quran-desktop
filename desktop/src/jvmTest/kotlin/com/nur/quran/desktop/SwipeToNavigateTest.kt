package com.nur.quran.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.nur.quran.desktop.ui.input.swipeToNavigate
import com.nur.quran.desktop.ui.input.touchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SwipeToNavigateTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testSwipeLeftCallsOnNext() {
        var nextCalls = 0
        var prevCalls = 0

        rule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .testTag("swipeBox")
                    .swipeToNavigate(
                        onNext = { nextCalls++ },
                        onPrevious = { prevCalls++ }
                    )
            )
        }

        // Drag to the left: from (300, 200) to (100, 200) -> totalDx = -200
        rule.onNodeWithTag("swipeBox").performMouseInput {
            moveTo(Offset(300f, 200f))
            press()
            moveTo(Offset(100f, 200f))
            release()
        }
        rule.waitForIdle()

        assertEquals("onNext should have been called once", 1, nextCalls)
        assertEquals("onPrevious should not have been called", 0, prevCalls)
    }

    @Test
    fun testSwipeRightCallsOnPrevious() {
        var nextCalls = 0
        var prevCalls = 0

        rule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .testTag("swipeBox")
                    .swipeToNavigate(
                        onNext = { nextCalls++ },
                        onPrevious = { prevCalls++ }
                    )
            )
        }

        // Drag to the right: from (100, 200) to (300, 200) -> totalDx = +200
        rule.onNodeWithTag("swipeBox").performMouseInput {
            moveTo(Offset(100f, 200f))
            press()
            moveTo(Offset(300f, 200f))
            release()
        }
        rule.waitForIdle()

        assertEquals("onNext should not have been called", 0, nextCalls)
        assertEquals("onPrevious should have been called once", 1, prevCalls)
    }

    @Test
    fun testVerticalDragDoesNotTriggerSwipeNavigation() {
        var nextCalls = 0
        var prevCalls = 0

        rule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .testTag("container")
                    .swipeToNavigate(
                        onNext = { nextCalls++ },
                        onPrevious = { prevCalls++ }
                    )
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items((0..50).toList()) { i ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Item $i")
                        }
                    }
                }
            }
        }

        // Drag vertically UP: from (200, 300) to (200, 100) -> dy = -200, dx = 0
        rule.onNodeWithTag("container").performMouseInput {
            moveTo(Offset(200f, 300f))
            press()
            moveTo(Offset(200f, 100f))
            release()
        }
        rule.waitForIdle()

        assertEquals("Vertical drag must not trigger onNext", 0, nextCalls)
        assertEquals("Vertical drag must not trigger onPrevious", 0, prevCalls)
    }

    @Test
    fun testTapPassesThroughToChildClickable() {
        var nextCalls = 0
        var prevCalls = 0
        var itemClicked = false

        rule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .testTag("container")
                    .swipeToNavigate(
                        onNext = { nextCalls++ },
                        onPrevious = { prevCalls++ }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .testTag("childButton")
                        .clickable { itemClicked = true }
                )
            }
        }

        rule.onNodeWithTag("childButton").performClick()
        rule.waitForIdle()

        assertTrue("Child button should receive click", itemClicked)
        assertEquals(0, nextCalls)
        assertEquals(0, prevCalls)
    }

    @Test
    fun testHorizontalDragOverLazyColumnNavigatesSuccessfully() {
        var nextCalls = 0
        var prevCalls = 0

        rule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .testTag("container")
                    .swipeToNavigate(
                        onNext = { nextCalls++ },
                        onPrevious = { prevCalls++ }
                    )
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items((0..50).toList()) { i ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable { /* child clickable */ }
                        ) {
                            Text("Item $i")
                        }
                    }
                }
            }
        }

        // Drag horizontally LEFT across the LazyColumn: (300, 200) to (100, 200)
        rule.onNodeWithTag("container").performMouseInput {
            moveTo(Offset(300f, 200f))
            press()
            moveTo(Offset(100f, 200f))
            release()
        }
        rule.waitForIdle()

        assertEquals("Horizontal swipe over LazyColumn must trigger onNext", 1, nextCalls)
        assertEquals("Horizontal swipe over LazyColumn must not trigger onPrevious", 0, prevCalls)
    }

    @Test
    fun testTouchTargetEnsuresAtLeast48dp() {
        rule.setContent {
            Box(
                modifier = Modifier
                    .touchTarget()
                    .size(24.dp)
                    .testTag("smallTarget")
                    .background(Color.Red)
            )
        }

        rule.onNodeWithTag("smallTarget")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
