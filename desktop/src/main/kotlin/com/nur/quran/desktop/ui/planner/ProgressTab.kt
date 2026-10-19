package com.nur.quran.desktop.ui.planner

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlannerAssignment
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.ReadingPlan
import kotlin.math.roundToInt

private val OverdueRed = Color(0xFFDC2626)
private val SuccessGreen = Color(0xFF10B981)
/**
 * PROGRESS tab of the planner, mirroring web Planner.jsx ActiveView
 * PROGRESS tab (lines ~1065-1232): pace ring + pace stats, Timeline day-dot
 * grid (38px dots, completed/today/partial/overdue/upcoming styles,
 * partial fill arc, difficulty red/green dots, legend), selected-day
 * detail card, Performance Insights (onTimeRate, catch-ups, pace), weekly
 * summary bars (getWeeklySummary, last 4), Action Required
 * (extend vs spread) and RevisionCtaCard.
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
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var selectedDay by remember(plan.id) { mutableStateOf<PlannerAssignment?>(null) }
    var showAdjust by remember { mutableStateOf(false) }
    var showRebalance by remember { mutableStateOf(false) }
    val overview = remember(plan, todayStr) { PlannerEngine.getPlannerOverview(plan, todayStr) }
    val analytics = remember(plan, todayStr) { PlannerEngine.getPlannerAnalytics(plan, todayStr) }
    val difficulty = remember(plan) { PlannerEngine.getDifficultyIndicators(plan.assignments) }
    val weekly = remember(plan) { PlannerEngine.getWeeklySummary(plan) }
    val completedDays = overview?.completedCount ?: 0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TimelineSection(pal, plan, todayStr, completedDays, difficulty, selectedDay, { selectedDay = it }, { showAdjust = true }, { showRebalance = true })
        PerformanceSection(pal, analytics.onTimeRate, analytics.catchUpDays, analytics.avgPagesPerDay.roundToInt())
        if (weekly.isNotEmpty()) WeeklySection(pal, weekly)
        val overdueDays = overview?.overdueDays ?: 0
        if (overdueDays > 0) OverdueSection(pal, overdueDays, { onAdoptPlan(PlannerEngine.rebalancePlanner(plan, "extend")) }, { onAdoptPlan(PlannerEngine.rebalancePlanner(plan, "spread")) })
    }
    if (showAdjust) AdjustPaceDialog(pal = pal, plan = plan, onDismiss = { showAdjust = false }, onApply = { nd -> onAdoptPlan(PlannerEngine.adjustPlannerPace(plan, nd)); showAdjust = false })
    if (showRebalance) RebalanceDialog(pal = pal, onDismiss = { showRebalance = false }, onPick = { s -> onAdoptPlan(PlannerEngine.rebalancePlanner(plan, s)); showRebalance = false })
}
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
    onRebalance: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(width = 6.dp, height = 20.dp).clip(RoundedCornerShape(3.dp)).background(pal.inkMuted))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Timeline", fontFamily = fontUi, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "($completedDays/${plan.durationDays} done)", fontFamily = fontBody, fontSize = 11.sp, color = pal.inkMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRebalance, shape = RoundedCornerShape(100.dp)) { Text(text = "Rebalance", fontFamily = fontUi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.inkMid) }
                OutlinedButton(onClick = onAdjust, shape = RoundedCornerShape(100.dp)) { Text(text = "Adjust", fontFamily = fontUi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.inkMid) }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = pal.white, border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Web parity (Planner.jsx 1080-1118): flex-wrap dot grid.
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
                        TimelineDot(pal, assignment, status, pct, difficulty[assignment.dayNumber]?.level, selectedDay?.dayNumber == assignment.dayNumber) {
                            onSelect(if (selectedDay?.dayNumber == assignment.dayNumber) null else assignment)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    listOf("Done" to pal.gold, "Today" to pal.teal, "Partial" to pal.ink, "Missed" to OverdueRed, "Upcoming" to pal.inkMuted).forEach { (label, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = label, fontFamily = fontBody, fontSize = 10.sp, color = pal.inkMuted)
                        }
                    }
                }
                selectedDay?.let { day ->
                    val sStatus = PlannerEngine.getAssignmentStatus(plan, day, todayStr)
                    val sProg = PlannerEngine.getAssignmentProgress(plan, day)
                    val sPct = if (sProg.totalPagesCount > 0) Math.round((sProg.readPagesCount.toFloat() / sProg.totalPagesCount) * 100) else 0
                    val statusText = when (sStatus) { "completed" -> "\u2713 Completed"; "today" -> "Today's plan ($sPct%)"; "partial" -> "Partially finished ($sPct%)"; "overdue" -> "Missed completely"; else -> "Upcoming" }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(14.dp), color = pal.cream, border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Day ${day.dayNumber}: ${day.title}", fontFamily = fontUi, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = statusText, fontFamily = fontBody, fontSize = 12.sp, color = pal.inkMuted)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun TimelineDot(pal: NurPalette, assignment: PlannerAssignment, status: String, pct: Int, difficultyLevel: String?, selected: Boolean, onClick: () -> Unit) {
    val fontBody = rememberBodyFontFamily()
    val bg = when (status) { "completed" -> pal.gold; "today" -> pal.white; "partial" -> pal.white; "overdue" -> OverdueRed.copy(alpha = 0.10f); else -> pal.cream }
    val borderColor = when (status) { "completed" -> pal.gold; "today" -> pal.gold; "partial" -> pal.gold; "overdue" -> OverdueRed.copy(alpha = 0.4f); else -> pal.boneDark }
    val numberColor = when (status) { "completed" -> Color.White; "today" -> pal.gold; "overdue" -> OverdueRed; else -> pal.inkMuted }
    val width = if (selected) 2.5.dp else if (status == "today") 2.dp else 1.5.dp
    val base = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(bg)
        .then(
            if (status == "partial" && !selected) {
                Modifier.drawBehind {
                    drawRoundRect(
                        color = pal.gold,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = width.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(6f, 4f), 0f
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f)
                    )
                }
            } else {
                Modifier.border(width = width, color = if (selected) pal.ink else borderColor, shape = CircleShape)
            }
        )
        .clickable(onClick = onClick)
    Box(modifier = base, contentAlignment = Alignment.Center) {
        if (status != "completed" && pct > 0) {
            Canvas(modifier = Modifier.size(42.dp)) { drawArc(color = pal.gold.copy(alpha = 0.22f), startAngle = -90f, sweepAngle = 360f * (pct / 100f), useCenter = true) }
        }
        Text(text = assignment.dayNumber.toString(), fontFamily = fontBody, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = numberColor)
        if (status == "completed") Text(text = "\u2713", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.95f), modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-3).dp))
        if (difficultyLevel != null && difficultyLevel != "moderate") {
            Box(modifier = Modifier.align(Alignment.TopEnd).size(10.dp).clip(CircleShape).background(if (difficultyLevel == "heavy") OverdueRed else SuccessGreen).border(1.5.dp, pal.white, CircleShape))
        }
    }
}

@Composable
private fun PerformanceSection(pal: NurPalette, onTimeRate: Int, catchUps: Int, pace: Int) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(shape = RoundedCornerShape(16.dp), color = pal.white, border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(pal = pal, text = "Performance Insights")
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightCard(pal, "ON-TIME COMPLETION", "$onTimeRate%", SuccessGreen, Modifier.weight(1f))
                InsightCard(pal, "CATCH-UPS USED", "$catchUps", pal.gold, Modifier.weight(1f))
                InsightCard(pal, "CURRENT PACE", "$pace u/day", pal.ink, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InsightCard(pal: NurPalette, label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(shape = RoundedCornerShape(14.dp), color = pal.cream, border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontFamily = fontBody, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = pal.inkMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontFamily = fontUi, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun WeeklySection(pal: NurPalette, weekly: List<com.nur.quran.shared.WeeklySummary>) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(shape = RoundedCornerShape(16.dp), color = pal.white, border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(pal = pal, text = "Weekly Progress")
            Spacer(modifier = Modifier.height(10.dp))
            weekly.takeLast(4).forEach { week ->
                val pct = if (week.totalUnits > 0) ((week.completedUnits * 100) / week.totalUnits).coerceIn(0, 100) else 0
                val isComplete = pct == 100
                Surface(shape = RoundedCornerShape(12.dp), color = pal.cream, border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = week.label, fontFamily = fontUi, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${week.completedUnits} / ${week.totalUnits}", fontFamily = fontBody, fontSize = 12.sp, color = if (isComplete) SuccessGreen else pal.inkMid)
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.width(120.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(pal.boneDark)) {
                                Box(modifier = Modifier.fillMaxWidth(pct / 100f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(if (isComplete) SuccessGreen else pal.gold))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueSection(pal: NurPalette, overdueDays: Int, onExtend: () -> Unit, onSpread: () -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(shape = RoundedCornerShape(16.dp), color = OverdueRed.copy(alpha = 0.06f), border = androidx.compose.foundation.BorderStroke(1.dp, OverdueRed.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Action Required", fontFamily = fontUi, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OverdueRed)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "You have fallen $overdueDays day${if (overdueDays > 1) "s" else ""} behind schedule.", fontFamily = fontBody, fontSize = 13.sp, color = pal.inkMid)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onExtend, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = pal.white, contentColor = OverdueRed), modifier = Modifier.fillMaxWidth()) { Text(text = "Shift Deadlines (Extend Plan)  \u2192", fontFamily = fontUi, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSpread, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = OverdueRed.copy(alpha = 0.12f), contentColor = OverdueRed), modifier = Modifier.fillMaxWidth()) { Text(text = "Redistribute Missed Pages  \u21BB", fontFamily = fontUi, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}
