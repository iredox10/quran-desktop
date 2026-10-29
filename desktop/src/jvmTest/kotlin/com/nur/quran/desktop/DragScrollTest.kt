package com.nur.quran.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.nur.quran.desktop.ui.input.dragToScroll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DragScrollTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testPlainLazyColumnDoesNotScrollOnMouseDragOnDesktop() {
        lateinit var state: androidx.compose.foundation.lazy.LazyListState

        rule.setContent {
            val listState = rememberLazyListState()
            state = listState

            LazyColumn(
                modifier = Modifier.height(300.dp).fillMaxWidth().testTag("plainList"),
                state = listState
            ) {
                items((0..100).toList()) { index ->
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Text("Item $index")
                    }
                }
            }
        }

        // Drag UP by 120 pixels with mouse
        rule.onNodeWithTag("plainList").performMouseInput {
            moveTo(Offset(100f, 200f))
            press()
            moveTo(Offset(100f, 80f))
            release()
        }

        rule.waitForIdle()

        // Plain LazyColumn on desktop remains at 0!
        println("Plain LazyColumn after mouse drag: firstVisibleItemIndex=${state.firstVisibleItemIndex}, offset=${state.firstVisibleItemScrollOffset}")
        assertEquals("Plain LazyColumn does NOT scroll on mouse drag on desktop", 0, state.firstVisibleItemIndex)
        assertEquals("Plain LazyColumn offset is 0", 0, state.firstVisibleItemScrollOffset)
    }

    @Test
    fun testLazyColumnDragToScrollAndClick() {
        var clickedItem = -1
        lateinit var state: androidx.compose.foundation.lazy.LazyListState

        rule.setContent {
            val listState = rememberLazyListState()
            state = listState

            LazyColumn(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
                    .testTag("scrollContainer")
                    .dragToScroll(listState),
                state = listState
            ) {
                items((0..100).toList()) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("item_$index")
                            .clickable { clickedItem = index }
                    ) {
                        Text("Item $index")
                    }
                }
            }
        }

        // 1. Click item 0 without dragging
        rule.onNodeWithTag("item_0").performClick()
        assertEquals("Item 0 should be clicked", 0, clickedItem)

        // 2. Drag UP by 150px
        rule.onNodeWithTag("scrollContainer").performMouseInput {
            moveTo(Offset(100f, 250f))
            press()
            moveTo(Offset(100f, 100f))
            release()
        }

        rule.waitForIdle()

        println("LazyColumn after drag: firstVisibleItemIndex=${state.firstVisibleItemIndex}, offset=${state.firstVisibleItemScrollOffset}")
        assertTrue(
            "LazyColumn should have scrolled forward",
            state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
        )
    }

    @Test
    fun testVerticalScrollColumnDragToScroll() {
        var clickedItem = -1
        lateinit var state: androidx.compose.foundation.ScrollState

        rule.setContent {
            val scrollState = rememberScrollState()
            state = scrollState

            Column(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
                    .testTag("columnContainer")
                    .verticalScroll(scrollState)
                    .dragToScroll(scrollState)
            ) {
                for (index in 0..50) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("col_item_$index")
                            .clickable { clickedItem = index }
                    ) {
                        Text("Col Item $index")
                    }
                }
            }
        }

        assertEquals(0, state.value)

        // 1. Click item 0
        rule.onNodeWithTag("col_item_0").performClick()
        assertEquals("Col Item 0 should be clicked", 0, clickedItem)

        // 2. Drag UP by 120px
        rule.onNodeWithTag("columnContainer").performMouseInput {
            moveTo(Offset(100f, 200f))
            press()
            moveTo(Offset(100f, 80f))
            release()
        }

        rule.waitForIdle()

        println("VerticalScroll Column after drag: scroll value=${state.value}")
        assertTrue("Column should have scrolled forward", state.value > 0)
    }

    @Test
    fun testLazyRowHorizontalDragToScroll() {
        lateinit var state: androidx.compose.foundation.lazy.LazyListState

        rule.setContent {
            val listState = rememberLazyListState()
            state = listState

            LazyRow(
                modifier = Modifier
                    .width(300.dp)
                    .height(100.dp)
                    .testTag("rowContainer")
                    .dragToScroll(listState, orientation = Orientation.Horizontal),
                state = listState
            ) {
                items((0..50).toList()) { index ->
                    Box(modifier = Modifier.width(80.dp).height(100.dp)) {
                        Text("Row $index")
                    }
                }
            }
        }

        assertEquals(0, state.firstVisibleItemIndex)

        // Drag LEFT by 120px (moves content left, scrolls forward)
        rule.onNodeWithTag("rowContainer").performMouseInput {
            moveTo(Offset(200f, 50f))
            press()
            moveTo(Offset(80f, 50f))
            release()
        }

        rule.waitForIdle()

        println("LazyRow after horizontal drag: firstVisibleItemIndex=${state.firstVisibleItemIndex}, offset=${state.firstVisibleItemScrollOffset}")
        assertTrue(
            "LazyRow should have scrolled horizontally",
            state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
        )
    }
}
