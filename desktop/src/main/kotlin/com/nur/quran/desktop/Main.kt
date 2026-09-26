package com.nur.quran.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.nur.quran.desktop.ui.screens.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Quran Nur") {
        App()
    }
}
