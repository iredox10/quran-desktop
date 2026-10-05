package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.PrayerStore
import com.nur.quran.desktop.ui.components.copyToClipboard
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLANNER_UNITS
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.PrayerTimings
import com.nur.quran.shared.ReadingPlan
import com.nur.quran.shared.buildPrayerSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TODAY tab of the planner, mirroring web `Planner.jsx` ActiveView TODAY tab
 * (lines 669-1063): big ring + "Daily Ritual" column (session pill, share,
 * settings gear) + sequential prayer-slot rows with
 * current/completed/locked/upcoming/empty statuses.
 *
 * Sequential display only — no lock-enforcement logic beyond status display;
 * progress writes are delegated to [onSlotDone]/[onSlotUndo] (coordinator).
 */
@Composable
fun TodayTab(
    pal: NurPalette,
    plan: ReadingPlan,
    todayStr: String,
    onOpenDay: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onSlotDone: (dayDate: String, upTo: Int) -> Unit = { _, _ -> },
    onSlotUndo: (dayDate: String, fromCount: Int) -> Unit = { _, _ -> }
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    // PlannerOverview has no todayAssignment field (verified) — resolve here,
    // same fallback as web: date match, else currentDayNumber - 1.
    val overview = remember(plan, todayStr) {
        PlannerEngine.getPlannerOverview(plan, todayStr)
    }
    val todayAssignment = remember(plan, todayStr) {
        plan.assignments.find { it.date == todayStr }
            ?: plan.assignments.getOrNull((overview?.currentDayNumber ?: 1) - 1)
    }
    val progress = remember(plan, todayAssignment) {
        todayAssignment?.let { PlannerEngine.getAssignmentProgress(plan, it) }
    }
    val todayDone = progress?.readPagesCount ?: 0
    val todayTotal = progress?.totalPagesCount ?: 0
    val todayPct = if (todayTotal > 0) ((todayDone * 100) / todayTotal).coerceIn(0, 100) else 0
    val hasStarted = todayDone > 0
    val dayDate = todayAssignment?.date ?: todayStr

    // Unit label uppercase from plan.unitType, web-style ("PAGE" -> "PAGES").
    val unitUpper = (PLANNER_UNITS[plan.unitType]?.label ?: plan.unitType).uppercase()
    val unitPlural = PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
    val ringLabel = "$todayDone OF $todayTotal ${unitUpper}S TODAY"

    // Session minutes pill: PlannerStore shape is Map<String(date), Int(minutes)>.
    val sessionMinutes = remember(plan.id, todayStr) {
        PlannerStore.getStudySessions()[todayStr] ?: 0
    }
    val completedDays = overview?.completedCount ?: 0

    // Prayer timings load async (blocking IO); slots render with null-safe
    // time pills while loading (buildPrayerSlots accepts null timings).
    var timings by remember { mutableStateOf<PrayerTimings?>(null) }
    LaunchedEffect(plan.id) {
        timings = withContext(Dispatchers.IO) { PrayerStore.getToday() }
    }
    val slots = remember(plan, todayAssignment, timings) {
        buildPrayerSlots(plan, todayAssignment, timings)
    }

    var copied by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PaceRing(
                    pal = pal,
                    progress = todayPct / 100f,
                    label = "$todayPct%",
                    modifier = Modifier.size(160.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ringLabel,
                    fontFamily = fontBody,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = pal.inkMuted
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle(pal = pal, text = "Daily Ritual")
                if (todayAssignment != null) {
                    Text(
                        text = todayAssignment.title,
                        fontFamily = fontUi,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.ink
                    )
                }
                if (sessionMinutes > 0) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = pal.tealSoft
                    ) {
                        Text(
                            text = "$sessionMinutes min today",
                            fontFamily = fontBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.teal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            copied = copyToClipboard(
                                "I've completed $completedDays days of my ${plan.durationDays}-day plan"
                            )
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(pal.cream)
                            .border(1.dp, pal.boneDark, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = if (copied) "Copied" else "Share progress",
                            tint = if (copied) pal.green else pal.inkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(pal.cream)
                            .border(1.dp, pal.boneDark, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Planner settings",
                            tint = pal.inkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slots.forEach { slot ->
                val isCurrent = slot.status == "current"
                val isCompleted = slot.status == "completed"
                val isUpcoming = slot.status == "upcoming"
                val isLocked = slot.status == "locked"
                val isEmpty = slot.status == "empty"

                val container = when {
                    isCurrent -> pal.cream
                    isCompleted -> pal.green.copy(alpha = 0.08f)
                    else -> pal.cream.copy(alpha = 0.6f)
                }
                val border = when {
                    isCurrent -> BorderStroke(2.dp, pal.gold)
                    isCompleted -> BorderStroke(1.dp, pal.green.copy(alpha = 0.4f))
                    else -> BorderStroke(1.dp, pal.boneDark)
                }
                // Icons verified by grep: Check, MenuBook, Info, Settings, Share.
                // Lock / Schedule / Clock are NOT in the codebase -> Info fallback.
                val statusIcon = when {
                    isCompleted -> Icons.Filled.Check
                    isCurrent -> Icons.Filled.MenuBook
                    else -> Icons.Filled.Info
                }
                val iconBg = when {
                    isCompleted -> pal.green
                    isCurrent -> pal.gold
                    else -> pal.bone
                }
                val iconTint = when {
                    isCompleted || isCurrent -> pal.white
                    else -> pal.inkMuted.copy(alpha = 0.6f)
                }
                val subLabel = when {
                    isCompleted -> "${slot.doneInSlot}/${slot.count} $unitPlural ✓"
                    isCurrent -> "${slot.doneInSlot} of ${slot.count} $unitPlural read"
                    isUpcoming -> "${slot.count} $unitPlural · pending"
                    isLocked -> "${slot.count} $unitPlural · locked"
                    else -> "No reading required"
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = container,
                    border = border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                statusIcon,
                                contentDescription = slot.status,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = slot.name,
                                    fontFamily = fontUi,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted || isLocked || isEmpty) {
                                        pal.inkMuted
                                    } else {
                                        pal.ink
                                    }
                                )
                                val slotTime = slot.time
                                if (slotTime != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = pal.bone
                                    ) {
                                        Text(
                                            text = slotTime,
                                            fontFamily = fontBody,
                                            fontSize = 10.sp,
                                            color = pal.inkMid,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subLabel,
                                fontFamily = fontBody,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isCurrent) pal.gold else pal.inkMuted
                            )
                        }
                        when {
                            isCompleted -> IconButton(
                                onClick = { onSlotUndo(dayDate, slot.slotStart) }
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Undo this slot",
                                    tint = pal.green,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            isCurrent -> Button(
                                onClick = { onOpenDay(dayDate) },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = pal.gold,
                                    contentColor = pal.white
                                )
                            ) {
                                Text(
                                    text = if (hasStarted) "Resume" else "Start",
                                    fontFamily = fontUi,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            isUpcoming -> Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, pal.boneDark, CircleShape)
                                    .clickable { onSlotDone(dayDate, slot.slotEnd) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, pal.inkMuted, CircleShape)
                                )
                            }
                            isLocked -> Icon(
                                Icons.Filled.Info,
                                contentDescription = "Locked",
                                tint = pal.inkMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Text(
                                text = "–",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                color = pal.inkMuted.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
