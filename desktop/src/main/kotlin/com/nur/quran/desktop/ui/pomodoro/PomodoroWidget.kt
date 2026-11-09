package com.nur.quran.desktop.ui.pomodoro

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

private fun formatClock(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/**
 * Pomodoro configuration card, mirroring the WEB planner's PomodoroWidget.
 *
 * All timer state lives in [PomodoroController] so the same session survives
 * minimize / navigation. Pass [onCollapse] to render a close affordance in the
 * header (used by the floating dock).
 */
@Composable
fun PomodoroWidget(
    pal: NurPalette,
    modifier: Modifier = Modifier,
    onCollapse: (() -> Unit)? = null
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    val profiles = PomodoroController.profiles
    val activeId = PomodoroController.activeId
    val mode = PomodoroController.mode
    val running = PomodoroController.running
    val secondsLeft = PomodoroController.secondsLeft
    val goal = PomodoroController.goal
    val todayCount = PomodoroController.todayCount
    val activeProfile = PomodoroController.activeProfile()

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

    val total = PomodoroController.phaseTotalSec().coerceAtLeast(1)
    val progress = 1f - (secondsLeft.toFloat() / total.toFloat()).coerceIn(0f, 1f)

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            color = Color.White
                        )
                    }
                    if (onCollapse != null) {
                        IconButton(onClick = onCollapse) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Minimize pomodoro",
                                tint = pal.inkMuted
                            )
                        }
                    }
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
                        text = activeProfile.name,
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
                                PomodoroController.selectProfile(p.id)
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
                    onClick = { PomodoroController.deleteActiveProfile() },
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
                        PomodoroController.createProfile(
                            name = newName,
                            focusMinutes = newFocus.toIntOrNull() ?: 25,
                            breakMinutes = newBreak.toIntOrNull() ?: 5
                        )
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
                text = formatClock(secondsLeft),
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
                    onClick = { PomodoroController.toggleRunning() },
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
                    onClick = { PomodoroController.reset() },
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
                    OutlinedButton(onClick = { PomodoroController.updateGoal(goal - 1) }) {
                        Text("−", fontFamily = fontBody)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$todayCount/$goal today",
                        fontFamily = fontBody,
                        fontSize = 13.sp,
                        color = pal.inkMid
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { PomodoroController.updateGoal(goal + 1) }) {
                        Text("+", fontFamily = fontBody)
                    }
                }
                Text(
                    text = "${activeProfile.focusMinutes}m focus · ${activeProfile.breakMinutes}m break",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted
                )
            }
        }
    }
}

/**
 * App-wide floating pomodoro: a compact pill (web `FloatingPomodoro`) that
 * expands into the full [PomodoroWidget] config card.
 *
 * Mount it once at the app root, aligned bottom-end. The ticker lives here so
 * it keeps running even while the card is collapsed or the user navigates.
 */
@Composable
fun PomodoroDock(pal: NurPalette, modifier: Modifier = Modifier) {
    if (!PomodoroController.dockVisible) return

    // Only tick while the timer runs; an always-on frame loop would keep the
    // app busy (and hang auto-advancing compose tests).
    val running = PomodoroController.running
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var lastTick = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (now - lastTick >= 1_000_000_000L) {
                    lastTick = now
                    PomodoroController.advance(1)
                }
            }
        }
    }

    val fontUi = rememberUiFontFamily()
    val expanded = PomodoroController.dockExpanded
    val focusMode = PomodoroController.mode == "focus"

    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        if (expanded) {
            PomodoroWidget(
                pal = pal,
                modifier = Modifier.widthIn(max = 360.dp).padding(20.dp),
                onCollapse = { PomodoroController.dockExpanded = false }
            )
        } else {
            Card(
                modifier = Modifier.padding(20.dp),
                shape = RoundedCornerShape(999.dp),
                colors = CardDefaults.cardColors(containerColor = pal.cream),
                border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = if (focusMode) pal.teal else pal.gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatClock(PomodoroController.secondsLeft),
                        fontFamily = fontUi,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(22.dp)
                            .background(pal.boneDark)
                    )
                    IconButton(
                        onClick = { PomodoroController.toggleRunning() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (running) pal.teal else pal.white)
                    ) {
                        Icon(
                            if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (running) "Pause pomodoro" else "Start pomodoro",
                            tint = if (running) Color.White else pal.ink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { PomodoroController.dockExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Pomodoro settings",
                            tint = pal.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
