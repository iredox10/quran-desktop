package com.nur.quran.desktop.ui.pomodoro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nur.quran.desktop.data.PomodoroStore
import com.nur.quran.desktop.data.SessionStore
import java.awt.Toolkit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Process-wide pomodoro timer state.
 *
 * The web app keeps the pomodoro clock in its global store, so the floating
 * timer keeps counting while the user navigates. The desktop port used to own
 * the countdown inside the widget composable, which meant the widget could not
 * be minimized or hidden without losing the session. All mutable state lives
 * here instead; [PomodoroDock] renders it and drives [advance].
 */
object PomodoroController {

    var profiles by mutableStateOf(PomodoroStore.profiles())
        private set
    var activeId by mutableStateOf(PomodoroStore.getActiveId())
        private set
    var mode by mutableStateOf("focus")
        private set
    var running by mutableStateOf(false)
        private set
    var secondsLeft by mutableStateOf(0)
        private set
    var goal by mutableStateOf(PomodoroStore.getDailyGoal())
        private set
    var todayCount by mutableStateOf(0)
        private set

    /** Floating dock is shown as a pill unless the config card is open. */
    var dockExpanded by mutableStateOf(false)
    var dockVisible by mutableStateOf(true)

    /** Test hook: notified with the phase that just finished ("focus"/"break"). */
    var onPhaseEnd: (String) -> Unit = {}

    /** Test hook: replaces the AWT beep so tests stay silent. */
    var beep: () -> Unit = {
        runCatching { Toolkit.getDefaultToolkit().beep() }
    }

    private var day: String = LocalDate.now().format(dateFmt)

    init {
        refreshToday()
        secondsLeft = phaseTotalSec()
    }

    fun activeProfile(): PomodoroStore.PomodoroProfile =
        profiles.firstOrNull { it.id == activeId } ?: profiles.first()

    fun phaseTotalSec(): Int {
        val profile = activeProfile()
        return if (mode == "focus") profile.focusMinutes * 60 else profile.breakMinutes * 60
    }

    fun toggleRunning() {
        running = !running
        if (running) {
            dockVisible = true
            refreshToday()
        }
    }

    fun reset() {
        running = false
        secondsLeft = phaseTotalSec()
    }

    fun selectProfile(id: String) {
        if (profiles.none { it.id == id }) return
        PomodoroStore.setActive(id)
        activeId = id
        mode = "focus"
        running = false
        secondsLeft = phaseTotalSec()
    }

    fun createProfile(name: String, focusMinutes: Int, breakMinutes: Int) {
        val focus = focusMinutes.coerceIn(1, 180)
        val rest = breakMinutes.coerceIn(1, 60)
        val profile = PomodoroStore.PomodoroProfile(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Custom $focus/$rest" },
            focusMinutes = focus,
            breakMinutes = rest
        )
        PomodoroStore.saveProfile(profile)
        PomodoroStore.setActive(profile.id)
        profiles = PomodoroStore.profiles()
        activeId = profile.id
        mode = "focus"
        running = false
        secondsLeft = phaseTotalSec()
    }

    fun deleteActiveProfile() {
        if (profiles.size <= 1) return
        PomodoroStore.deleteProfile(activeId)
        profiles = PomodoroStore.profiles()
        activeId = PomodoroStore.getActiveId()
        mode = "focus"
        running = false
        secondsLeft = phaseTotalSec()
    }

    fun updateGoal(value: Int) {
        val next = value.coerceIn(1, 32)
        goal = next
        PomodoroStore.putDailyGoal(next)
    }

    fun refreshToday() {
        val today = LocalDate.now().format(dateFmt)
        day = today
        todayCount = PomodoroStore.todayCount(today)
    }

    /**
     * Advances the countdown by [seconds]. A phase that reaches zero is
     * logged, mirrored into the analytics session log, and switches the timer
     * to the other phase (paused), matching the previous widget behaviour.
     */
    fun advance(seconds: Long = 1) {
        if (!running || seconds <= 0) return
        val today = LocalDate.now().format(dateFmt)
        if (today != day) {
            refreshToday()
        }
        var remaining = secondsLeft.toLong() - seconds
        while (remaining <= 0L) {
            completePhase()
            if (!running) return
            remaining += secondsLeft.toLong()
        }
        secondsLeft = remaining.toInt()
    }

    private fun completePhase() {
        val finished = mode
        val profile = activeProfile()
        beep()
        if (finished == "focus") {
            PomodoroStore.logSession(day, profile.focusMinutes * 60L, "focus")
            SessionStore.log("focus", null, profile.focusMinutes)
        } else {
            PomodoroStore.logSession(day, profile.breakMinutes * 60L, "break")
        }
        refreshToday()
        mode = if (finished == "focus") "break" else "focus"
        secondsLeft = phaseTotalSec()
        running = false
        onPhaseEnd(finished)
    }
}
