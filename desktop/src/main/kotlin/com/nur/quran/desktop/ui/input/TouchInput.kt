package com.nur.quran.desktop.ui.input

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Shared touch/input helpers for the desktop app.
 *
 * Compose Desktop already routes touch taps through `clickable`, but raw
 * swipe navigation (web `react-swipeable`) and finger-sized hit targets need
 * explicit handling on touchscreens. This file centralises both so every
 * reader/screen behaves the same.
 */

/** Minimum interactive hit-target, per Material touch-target guidance. */
val TouchTargetMin = 48.dp

/**
 * Enforces a minimum 48dp touch target while keeping the visual size intact.
 * Use on icon buttons / checkboxes / small pills.
 */
fun Modifier.minTouchTarget(): Modifier = this.sizeIn(minWidth = TouchTargetMin, minHeight = TouchTargetMin)

/**
 * Horizontal swipe navigation: swipe left = next, swipe right = previous.
 *
 * Mirrors web `useSwipeable(onSwipedLeft/onSwipedRight)` in
 * `PlannerReader.jsx` (lines ~545-573). Threshold of 64px avoids accidental
 * page turns from vertical scrolling.
 */
fun Modifier.swipeToNavigate(
    enabled: Boolean = true,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
): Modifier {
    if (!enabled) return this
    return this.pointerInput(onNext, onPrevious) {
        var totalDx = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDx = 0f },
            onHorizontalDrag = { _, delta -> totalDx += delta },
            onDragEnd = {
                when {
                    totalDx <= -64f -> onNext()
                    totalDx >= 64f -> onPrevious()
                }
            },
        )
    }
}
