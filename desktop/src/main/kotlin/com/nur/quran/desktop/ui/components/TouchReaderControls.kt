package com.nur.quran.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * On-screen touch controls & tablet ergonomics for reader screens.
 *
 * Provides:
 * 1. Edge-docked Floating Action Buttons for thumb-friendly Previous / Next navigation.
 * 2. Quick Font Size Steppers (+ / -) directly reachable without opening Settings.
 * 3. Animated Floating Scroll-To-Top button for long chapters / assignments.
 * 4. Fast tablet mode toggle.
 */

@Composable
fun BoxScope.TouchReaderOverlay(
    pal: NurPalette,
    touchMode: Boolean,
    onToggleTouchMode: () -> Unit,
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    onNext: (() -> Unit)? = null,
    nextLabel: String? = null,
    onPrevious: (() -> Unit)? = null,
    previousLabel: String? = null,
    showScrollToTop: Boolean = false,
    onScrollToTop: (() -> Unit)? = null,
    bottomPadding: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()

    // ── Edge navigators for thumb reach (docked to screen borders) ─────
    if (touchMode) {
        if (onPrevious != null) {
            EdgeNavTab(
                pal = pal,
                fontUi = fontUi,
                label = previousLabel ?: "Prev",
                isLeft = true,
                onClick = onPrevious,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        if (onNext != null) {
            EdgeNavTab(
                pal = pal,
                fontUi = fontUi,
                label = nextLabel ?: "Next",
                isLeft = false,
                onClick = onNext,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }

    // ── Floating Scroll-To-Top button (visible when scrolled down) ─────
    AnimatedVisibility(
        visible = showScrollToTop && onScrollToTop != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = if (touchMode) bottomPadding + 56.dp else bottomPadding + 8.dp)
    ) {
        Surface(
            onClick = { onScrollToTop?.invoke() },
            shape = CircleShape,
            color = pal.teal,
            contentColor = pal.white,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, pal.gold.copy(alpha = 0.4f)),
            modifier = Modifier.size(48.dp).touchTarget()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    tint = pal.white,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // ── Bottom Floating Touch HUD (Font steppers + Tablet Mode toggle) ──
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomPadding)
    ) {
        if (touchMode) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pal.surface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, pal.gold.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
                modifier = Modifier.height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    // Touch Mode Active indicator / toggle button
                    IconButton(
                        onClick = onToggleTouchMode,
                        modifier = Modifier.touchTarget().size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = "Tablet mode active. Tap to toggle.",
                            tint = pal.gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(pal.boneDark)
                    )

                    // Font Stepper: [-]
                    IconButton(
                        onClick = {
                            val next = (currentScale - 0.1f).coerceIn(0.6f, 2.2f)
                            onScaleChange(next)
                        },
                        enabled = currentScale > 0.65f,
                        modifier = Modifier.touchTarget().size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease font size",
                            tint = if (currentScale > 0.65f) pal.ink else pal.inkMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Current Scale indicator (e.g. 100%)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TextFields,
                            contentDescription = null,
                            tint = pal.inkMid,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(currentScale * 100 + 0.5f).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.ink
                        )
                    }

                    // Font Stepper: [+]
                    IconButton(
                        onClick = {
                            val next = (currentScale + 0.1f).coerceIn(0.6f, 2.2f)
                            onScaleChange(next)
                        },
                        enabled = currentScale < 2.15f,
                        modifier = Modifier.touchTarget().size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase font size",
                            tint = if (currentScale < 2.15f) pal.ink else pal.inkMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // When Touch Mode is OFF: show a discreet, unobtrusive pill button
            // so touch/tablet users can quickly activate Touch Mode without digging in settings.
            Surface(
                onClick = onToggleTouchMode,
                shape = RoundedCornerShape(20.dp),
                color = pal.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, pal.boneDark),
                shadowElevation = 3.dp,
                modifier = Modifier.height(34.dp).touchTarget()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = "Touch mode",
                        tint = pal.inkMid,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Touch",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontUi,
                        color = pal.inkMid
                    )
                }
            }
        }
    }
}

/**
 * Thumb-friendly edge navigation tab docked to the screen border.
 *
 * Left tab: docked to left bezel (curved on right).
 * Right tab: docked to right bezel (curved on left).
 */
@Composable
private fun EdgeNavTab(
    pal: NurPalette,
    fontUi: FontFamily,
    label: String,
    isLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = if (isLeft) {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = pal.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, pal.gold.copy(alpha = 0.35f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(64.dp)
            .width(44.dp)
            .touchTarget()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = if (isLeft) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                contentDescription = label,
                tint = pal.inkMid,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (label.length > 5) label.take(4) + "…" else label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontUi,
                color = pal.inkMuted,
                maxLines = 1
            )
        }
    }
}
