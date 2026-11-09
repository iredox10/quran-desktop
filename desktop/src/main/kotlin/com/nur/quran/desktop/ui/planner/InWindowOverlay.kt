package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Clean in-window modal backdrop and positioning container for desktop Compose.
 * Replaces native OS AWT Dialogs to avoid Linux/X11 window manager focus loss,
 * multiple OS windows, and monitor misplacement.
 */
@Composable
fun InWindowOverlay(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Scrim backdrop behind content: clicking the scrim dismisses the overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() }
        )

        // 2. Modal content sits ON TOP of scrim as a sibling, cleanly receiving all pointer events
        Box(modifier = modifier) {
            content()
        }
    }
}
