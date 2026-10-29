package com.nur.quran.desktop.ui.input

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Shared touch/input helpers for the desktop app.
 *
 * Compose Desktop routes touch taps through `clickable` and touch drags through
 * scroll containers, but touchscreens lack finger-sized hit targets and
 * reliable swipe navigation that does not conflict with vertical scrolling.
 *
 * This file centralises both:
 * 1. [touchTarget] guarantees the 48dp minimum interactive component size.
 * 2. [swipeToNavigate] provides bidirectional-safe horizontal swipe navigation
 *    (swipe left = next, swipe right = previous) that disambiguates against
 *    child vertical scrollables in [PointerEventPass.Initial] so that:
 *    - Dominantly horizontal drags lock and navigate without child cancellation.
 *    - Dominantly vertical drags yield to child scroll containers (e.g. LazyColumn).
 *    - Taps pass directly through to child clickables without interference.
 */

/** Minimum interactive hit-target, per Material touch-target guidance. */
val TouchTargetMin = 48.dp

/**
 * Guarantees a finger-sized hit target (minimum 48dp) without altering the
 * laid-out visual size when wrapped in a container: [minimumInteractiveComponentSize]
 * pads the touch hit area to 48dp and centres the content.
 *
 * Combine with [defaultMinSize] so interactive wrappers never shrink below 48dp
 * even if local interactive component size is modified.
 */
fun Modifier.touchTarget(minSize: Dp = TouchTargetMin): Modifier =
    this.minimumInteractiveComponentSize().defaultMinSize(minWidth = minSize, minHeight = minSize)

/**
 * Horizontal swipe navigation: swipe left = next, swipe right = previous.
 *
 * Disambiguates horizontal swipes against child vertical scroll containers
 * (such as LazyColumn in Surah/Page readers).
 *
 * In [PointerEventPass.Initial]:
 * - Monitors pointer movement before children consume it.
 * - When horizontal movement dominates vertical movement ([abs(dx) > abs(dy) * 1.25f]
 *   and [abs(dx) > touchSlop]), it locks into a horizontal swipe and consumes
 *   pointer changes so child vertical lists do NOT scroll or cancel the gesture.
 * - If vertical movement dominates ([abs(dy) > touchSlop]), it yields immediately,
 *   allowing child scrollables to handle vertical scrolling freely.
 * - On pointer release, checks both drag distance threshold and fling velocity
 *   ([velocityThreshold]) for quick, natural flicks.
 *
 * @param enabled Whether gesture detection is active.
 * @param threshold Minimum drag distance in [Dp] to trigger navigation. Defaults to 56.dp.
 * @param velocityThreshold Minimum fling velocity to trigger navigation on a quick flick. Defaults to 250.dp.
 * @param onNext Invoked when user swipes to the left (advance).
 * @param onPrevious Invoked when user swipes to the right (go back).
 */
fun Modifier.swipeToNavigate(
    enabled: Boolean = true,
    threshold: Dp = 56.dp,
    velocityThreshold: Dp = 250.dp,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
): Modifier {
    if (!enabled) return this
    return this.pointerInput(threshold, velocityThreshold, onNext, onPrevious) {
        val minDragPx = threshold.toPx()
        val minVelocityPx = velocityThreshold.toPx()
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var totalDx = 0f
            var totalDy = 0f
            val velocityTracker = VelocityTracker()
            velocityTracker.addPointerInputChange(down)
            var pointerId = down.id
            var isHorizontalSwipe: Boolean? = null

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                if (change.changedToUpIgnoreConsumed()) {
                    velocityTracker.addPointerInputChange(change)
                    val velocityX = velocityTracker.calculateVelocity().x
                    if (isHorizontalSwipe == true) {
                        change.consume()
                        val passedDistanceNext = totalDx <= -minDragPx
                        val passedDistancePrev = totalDx >= minDragPx
                        val passedVelocityNext = totalDx < -touchSlop && velocityX <= -minVelocityPx
                        val passedVelocityPrev = totalDx > touchSlop && velocityX >= minVelocityPx

                        if (passedDistanceNext || passedVelocityNext) {
                            onNext()
                        } else if (passedDistancePrev || passedVelocityPrev) {
                            onPrevious()
                        }
                    }
                    break
                }

                if (change.isConsumed && isHorizontalSwipe != true) {
                    // Child consumed the event before horizontal lock was decided. Yield.
                    break
                }

                val dragDelta = change.position - change.previousPosition
                totalDx += dragDelta.x
                totalDy += dragDelta.y
                velocityTracker.addPointerInputChange(change)

                val absDx = abs(totalDx)
                val absDy = abs(totalDy)

                if (isHorizontalSwipe == null) {
                    if (absDx > touchSlop || absDy > touchSlop) {
                        if (absDx > absDy * 1.25f && absDx > touchSlop) {
                            // Dominantly horizontal swipe intent detected.
                            isHorizontalSwipe = true
                        } else if (absDy > touchSlop) {
                            // Dominantly vertical scroll intent detected. Yield to child.
                            isHorizontalSwipe = false
                            break
                        }
                    }
                }

                if (isHorizontalSwipe == true) {
                    change.consume()
                }
            }
        }
    }
}

/**
 * Enables pointer drag-to-scroll on desktop Compose containers ([LazyColumn],
 * [LazyRow], or [Modifier.verticalScroll] / [Modifier.horizontalScroll]).
 *
 * ## Root Cause on Compose Desktop
 *
 * In Compose Multiplatform Desktop (JVM), [ScrollableDefaults] and `scrollable`
 * explicitly filter out mouse pointers (`change.type != PointerType.Mouse`).
 * Because Linux touchscreen drivers, Wayland/Xwayland, and [TouchBridge]
 * synthesise standard AWT mouse events, all finger touches arrive as
 * `PointerType.Mouse`. As a result, standard Compose desktop containers only
 * scroll via mouse wheel (`PointerEventType.Scroll`) or scrollbars, ignoring
 * finger drag gestures completely.
 *
 * Furthermore, Compose's built-in `detectVerticalDragGestures` uses a tiny
 * `mouseSlop` (0.125dp) for mouse pointers. If used directly, any minor
 * finger wobble during a normal tap consumes the pointer change as a drag,
 * cancelling all child `clickable`s and breaking button/verse taps!
 *
 * ## How This Modifier Solves It
 *
 * 1. Uses [awaitVerticalTouchSlopOrCancellation] / [awaitHorizontalTouchSlopOrCancellation],
 *    which explicitly enforce standard touch slop (18dp) regardless of pointer type.
 *    Small taps pass unconsumed to child clickables.
 * 2. Once touch slop is exceeded, it consumes pointer changes, cancels child clicks,
 *    and smoothly dispatches drag deltas to the [ScrollableState].
 * 3. Tracks velocity via [VelocityTracker] and performs a natural fling deceleration
 *    via [FlingBehavior] on pointer release.
 * 4. Cancels any ongoing fling immediately when a new touch down occurs.
 *
 * @param state Any [ScrollableState] (e.g. `LazyListState` or `ScrollState`).
 * @param orientation Scrolling direction ([Orientation.Vertical] or [Orientation.Horizontal]).
 * @param enabled Whether drag-scrolling is active.
 * @param reverseDirection Reverses drag-to-scroll direction.
 * @param flingBehavior Custom fling physics; defaults to [ScrollableDefaults.flingBehavior].
 */
fun Modifier.dragToScroll(
    state: ScrollableState,
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
): Modifier = composed(
    inspectorInfo = {
        name = "dragToScroll"
        properties["state"] = state
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
    }
) {
    if (!enabled) return@composed this

    val fling = flingBehavior ?: ScrollableDefaults.flingBehavior()
    val scope = rememberCoroutineScope()

    pointerInput(state, orientation, reverseDirection) {
        var flingJob: Job? = null

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Stop any ongoing fling when pointer is placed down
            flingJob?.cancel()
            flingJob = null

            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)

            val mult = if (reverseDirection) 1f else -1f

            when (orientation) {
                Orientation.Vertical -> {
                    var overSlopAccum = 0f
                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        change.consume()
                        overSlopAccum = overSlop
                    }

                    if (drag != null) {
                        tracker.addPointerInputChange(drag)
                        state.dispatchRawDelta(overSlopAccum * mult)

                        val success = verticalDrag(drag.id) { change ->
                            tracker.addPointerInputChange(change)
                            val delta = change.positionChange().y
                            change.consume()
                            state.dispatchRawDelta(delta * mult)
                        }

                        if (success) {
                            val velocityY = tracker.calculateVelocity().y
                            val flingVel = velocityY * mult
                            if (abs(flingVel) > 50f) {
                                flingJob = scope.launch {
                                    state.scroll {
                                        with(fling) {
                                            performFling(flingVel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Orientation.Horizontal -> {
                    var overSlopAccum = 0f
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        change.consume()
                        overSlopAccum = overSlop
                    }

                    if (drag != null) {
                        tracker.addPointerInputChange(drag)
                        state.dispatchRawDelta(overSlopAccum * mult)

                        val success = horizontalDrag(drag.id) { change ->
                            tracker.addPointerInputChange(change)
                            val delta = change.positionChange().x
                            change.consume()
                            state.dispatchRawDelta(delta * mult)
                        }

                        if (success) {
                            val velocityX = tracker.calculateVelocity().x
                            val flingVel = velocityX * mult
                            if (abs(flingVel) > 50f) {
                                flingJob = scope.launch {
                                    state.scroll {
                                        with(fling) {
                                            performFling(flingVel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

