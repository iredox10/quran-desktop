package com.nur.quran.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nur.quran.desktop.ui.screens.App

fun main() = application {
    // Large default size AND maximized placement: if the window manager
    // ignores maximization, the explicit size still gives a big window.
    val state = rememberWindowState(
        placement = WindowPlacement.Maximized,
        position = WindowPosition.PlatformDefault,
        size = DpSize(1440.dp, 900.dp)
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Quran Nur",
        state = state
    ) {
        App()
    }
}
