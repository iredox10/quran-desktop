package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.PrayerStore
import com.nur.quran.desktop.ui.components.copyToClipboard
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLANNER_UNITS
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.PrayerSlot
import com.nur.quran.shared.PrayerTimings
import com.nur.quran.shared.ReadingPlan
import com.nur.quran.shared.buildPrayerSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * (lines 669-1063):
 * - 240dp Circular RingProgress hero with ambient radial glow drop shadow,
 *   prominent todayPct%, and uppercase subtitle tracking ("X OF Y PAGES TODAY").
 * - "Daily Ritual" column with teal accent pill marker, session timer pill (if >0s),
 *   share and settings action buttons.
 * - Sequential prayer slots (Fajr, Dhuhr, Asr, Maghrib, Isha) with timing label,
 *   visual state (completed, current, upcoming, locked, empty), Start/Resume action,
 *   mark-done circle, and undo button.
 * - Primary CTA Button with dynamic label navigating directly to the reader.
 * - Touch & tablet compatibility (48dp touch targets on interactive controls).
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
    val todayComplete = progress?.isComplete ?: false

    val nextAssignment = if (todayComplete) {
        plan.assignments.firstOrNull { a ->
            (todayAssignment == null || a.dayNumber > todayAssignment.dayNumber) &&
                !PlannerEngine.getAssignmentProgress(plan, a).isComplete
        }
    } else null
    val planDone = (overview?.isFinishedWindow ?: false) && nextAssignment == null
    val ctaRouteDate = (nextAssignment ?: overview?.firstIncomplete)?.date ?: dayDate

    // Dynamic CTA label matching web parity (Planner.jsx 825-830)
    val ctaLabel = when {
        planDone -> "Plan Complete ✓"
        todayComplete && nextAssignment != null -> "Continue to Day ${nextAssignment.dayNumber}"
        todayComplete -> "All Caught Up"
        hasStarted -> "Resume Reading"
        else -> "Open Al-Quran"
    }

    // Web parity (line 821): "N OF M <UNIT>S TODAY".
    val unitUpper = (PLANNER_UNITS[plan.unitType]?.label ?: plan.unitType).uppercase()
    val unitPlural = PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
    val ringLabel = "$todayDone OF $todayTotal ${unitUpper}S TODAY"
    val currentDay = overview?.currentDayNumber ?: todayAssignment?.dayNumber ?: 1

    // Session-time pill, web parity (lines 802-810): plannerSessionTimers
    val sessionSeconds = remember(plan.id, todayStr) {
        (PlannerStore.getStudySessions()[todayStr] ?: 0) * 60
    }
    val sessionLabel = remember(sessionSeconds) { formatSessionTime(sessionSeconds) }
    val completedDays = overview?.completedCount ?: 0
    val resumeDayDate = overview?.firstIncomplete?.date
    val shareText =
        "I've completed $completedDays days of my ${plan.durationDays}-day Quran reading plan " +
            "on Quran Nur! I'm currently on Day $currentDay."

    var timings by remember { mutableStateOf<PrayerTimings?>(null) }
    LaunchedEffect(plan.id) {
        timings = withContext(Dispatchers.IO) { PrayerStore.getToday() }
    }
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
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    val handleShare = {
        onShareProgress?.invoke() ?: run {
            copied = copyToClipboard(shareText)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val isNarrow = maxWidth < 720.dp
        if (isNarrow) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Centered Hero PaceRing with radial glow
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    RingHero(pal = pal, todayPct = todayPct, ringLabel = ringLabel)
                }

                // Daily Ritual Column
                DailyRitualSection(
                    pal = pal,
                    fontUi = fontUi,
                    sessionSeconds = sessionSeconds,
                    sessionLabel = sessionLabel,
                    copied = copied,
                    onShare = handleShare,
                    onOpenSettings = onOpenSettings,
                    todayAssignmentTitle = todayAssignment?.title,
                    slots = slots,
                    unitPlural = unitPlural,
                    hasStarted = hasStarted,
                    dayDate = dayDate,
                    ctaLabel = ctaLabel,
                    onCtaClick = { onOpenDay(resumeDayDate ?: ctaRouteDate) },
                    onSlotDone = onSlotDone,
                    onSlotUndo = onSlotUndo
                )
            }
        } else {
            // Side-by-side desktop layout matching web Planner.jsx (md:flex-row gap-10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: 240dp Circular RingProgress hero
                Box(
                    modifier = Modifier.width(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RingHero(pal = pal, todayPct = todayPct, ringLabel = ringLabel)
                }

                // Right Column: Daily Ritual + Prayer Slots + Primary CTA
                DailyRitualSection(
                    pal = pal,
                    fontUi = fontUi,
                    sessionSeconds = sessionSeconds,
                    sessionLabel = sessionLabel,
                    copied = copied,
                    onShare = handleShare,
                    onOpenSettings = onOpenSettings,
                    todayAssignmentTitle = todayAssignment?.title,
                    slots = slots,
                    unitPlural = unitPlural,
                    hasStarted = hasStarted,
                    dayDate = dayDate,
                    ctaLabel = ctaLabel,
                    onCtaClick = { onOpenDay(resumeDayDate ?: ctaRouteDate) },
                    onSlotDone = onSlotDone,
                    onSlotUndo = onSlotUndo,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 240dp Circular RingProgress hero with ambient radial glow / accent drop shadow.
 * Mirrors web Planner.jsx lines 944-950.
 */
@Composable
private fun RingHero(pal: NurPalette, todayPct: Int, ringLabel: String) {
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial glow behind the ring
        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer { alpha = 0.22f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(pal.teal.copy(alpha = 0.45f), Color.Transparent),
                        radius = 240f
                    ),
                    shape = CircleShape
                )
        )
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
}

/**
 * Daily Ritual column containing:
 * - Header with teal accent pill marker, session timer pill, share and settings buttons.
 * - Sequential prayer slots (completed, current, upcoming, locked, empty).
 * - Primary CTA Button.
 */
@Composable
private fun DailyRitualSection(
    pal: NurPalette,
    fontUi: FontFamily,
    sessionSeconds: Int,
    sessionLabel: String,
    copied: Boolean,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    todayAssignmentTitle: String?,
    slots: List<PrayerSlot>,
    unitPlural: String,
    hasStarted: Boolean,
    dayDate: String,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    onSlotDone: (dayDate: String, upTo: Int) -> Unit,
    onSlotUndo: (dayDate: String, fromCount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Daily Ritual Header Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Teal accent pill marker (web parity line 956)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(24.dp)
                        .clip(CircleShape)
                        .background(pal.teal)
                )
                Text(
                    text = "Daily Ritual",
                    fontFamily = fontUi,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Session timer pill if > 0 (web parity lines 959-964)
                if (sessionSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = pal.tealSoft,
                        border = BorderStroke(1.dp, pal.teal.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = "Reading session time",
                                tint = pal.teal,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = sessionLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.teal
                            )
                        }
                    }
                }

                // Share progress button (web parity lines 966-972)
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(pal.cream)
                        .border(1.5.dp, pal.boneDark, CircleShape)
                        .touchTarget()
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = if (copied) "Copied!" else "Share Progress",
                        tint = if (copied) pal.green else pal.inkMid,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Settings button (web parity lines 973-975)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(pal.cream)
                        .border(1.5.dp, pal.boneDark, CircleShape)
                        .touchTarget()
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Planner Settings",
                        tint = pal.inkMid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (todayAssignmentTitle != null) {
            Text(
                text = todayAssignmentTitle,
                fontFamily = fontUi,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.inkMid
            )
        }

        // ── Sequential Prayer Slots ──
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slots.forEach { slot ->
                val isCurrent = slot.status == "current"
                val isCompleted = slot.status == "completed"
                val isUpcoming = slot.status == "upcoming"
                val isLocked = slot.status == "locked"
                val isEmpty = slot.status == "empty"

                val scale = when {
                    isCurrent -> 1.015f
                    isCompleted || isEmpty || isLocked -> 0.96f
                    else -> 1f
                }
                val opacity = when {
                    isCompleted -> 0.85f
                    isEmpty || isLocked -> 0.7f
                    else -> 1f
                }
                val container = when {
                    isCurrent -> pal.white
                    isCompleted -> pal.green.copy(alpha = 0.04f)
                    isUpcoming -> pal.white
                    else -> pal.cream.copy(alpha = 0.5f)
                }
                val border = when {
                    isCurrent -> BorderStroke(1.5.dp, pal.teal)
                    isCompleted -> BorderStroke(1.dp, pal.green.copy(alpha = 0.25f))
                    else -> BorderStroke(1.5.dp, pal.boneDark)
                }
                val iconBg = when {
                    isCompleted -> pal.green
                    isCurrent -> pal.teal
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
                val verticalPadding = if (isCompleted || isEmpty || isLocked) 10.dp else 14.dp

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = container,
                    border = border,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .alpha(opacity)
                        .then(
                            if (isCurrent) Modifier.clickable { onCtaClick() }
                            else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = verticalPadding
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Slot Status Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                                    Icons.AutoMirrored.Filled.MenuBook,
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

                        // Slot Info
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
                                    modifier = if (isCompleted) Modifier.alpha(0.5f) else Modifier
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
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = pal.inkMid,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = subLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = if (isCurrent) pal.teal else pal.inkMuted
                            )
                        }

                        // Action Controls
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                isCompleted -> Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(pal.green.copy(alpha = 0.12f))
                                        .border(
                                            1.dp,
                                            pal.green.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                        .touchTarget()
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
                                isCurrent -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Mark complete check button
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(pal.white)
                                            .border(1.5.dp, pal.teal, CircleShape)
                                            .touchTarget()
                                            .clickable { onSlotDone(dayDate, slot.slotEnd) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Mark slot done",
                                            tint = pal.teal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    // Start / Resume Button
                                    Button(
                                        onClick = { onCtaClick() },
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = pal.teal,
                                            contentColor = pal.white
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        modifier = Modifier.touchTarget()
                                    ) {
                                        Text(
                                            text = if (hasStarted) "Resume" else "Start Reading",
                                            fontFamily = fontUi,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                isUpcoming -> Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(pal.white)
                                        .border(1.5.dp, pal.boneDark, CircleShape)
                                        .touchTarget()
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
                                isLocked -> Box(
                                    modifier = Modifier.size(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Locked",
                                        tint = pal.inkMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                else -> Box(
                                    modifier = Modifier.size(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "–",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Light,
                                        color = pal.inkMuted.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Primary CTA Button (bottom of ritual column) ──
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onCtaClick,
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = pal.teal,
                contentColor = pal.white
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .touchTarget()
        ) {
            Text(
                text = ctaLabel,
                fontFamily = fontUi,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
