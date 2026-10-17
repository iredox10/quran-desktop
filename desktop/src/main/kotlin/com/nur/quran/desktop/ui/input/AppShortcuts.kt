package com.nur.quran.desktop.ui.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * App-wide keyboard map, mirroring the web planner reader shortcuts
 * (`PlannerReader.jsx` lines ~545-573: ArrowLeft/ArrowRight page turns) plus
 * conveniences desktop readers already use (Home/End jump, Esc back).
 *
 * Centralised so Planner/Surah/Page/Hifdh readers stay consistent.
 * Returns true when the event was consumed.
 */
object AppShortcuts {
    /**
     * Page-turn pair used by readers: ArrowLeft = next, ArrowRight = previous
     * (RTL reading order, matches web + Surah/Page readers).
     */
    fun pageTurn(event: KeyEvent, onNext: () -> Boolean, onPrevious: () -> Boolean): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        return when (event.key) {
            Key.DirectionLeft -> onNext()
            Key.DirectionRight -> onPrevious()
            Key.PageUp -> onPrevious()
            Key.PageDown -> onNext()
            Key.Spacebar -> onNext()
            else -> false
        }
    }

    /**
     * Global shell shortcuts handled at the [androidx.compose.ui.window.Window]
     * level or the top-level [com.nur.quran.desktop.ui.screens.App] box.
     *
     * - Ctrl/Cmd+1..8: jump to sidebar sections (Home, Memorize, Planner,
     *   Sauka, Analytics, Library, Downloads, History)
     * - Alt+Left / Esc: back
     * - F11: fullscreen toggle (handled in `Main.kt`)
     */
    /**
     * Section jumps only (Ctrl/Cmd+1..8). Safe to run on the unfocused shell
     * via bubbled `onKeyEvent` — never fires for arrows/Esc/Space, so it can
     * never steal reader keys.
     */
    fun sectionJump(
        event: KeyEvent,
        topLevelRoute: (index: Int) -> Unit,
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val ctrl = event.isCtrlPressed || event.isMetaPressed
        if (!ctrl) return false
        val index = when (event.key) {
            Key.One -> 0
            Key.Two -> 1
            Key.Three -> 2
            Key.Four -> 3
            Key.Five -> 4
            Key.Six -> 5
            Key.Seven -> 6
            Key.Eight -> 7
            else -> -1
        }
        if (index >= 0) {
            topLevelRoute(index)
            return true
        }
        return false
    }

    fun shellShortcut(
        event: KeyEvent,
        topLevelRoute: (index: Int) -> Unit,
        onBack: () -> Boolean,
    ): Boolean {
        if (sectionJump(event, topLevelRoute)) return true
        if (event.type != KeyEventType.KeyDown) return false
        return when {
            event.key == Key.Escape -> onBack()
            event.isAltPressed && event.key == Key.DirectionLeft -> onBack()
            else -> false
        }
    }

    /** Human-readable cheat-sheet shown in the shortcuts dialog. */
    val cheatSheet: List<Pair<String, String>> = listOf(
        "← / →" to "Next / previous page or surah (RTL reading order)",
        "Space / PgDn / PgUp" to "Next / previous page in readers",
        "Home / End" to "Jump to top / bottom of a list",
        "Esc" to "Back (close dialog first when open)",
        "Alt + ←" to "Back from anywhere",
        "Ctrl/⌘ + 1…8" to "Jump to Home, Memorize, Planner, Sauka, Analytics, Library, Downloads, History",
        "Swipe ← / →" to "Next / previous page on touchscreens",
        "F11" to "Toggle fullscreen",
    )
}

/** Convenience: ctrl-or-cmd pressed (Linux/Windows Ctrl, macOS Cmd). */
private val KeyEvent.ctrlOrCmd: Boolean get() = isCtrlPressed || isMetaPressed
