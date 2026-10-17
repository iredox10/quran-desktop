package com.nur.quran.desktop.ui.input

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Shared touch/input helpers for the desktop app.
 *
 * Compose Desktop already routes touch taps through `clickable` and touch
 * drags through scroll containers, so taps and scrolling work everywhere by
 * default. What touchscreens actually lack on a mouse-first UI is finger-sized
 * hit targets and swipe navigation (web `react-swipeable`). This file
 * centralises both so every reader/screen behaves the same.
 */

/** Minimum interactive hit-target, per Material touch-target guidance. */
val TouchTargetMin = 48.dp

/**
 * Guarantees a finger-sized hit target without changing the laid-out visual
 * size: [minimumInteractiveComponentSize] pads the *touch* area to 48dp and
 * centres the content, so a 16dp glyph still looks 16dp but is tappable with
 * a fingertip. Apply it to the interactive wrapper (IconButton / clickable Box),
 * never to the inner Icon.
 */
fun Modifier.touchTarget(): Modifier = this.minimumInteractiveComponentSize()

/**
 * Horizontal swipe navigation: swipe left = next, swipe right = previous.
 *
 * Mirrors web `useSwipeable(onSwippedLeft/onSwipedRight)` in
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
