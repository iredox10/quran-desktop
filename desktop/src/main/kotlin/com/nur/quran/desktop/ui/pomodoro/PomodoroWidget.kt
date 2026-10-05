package com.nur.quran.desktop.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.PomodoroStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import java.awt.Toolkit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.delay

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Self-contained desktop pomodoro widget, mirroring the WEB planner's
 * FloatingPomodoro / PomodoroWidget behaviour.
 *
 * Owns all of its state (store-backed): profile selection + inline
 * create/delete, focus/break countdown with [LaunchedEffect], beep +
 * session logging on phase end with auto-switch, and the daily-goal stepper.
 */
@Composable
fun PomodoroWidget(pal: NurPalette, modifier: Modifier = Modifier) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    var profiles by remember { mutableStateOf(PomodoroStore.profiles()) }
    var activeId by remember { mutableStateOf(PomodoroStore.getActiveId()) }
    var mode by remember { mutableStateOf("focus") }
    var running by remember { mutableStateOf(false) }
    var goal by remember { mutableStateOf(PomodoroStore.getDailyGoal()) }
    var logVersion by remember { mutableStateOf(0) }

    fun activeProfile() = profiles.firstOrNull { it.id == activeId } ?: profiles.first()
    fun phaseTotalSec() =
        if (mode == "focus") activeProfile().focusMinutes * 60 else activeProfile().breakMinutes * 60

    var secondsLeft by remember { mutableStateOf(phaseTotalSec()) }

    // Keep the countdown in sync when the profile or mode changes while paused.
    LaunchedEffect(activeId, mode, profiles) {
        if (!running) secondsLeft = phaseTotalSec()
    }

    val today = remember(logVersion) { LocalDate.now().format(dateFmt) }
    val todayCount = remember(logVersion) { PomodoroStore.todayCount(today) }

    fun handlePhaseEnd(finishedMode: String) {
        Toolkit.getDefaultToolkit().beep()
        val profile = activeProfile()
        if (finishedMode == "focus") {
            val focusSec = (profile.focusMinutes * 60).toLong()
            PomodoroStore.logSession(today, focusSec, "focus")
            // Mirror into the analytics session log (SessionStore only, never PlannerStore).
            SessionStore.log("focus", null, profile.focusMinutes)
            mode = "break"
            secondsLeft = profile.breakMinutes * 60
        } else {
            PomodoroStore.logSession(today, (profile.breakMinutes * 60).toLong(), "break")
            mode = "focus"
            secondsLeft = profile.focusMinutes * 60
        }
        logVersion++
    }

    LaunchedEffect(running, mode, activeId) {
        while (running) {
            delay(1000L)
            if (secondsLeft <= 1) {
                handlePhaseEnd(mode)
            } else {
                secondsLeft--
            }
        }
    }

    fun fmt(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        return "%02d:%02d".format(s / 60, s % 60)
    }

    val total = phaseTotalSec().coerceAtLeast(1)
    val progress = 1f - (secondsLeft.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    var menuExpanded by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newFocus by remember { mutableStateOf("25") }
    var newBreak by remember { mutableStateOf("5") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = pal.gold,
        unfocusedBorderColor = pal.boneDark,
        focusedTextColor = pal.ink,
        unfocusedTextColor = pal.ink
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pomodoro",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (mode == "focus") pal.teal else pal.gold)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (mode == "focus") "FOCUS" else "BREAK",
                        fontFamily = fontBody,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profile selector.
            Box {
                OutlinedButton(
                    onClick = { menuExpanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = pal.ink)
                ) {
                    Text(
                        text = activeProfile().name,
                        fontFamily = fontBody,
                        fontSize = 13.sp
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    profiles.forEach { p ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${p.name}  ${p.focusMinutes}/${p.breakMinutes}",
                                    fontFamily = fontBody,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                PomodoroStore.setActive(p.id)
                                activeId = p.id
                                mode = "focus"
                                running = false
                                secondsLeft = p.focusMinutes * 60
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showCreate = !showCreate }) {
                    Text(
                        if (showCreate) "Cancel" else "New profile",
                        fontFamily = fontBody,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        PomodoroStore.deleteProfile(activeId)
                        profiles = PomodoroStore.profiles()
                        activeId = PomodoroStore.getActiveId()
                        mode = "focus"
                        running = false
                        secondsLeft = (profiles.firstOrNull { it.id == activeId }
                            ?.focusMinutes ?: 25) * 60
                    },
                    enabled = profiles.size > 1
                ) {
                    Text("Delete", fontFamily = fontBody, fontSize = 12.sp)
                }
            }

            if (showCreate) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name", fontSize = 12.sp) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFocus,
                        onValueChange = { newFocus = it.filter(Char::isDigit).take(3) },
                        label = { Text("Focus min", fontSize = 12.sp) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newBreak,
                        onValueChange = { newBreak = it.filter(Char::isDigit).take(3) },
                        label = { Text("Break min", fontSize = 12.sp) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val focusMin = newFocus.toIntOrNull()?.coerceIn(1, 180) ?: 25
                        val breakMin = newBreak.toIntOrNull()?.coerceIn(1, 60) ?: 5
                        val profile = PomodoroStore.PomodoroProfile(
                            id = UUID.randomUUID().toString(),
                            name = newName.ifBlank { "Custom $focusMin/$breakMin" },
                            focusMinutes = focusMin,
                            breakMinutes = breakMin
                        )
                        PomodoroStore.saveProfile(profile)
                        PomodoroStore.setActive(profile.id)
                        profiles = PomodoroStore.profiles()
                        activeId = profile.id
                        mode = "focus"
                        running = false
                        secondsLeft = focusMin * 60
                        newName = ""
                        newFocus = "25"
                        newBreak = "5"
                        showCreate = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
                ) {
                    Text("Save profile", fontFamily = fontBody, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = fmt(secondsLeft),
                fontFamily = fontUi,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = pal.gold,
                trackColor = pal.boneDark
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { running = !running },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (running) "Pause" else "Start",
                        fontFamily = fontBody,
                        fontSize = 14.sp
                    )
                }
                OutlinedButton(
                    onClick = {
                        running = false
                        secondsLeft = phaseTotalSec()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset", fontFamily = fontBody, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        goal = (goal - 1).coerceAtLeast(1)
                        PomodoroStore.putDailyGoal(goal)
                    }) { Text("−", fontFamily = fontBody) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$todayCount/$goal today",
                        fontFamily = fontBody,
                        fontSize = 13.sp,
                        color = pal.inkMid
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        goal = (goal + 1).coerceAtMost(32)
                        PomodoroStore.putDailyGoal(goal)
                    }) { Text("+", fontFamily = fontBody) }
                }
                Text(
                    text = "${activeProfile().focusMinutes}m focus · ${activeProfile().breakMinutes}m break",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted
                )
            }
        }
    }
}
