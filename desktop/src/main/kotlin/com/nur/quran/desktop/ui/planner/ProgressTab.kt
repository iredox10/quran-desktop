package com.nur.quran.desktop.ui.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLANNER_UNITS
import com.nur.quran.shared.PlannerAssignment
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import com.nur.quran.shared.WeeklySummary
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val OverdueRed = Color(0xFFDC2626)
private val SuccessGreen = Color(0xFF10B981)

private enum class ScheduleStatus {
    AHEAD,
    BEHIND,
    ON_TRACK
}

/**
 * PROGRESS tab of the planner, mirroring web Planner.jsx ActiveView
 * PROGRESS tab (lines ~1065-1232): pace ring + pace stats, Timeline day-dot
 * grid (38px dots, completed/today/partial/overdue/upcoming styles,
 * partial fill arc, difficulty red/green dots, legend), selected-day
 * detail card with "Read Day" button, Performance Insights (onTimeRate, catch-ups, pace),
 * weekly summary bars (getWeeklySummary, last 4), Action Required
 * (extend vs spread), and touch/tablet accessibility.
 *
 * Mutations go through [onAdoptPlan] (screen coordinator persists +
 * refreshes), matching web setPlanner(updated).
 */
@Composable
fun ProgressTab(
    pal: NurPalette,
    plan: ReadingPlan,
    todayStr: String,
    onAdoptPlan: (ReadingPlan) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDay: (String) -> Unit = {}
) {
    var selectedDay by remember(plan.id) { mutableStateOf<PlannerAssignment?>(null) }
    var showAdjust by remember { mutableStateOf(false) }
    var showRebalance by remember { mutableStateOf(false) }

    val overview = remember(plan, todayStr) { PlannerEngine.getPlannerOverview(plan, todayStr) }
    val analytics = remember(plan, todayStr) { PlannerEngine.getPlannerAnalytics(plan, todayStr) }
    val difficulty = remember(plan) { PlannerEngine.getDifficultyIndicators(plan.assignments) }
    val weekly = remember(plan) { PlannerEngine.getWeeklySummary(plan) }

    val completedDays = overview?.completedCount ?: 0
    val overdueDays = overview?.overdueDays ?: 0
    val dueAssignmentsCount = plan.assignments.count { it.date <= todayStr }

    // Schedule status computation (Ahead, Behind, On Track)
    val scheduleStatus = when {
        overdueDays > 0 -> ScheduleStatus.BEHIND
        completedDays > dueAssignmentsCount -> ScheduleStatus.AHEAD
        else -> ScheduleStatus.ON_TRACK
    }
    val daysAhead = max(0, completedDays - dueAssignmentsCount)

    // Projected completion calculations
    val totalUnits = remember(plan) {
        plan.assignments.sumOf { PlannerEngine.getAssignmentProgress(plan, it).totalPagesCount }
    }
    val totalReadUnits = remember(plan) {
        plan.assignments.sumOf { PlannerEngine.getAssignmentProgress(plan, it).readPagesCount }
    }
    val remainingUnits = max(0, totalUnits - totalReadUnits)
    val plannedDailyPages = ceil(totalUnits.toDouble() / max(1, plan.durationDays)).toInt()
    val currentPace = if (analytics.avgPagesPerDay > 0) analytics.avgPagesPerDay.roundToInt() else plannedDailyPages

    val targetEndDate = plan.assignments.lastOrNull()?.date ?: plan.startDate
    val targetDateLabel = remember(targetEndDate) { PlannerEngine.formatPlannerShortDate(targetEndDate) }

    val projectedDateLabel = remember(currentPace, remainingUnits, todayStr, targetDateLabel) {
        if (currentPace > 0 && remainingUnits > 0) {
            val remainingDaysNeeded = ceil(remainingUnits.toDouble() / currentPace).toInt()
            val projectedDateStr = PlannerEngine.addDays(todayStr, remainingDaysNeeded)
            PlannerEngine.formatPlannerShortDate(projectedDateStr)
        } else {
            targetDateLabel
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Pace Ring & Projected Completion Card
        PaceProjectedSection(
            pal = pal,
            plan = plan,
            currentPace = currentPace,
            plannedPace = plannedDailyPages,
            completedDays = completedDays,
            totalDays = plan.durationDays,
            targetDateLabel = targetDateLabel,
            projectedDateLabel = projectedDateLabel,
            scheduleStatus = scheduleStatus,
            overdueDays = overdueDays,
            daysAhead = daysAhead,
            onAdjust = { showAdjust = true }
        )

        // 2. Timeline Day-Dot Grid
        TimelineSection(
            pal = pal,
            plan = plan,
            todayStr = todayStr,
            completedDays = completedDays,
            difficulty = difficulty,
            selectedDay = selectedDay,
            onSelect = { selectedDay = it },
            onAdjust = { showAdjust = true },
            onRebalance = { showRebalance = true },
            onOpenDay = onOpenDay
        )

        // 3. Performance Insights Card
        PerformanceSection(
            pal = pal,
            onTimeRate = analytics.onTimeRate,
            catchUps = analytics.catchUpDays,
            pace = currentPace
        )

        // 4. Weekly Progress Section
        if (weekly.isNotEmpty()) {
            WeeklySection(pal = pal, weekly = weekly)
        }

        // 5. Rebalance Banner / Action Required
        if (overdueDays > 0) {
            OverdueSection(
                pal = pal,
                overdueDays = overdueDays,
                onExtend = { onAdoptPlan(PlannerEngine.rebalancePlanner(plan, "extend")) },
                onSpread = { onAdoptPlan(PlannerEngine.rebalancePlanner(plan, "spread")) }
            )
        }
    }

    if (showAdjust) {
        AdjustPaceDialog(
            pal = pal,
            plan = plan,
            onDismiss = { showAdjust = false },
            onApply = { nd ->
                onAdoptPlan(PlannerEngine.adjustPlannerPace(plan, nd))
                showAdjust = false
            }
        )
    }

    if (showRebalance) {
        RebalanceDialog(
            pal = pal,
            onDismiss = { showRebalance = false },
            onPick = { s ->
                onAdoptPlan(PlannerEngine.rebalancePlanner(plan, s))
                showRebalance = false
            }
        )
    }
}

/**
 * 1. Pace Ring & Projected Completion Card.
 * Displays pace ring (daily pages/pace), status badge (Ahead, Behind, On Track),
 * projected completion vs target dates, and "Adjust Pace" action button.
 */
@Composable
private fun PaceProjectedSection(
    pal: NurPalette,
    plan: ReadingPlan,
    currentPace: Int,
    plannedPace: Int,
    completedDays: Int,
    totalDays: Int,
    targetDateLabel: String,
    projectedDateLabel: String,
    scheduleStatus: ScheduleStatus,
    overdueDays: Int,
    daysAhead: Int,
    onAdjust: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val completionPct = if (totalDays > 0) Math.round((completedDays.toFloat() / totalDays) * 100) else 0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = pal.white,
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Speed,
                        contentDescription = null,
                        tint = pal.gold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pace & Projected Completion",
                        fontFamily = fontUi,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                }

                // Status Badge
                when (scheduleStatus) {
                    ScheduleStatus.AHEAD -> {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.40f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ahead of Schedule",
                                    fontFamily = fontUi,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                    ScheduleStatus.BEHIND -> {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = OverdueRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OverdueRed.copy(alpha = 0.40f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = OverdueRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Behind Schedule ($overdueDays d)",
                                    fontFamily = fontUi,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OverdueRed
                                )
                            }
                        }
                    }
                    ScheduleStatus.ON_TRACK -> {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = pal.teal.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, pal.teal.copy(alpha = 0.40f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = pal.teal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "On Track",
                                    fontFamily = fontUi,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.teal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body: Pace Ring + Trajectory stats + Adjust pace CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // PaceRing display with current pace / daily pages
                PaceCardRing(
                    pal = pal,
                    durationDays = totalDays,
                    dailyPages = if (currentPace > 0) currentPace else plannedPace,
                    selected = true
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Target vs Projected Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "TARGET DATE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$targetDateLabel ($totalDays d)",
                                fontFamily = fontUi,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink
                            )
                        }

                        Column {
                            Text(
                                text = "PROJECTED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = projectedDateLabel,
                                fontFamily = fontUi,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (scheduleStatus) {
                                    ScheduleStatus.BEHIND -> OverdueRed
                                    ScheduleStatus.AHEAD -> SuccessGreen
                                    ScheduleStatus.ON_TRACK -> pal.teal
                                }
                            )
                        }
                    }

                    // Context description
                    val statusMessage = when (scheduleStatus) {
                        ScheduleStatus.BEHIND -> "You have fallen $overdueDays day${if (overdueDays > 1) "s" else ""} behind schedule. Adjust your pace or redistribute missed pages below."
                        ScheduleStatus.AHEAD -> "You are $daysAhead day${if (daysAhead > 1) "s" else ""} ahead of schedule! At this pace you will complete earlier than targeted."
                        ScheduleStatus.ON_TRACK -> "$completedDays of $totalDays days completed ($completionPct%). Steady pace toward completion."
                    }
                    Text(
                        text = statusMessage,
                        fontFamily = fontBody,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = pal.inkMid
                    )

                    // Adjust Pace CTA Button
                    Row {
                        OutlinedButton(
                            onClick = onAdjust,
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.touchTarget(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = null,
                                tint = pal.inkMid,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Adjust Pace",
                                fontFamily = fontUi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.inkMid
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Timeline Day-Dot Grid.
 * Displays 38px circular day dots (1..durationDays) with dynamic styling
 * (completed checkmark, today glow, partial conic fill, missed red, upcoming muted,
 * difficulty indicators), status legend row, and Selected Day Preview Card with "Read Day".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineSection(
    pal: NurPalette,
    plan: ReadingPlan,
    todayStr: String,
    completedDays: Int,
    difficulty: Map<Int, com.nur.quran.shared.PlannerEngine.DifficultyIndicator>,
    selectedDay: PlannerAssignment?,
    onSelect: (PlannerAssignment?) -> Unit,
    onAdjust: () -> Unit,
    onRebalance: () -> Unit,
    onOpenDay: (String) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Column {
        // Timeline Header: Title + Counters + Rebalance / Adjust CTAs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(pal.inkMuted)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Timeline",
                    fontFamily = fontUi,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($completedDays/${plan.durationDays} done)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = pal.inkMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRebalance,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.touchTarget(44.dp)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = pal.inkMid,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rebalance",
                        fontFamily = fontUi,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.inkMid
                    )
                }
                OutlinedButton(
                    onClick = onAdjust,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.touchTarget(44.dp)
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = null,
                        tint = pal.inkMid,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Adjust",
                        fontFamily = fontUi,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.inkMid
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = pal.white,
            border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Day-Dot Grid (38px dots with 8-10dp spacing)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plan.assignments.forEach { assignment ->
                        val status = PlannerEngine.getAssignmentStatus(plan, assignment, todayStr)
                        val prog = PlannerEngine.getAssignmentProgress(plan, assignment)
                        val pct = if (prog.totalPagesCount > 0) {
                            Math.round((prog.readPagesCount.toFloat() / prog.totalPagesCount) * 100)
                        } else 0
                        val diffLevel = difficulty[assignment.dayNumber]?.level
                        val isSelected = selectedDay?.dayNumber == assignment.dayNumber

                        TimelineDot(
                            pal = pal,
                            assignment = assignment,
                            status = status,
                            pct = pct,
                            difficultyLevel = diffLevel,
                            selected = isSelected,
                            onClick = {
                                onSelect(if (isSelected) null else assignment)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(
                        "Done" to pal.gold,
                        "Today" to pal.teal,
                        "Partial" to pal.ink,
                        "Missed" to OverdueRed,
                        "Upcoming" to pal.inkMuted
                    ).forEach { (label, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 0.4.sp,
                                color = pal.inkMuted
                            )
                        }
                    }
                }

                // Selected Day Preview Card (expandable with "Read Day" button)
                AnimatedVisibility(
                    visible = selectedDay != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    selectedDay?.let { day ->
                        val sStatus = PlannerEngine.getAssignmentStatus(plan, day, todayStr)
                        val sProg = PlannerEngine.getAssignmentProgress(plan, day)
                        val sPct = if (sProg.totalPagesCount > 0) {
                            Math.round((sProg.readPagesCount.toFloat() / sProg.totalPagesCount) * 100)
                        } else 0
                        val dateLabel = PlannerEngine.formatPlannerDateLabel(day.date)
                        val statusText = when (sStatus) {
                            "completed" -> "✓ Completed"
                            "today" -> "Today's plan ($sPct%)"
                            "partial" -> "Partially finished ($sPct%)"
                            "overdue" -> "Missed completely"
                            else -> "Upcoming"
                        }
                        val unitPlural = PLANNER_UNITS[plan.unitType]?.plural?.lowercase() ?: "pages"

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = pal.cream,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Day ${day.dayNumber}: ${day.title}",
                                        fontFamily = fontUi,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "$dateLabel  ·  $statusText  ·  ${sProg.readPagesCount}/${sProg.totalPagesCount} $unitPlural",
                                        fontFamily = fontBody,
                                        fontSize = 12.sp,
                                        color = pal.inkMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Button(
                                    onClick = { onOpenDay(day.date) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.gold,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.touchTarget(44.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Read Day ${day.dayNumber}",
                                        fontFamily = fontUi,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 38px circular day dot with dynamic status styling, touch target padding,
 * difficulty dot (heavy=red, light=green), partial conic fill, and completion checkmark.
 */
@Composable
private fun TimelineDot(
    pal: NurPalette,
    assignment: PlannerAssignment,
    status: String,
    pct: Int,
    difficultyLevel: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fontBody = rememberBodyFontFamily()

    val bg = when (status) {
        "completed" -> pal.gold
        "today" -> pal.white
        "partial" -> pal.white
        "overdue" -> OverdueRed.copy(alpha = 0.12f)
        else -> pal.cream
    }

    val borderColor = when {
        selected -> pal.ink
        status == "completed" -> pal.gold
        status == "today" -> pal.gold
        status == "partial" -> pal.gold
        status == "overdue" -> OverdueRed.copy(alpha = 0.45f)
        else -> pal.boneDark
    }

    val numberColor = when (status) {
        "completed" -> Color.White
        "today" -> pal.gold
        "overdue" -> OverdueRed
        "partial" -> pal.ink
        else -> pal.inkMuted
    }

    val borderWidth = when {
        selected -> 2.5.dp
        status == "today" -> 2.dp
        else -> 1.5.dp
    }

    // Interactive touch wrapper guaranteeing accessible touch area
    Box(
        modifier = Modifier
            .size(42.dp)
            .touchTarget(42.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Visual 38px circular dot
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(bg)
                .then(
                    if (status == "partial" && !selected) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = pal.gold,
                                style = Stroke(
                                    width = borderWidth.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                                ),
                                cornerRadius = CornerRadius(size.width / 2f)
                            )
                        }
                    } else {
                        Modifier.border(
                            width = borderWidth,
                            color = borderColor,
                            shape = CircleShape
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Partial progress arc fill
            if (status != "completed" && pct > 0) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    drawArc(
                        color = pal.gold.copy(alpha = 0.22f),
                        startAngle = -90f,
                        sweepAngle = 360f * (pct / 100f),
                        useCenter = true
                    )
                }
            }

            // Day number
            Text(
                text = assignment.dayNumber.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = numberColor
            )

            // Completed checkmark
            if (status == "completed") {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-3).dp, y = (-2).dp)
                )
            }

            // Difficulty indicator dot (red for heavy, green for light)
            if (difficultyLevel != null && difficultyLevel != "moderate") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (difficultyLevel == "heavy") OverdueRed else SuccessGreen)
                        .border(1.2.dp, pal.white, CircleShape)
                )
            }
        }
    }
}

/**
 * 3. Performance Insights Card.
 * 3 metrics in a grid: On-Time Completion (%), Catch-ups Used (count), Current Pace (u/day).
 */
@Composable
private fun PerformanceSection(pal: NurPalette, onTimeRate: Int, catchUps: Int, pace: Int) {
    val fontUi = rememberUiFontFamily()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pal.white,
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = pal.gold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Performance Insights",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InsightCard(
                    pal = pal,
                    label = "ON-TIME COMPLETION",
                    value = "$onTimeRate%",
                    valueColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                InsightCard(
                    pal = pal,
                    label = "CATCH-UPS USED",
                    value = "$catchUps",
                    valueColor = pal.gold,
                    modifier = Modifier.weight(1f)
                )
                InsightCard(
                    pal = pal,
                    label = "CURRENT PACE",
                    value = "$pace",
                    unit = "u/day",
                    valueColor = pal.ink,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    pal: NurPalette,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = pal.cream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = fontUi,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = pal.inkMuted
                    )
                }
            }
        }
    }
}

/**
 * 4. Weekly Progress Section.
 * Breakdown cards for the last 4 weeks with Week label, completed/total units, percentage, and progress bar.
 */
@Composable
private fun WeeklySection(pal: NurPalette, weekly: List<WeeklySummary>) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pal.white,
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = pal.inkMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Progress",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weekly.takeLast(4).forEach { week ->
                    val pct = if (week.totalUnits > 0) {
                        ((week.completedUnits * 100) / week.totalUnits).coerceIn(0, 100)
                    } else 0
                    val isComplete = pct == 100

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = pal.cream,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = week.label,
                                fontFamily = fontUi,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${week.completedUnits} / ${week.totalUnits} ($pct%)",
                                    fontFamily = fontBody,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isComplete) SuccessGreen else pal.inkMid
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(pal.boneDark)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct / 100f)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(if (isComplete) SuccessGreen else pal.gold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Rebalance Banner / Action Required.
 * Displayed when user has fallen behind schedule.
 * Provides "Shift Deadlines (Extend Plan)" and "Redistribute Missed Pages" actions.
 */
@Composable
private fun OverdueSection(pal: NurPalette, overdueDays: Int, onExtend: () -> Unit, onSpread: () -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OverdueRed.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OverdueRed.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(OverdueRed)
                        .border(2.dp, OverdueRed.copy(alpha = 0.3f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Action Required",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdueRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You have fallen $overdueDays day${if (overdueDays > 1) "s" else ""} behind schedule.",
                fontFamily = fontBody,
                fontSize = 13.sp,
                color = pal.inkMid
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExtend,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pal.white,
                        contentColor = OverdueRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OverdueRed.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .touchTarget(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shift Deadlines (Extend Plan)",
                            fontFamily = fontUi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Button(
                    onClick = onSpread,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OverdueRed.copy(alpha = 0.12f),
                        contentColor = OverdueRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .touchTarget(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Redistribute Missed Pages",
                            fontFamily = fontUi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
