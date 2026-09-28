package com.nur.quran.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nur.quran.desktop.ui.screens.App

fun main() = application {
    // True fullscreen: some window managers ignore maximized requests and
    // even explicit sizes, but honor fullscreen placement. F11 toggles,
    // Esc drops back to a large floating window.
    val state = rememberWindowState(
        placement = WindowPlacement.Fullscreen,
        position = WindowPosition.PlatformDefault,
        size = DpSize(1440.dp, 900.dp)
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Quran Nur",
        state = state,
        onKeyEvent = {
            if (it.type != KeyEventType.KeyDown) return@Window false
            when {
                it.key == Key.F11 -> {
                    state.placement =
                        if (state.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating
                        else WindowPlacement.Fullscreen
                    true
                }
                it.key == Key.Escape && state.placement == WindowPlacement.Fullscreen -> {
                    state.placement = WindowPlacement.Floating
                    true
                }
                else -> false
            }
        }
    ) {
        App()
    }
}
