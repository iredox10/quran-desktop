package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
 * Web parity (Planner.jsx lines 803-810): totalSeconds -> "1h 5m" / "5m 3s" / "12s".
 */
internal fun formatSessionTime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    if (h > 0) return "${h}h ${m}m"
    if (m > 0) return "${m}m ${s}s"
    return "${s}s"
}

/**
 * TODAY tab of the planner, mirroring web `Planner.jsx` ActiveView TODAY tab
 * (lines 669-1063): 200dp RingProgress hero with "% + day count"
 * + "Daily Ritual" column (session-time pill, share-progress, settings gear
 * opening PlannerSettingsDialog via [onOpenSettings]) + sequential
 * prayer-slot rows exactly like web buildPrayerSlots
 * (completed/current/locked/upcoming/empty with Start/Resume deep-link
 * route to the day reader, undo, mark-done circle) + resumeRoute from the
 * last position (first incomplete = today when unfinished).
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
    onSlotUndo: (dayDate: String, fromCount: Int) -> Unit = { _, _ -> },
    onShareProgress: (() -> Unit)? = null
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

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
    // Web parity (Planner.jsx lines 764-766): page-unit counts.
    val todayDone = progress?.readPagesCount ?: 0
    val todayTotal = progress?.totalPagesCount ?: 0
    val todayPct = if (todayTotal > 0) ((todayDone * 100) / todayTotal).coerceIn(0, 100) else 0
    // Web parity (line 769): hasStartedReading = todayDone > 0 || lastReadPage.
    val hasStarted = todayDone > 0 || plan.lastReadPage != null
    val dayDate = todayAssignment?.date ?: todayStr

    // Web parity (line 821): "N OF M <UNIT>S TODAY".
    val unitUpper = (PLANNER_UNITS[plan.unitType]?.label ?: plan.unitType).uppercase()
    val unitPlural = PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
    val ringLabel = "$todayDone OF $todayTotal ${unitUpper}S TODAY"
    // Current day count under the ring (web header "Day X of N").
    val currentDay = overview?.currentDayNumber ?: todayAssignment?.dayNumber ?: 1

    // Session-time pill, web parity (lines 802-810): plannerSessionTimers
    // totalSeconds formatted "1h 5m" / "5m 3s" / "12s". Desktop
    // PlannerStore keeps minutes per date; convert to seconds for the same
    // format function.
    val sessionSeconds = remember(plan.id, todayStr) {
        (PlannerStore.getStudySessions()[todayStr] ?: 0) * 60
    }
    val sessionLabel = remember(sessionSeconds) { formatSessionTime(sessionSeconds) }
    val completedDays = overview?.completedCount ?: 0
    // Web parity (line 768): resumeRoute = first-incomplete day reader.
    val resumeDayDate = overview?.firstIncomplete?.date
    // Full web share text (lines 791-794); coordinator override wins.
    val shareText =
        "I've completed $completedDays days of my ${plan.durationDays}-day Quran reading plan " +
            "on Quran Nur! I'm currently on Day $currentDay."

    // Prayer timings load async (blocking IO); slots render with null-safe
    // time pills while loading (buildPrayerSlots accepts null timings).
    var timings by remember { mutableStateOf<PrayerTimings?>(null) }
    LaunchedEffect(plan.id) {
        timings = withContext(Dispatchers.IO) { PrayerStore.getToday() }
    }
    // Web parity: slots honor the saved prayerSettings (activePrayers subset
    // + before/after/split read preference) from the Planner Settings dialog.
    val prayerPrefs = remember { PlannerStore.getPrayerPrefs() }
    val slots = remember(plan, todayAssignment, timings, prayerPrefs) {
        buildPrayerSlots(
            plan = plan,
            todayAssignment = todayAssignment,
            prayerTimings = timings,
            readPreference = prayerPrefs.readPreference,
            activePrayers = prayerPrefs.activePrayers
        )
    }

    var copied by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Web parity (Planner.jsx 946-948): 240px ring, 3.5rem label,
                // 0.65rem mono uppercase ring label.
                PaceRing(
                    pal = pal,
                    progress = todayPct / 100f,
                    label = "$todayPct%",
                    subLabel = ringLabel,
                    diameter = 240.dp,
                    labelFontSize = 56.sp,
                    subLabelFontSize = 10.4.sp
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
                if (sessionSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = pal.tealSoft
                    ) {
                        Text(
                            text = sessionLabel,
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
                            // Coordinator override wins; desktop fallback copies
                            // the full web share text to the clipboard.
                            onShareProgress?.invoke() ?: run {
                                copied = copyToClipboard(shareText)
                            }
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

        Column(verticalArrangement = Arrangement.spacedBy(12.8.dp)) {
            slots.forEach { slot ->
                val isCurrent = slot.status == "current"
                val isCompleted = slot.status == "completed"
                val isUpcoming = slot.status == "upcoming"
                val isLocked = slot.status == "locked"
                val isEmpty = slot.status == "empty"

                // Web parity (Planner.jsx 987-997): current scales up 2%,
                // completed/empty/locked scale down to 95% and fade, and the
                // completed/locked/empty rows use tighter vertical padding.
                val scale = when {
                    isCurrent -> 1.02f
                    isCompleted || isEmpty || isLocked -> 0.95f
                    else -> 1f
                }
                val opacity = when {
                    isCompleted -> 0.8f
                    isEmpty || isLocked -> 0.7f
                    else -> 1f
                }
                val container = when {
                    isCurrent -> pal.white
                    isCompleted -> pal.green.copy(alpha = 0.03f)
                    isUpcoming -> pal.white
                    else -> pal.cream.copy(alpha = 0.5f)
                }
                val border = when {
                    isCurrent -> BorderStroke(1.dp, pal.gold)
                    isCompleted -> BorderStroke(1.dp, pal.green.copy(alpha = 0.2f))
                    else -> BorderStroke(1.5.dp, pal.boneDark)
                }
                val iconBg = when {
                    isCompleted -> pal.green
                    isCurrent -> pal.gold
                    isUpcoming -> pal.bone
                    else -> Color.Transparent
                }
                val iconBorder = if (isEmpty || isLocked) {
                    BorderStroke(1.dp, pal.boneDark)
                } else null
                val subLabel = when {
                    isCompleted -> "${slot.doneInSlot}/${slot.count} $unitPlural ✓"
                    isCurrent -> "${slot.doneInSlot} of ${slot.count} $unitPlural read"
                    isUpcoming -> "${slot.count} $unitPlural · pending"
                    isLocked -> "${slot.count} $unitPlural · locked"
                    else -> "No reading required"
                }
                val verticalPadding = if (isCompleted || isEmpty || isLocked) 10.dp else 16.dp

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = container,
                    border = border,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .alpha(opacity)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = verticalPadding
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(iconBg)
                                .then(
                                    if (iconBorder != null) {
                                        Modifier.border(iconBorder, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCompleted -> Icon(
                                    Icons.Filled.Check,
                                    contentDescription = slot.status,
                                    tint = pal.white,
                                    modifier = Modifier.size(20.dp)
                                )
                                isCurrent -> Icon(
                                    Icons.Filled.MenuBook,
                                    contentDescription = slot.status,
                                    tint = pal.white,
                                    modifier = Modifier.size(20.dp)
                                )
                                isUpcoming -> Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = slot.status,
                                    tint = pal.inkMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                isLocked -> Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = slot.status,
                                    tint = pal.inkMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                else -> Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(pal.inkMuted)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = slot.name,
                                    fontFamily = fontUi,
                                    fontSize = 17.6.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isCompleted) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    },
                                    color = pal.ink,
                                    modifier = if (isCompleted) {
                                        Modifier.alpha(0.5f)
                                    } else {
                                        Modifier
                                    }
                                )
                                val slotTime = slot.time
                                if (slotTime != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = pal.bone,
                                        border = BorderStroke(1.5.dp, pal.boneDark)
                                    ) {
                                        Text(
                                            text = slotTime,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 11.2.sp,
                                            color = pal.inkMid,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subLabel,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp,
                                color = if (isCurrent) pal.gold else pal.inkMuted
                            )
                        }
                        Box(modifier = Modifier.size(32.dp)) {
                            when {
                                isCompleted -> Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(pal.green.copy(alpha = 0.12f))
                                        .border(
                                            1.dp,
                                            pal.green.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                        .clickable { onSlotUndo(dayDate, slot.slotStart) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Undo this prayer",
                                        tint = pal.green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                isCurrent -> Button(
                                    // Web parity (1033-1037): Start/Resume
                                    // deep-links to the day reader; Resume goes
                                    // to the first-incomplete day when available.
                                    onClick = { onOpenDay(resumeDayDate ?: dayDate) },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.gold,
                                        contentColor = pal.white
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 20.dp,
                                        vertical = 10.dp
                                    ),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Text(
                                        text = if (hasStarted) "Resume" else "Start",
                                        fontFamily = fontUi,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.2.sp
                                    )
                                }
                                isUpcoming -> Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(pal.white)
                                        .border(1.5.dp, pal.boneDark, CircleShape)
                                        .clickable { onSlotDone(dayDate, slot.slotEnd) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, pal.inkMuted, CircleShape)
                                    )
                                }
                                isLocked -> Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = pal.inkMuted.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                                else -> Text(
                                    text = "–",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    color = pal.inkMuted.copy(alpha = 0.3f),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
