package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.shared.PlannerEngine

/**
 * Planner history sheet, ported from the web `PlannerHistory.jsx`: completion
 * summary, All/Completed filter, per-plan days/pages/reading-time, and an
 * expandable detail with reflections and bookmark count.
 */
@Composable
fun PlannerHistoryModal(
    pal: NurPalette,
    summaries: List<PlannerHistory.PlanSummary>,
    onDismiss: () -> Unit
) {
    val fontBody = rememberBodyFontFamily()
    var completedOnly by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    val visible = remember(summaries, completedOnly) {
        PlannerHistory.filterCompleted(summaries, completedOnly)
    }
    val completedCount = summaries.count { it.isComplete }

    ModalShell(pal = pal, onDismiss = onDismiss, title = "Plan History") {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(scrollState)
                .dragToScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$completedCount of ${summaries.size} plans completed",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = pal.inkMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryFilterChip("All", selected = !completedOnly, pal = pal) {
                    completedOnly = false
                }
                HistoryFilterChip("Completed", selected = completedOnly, pal = pal) {
                    completedOnly = true
                }
            }

            if (visible.isEmpty()) {
                Text(
                    text = if (completedOnly) {
                        "No completed plans yet."
                    } else {
                        "Finish or archive a plan and it will show up here."
                    },
                    fontFamily = fontBody,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }

            visible.forEach { summary ->
                HistoryPlanCard(
                    summary = summary,
                    expanded = expandedId == summary.plan.id,
                    onToggle = {
                        expandedId = if (expandedId == summary.plan.id) null else summary.plan.id
                    },
                    pal = pal,
                    fontBody = fontBody
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    pal: NurPalette,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) pal.teal else pal.white,
        border = BorderStroke(1.dp, if (selected) pal.teal else pal.boneDark),
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).touchTarget().clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            color = if (selected) pal.white else pal.inkMid,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HistoryPlanCard(
    summary: PlannerHistory.PlanSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    pal: NurPalette,
    fontBody: androidx.compose.ui.text.font.FontFamily
) {
    val plan = summary.plan
    val mono: androidx.compose.ui.text.font.FontFamily = FontFamily.Monospace
    val finishedLabel = summary.completedOn?.let {
        runCatching { PlannerEngine.formatPlannerDateLabel(it) }.getOrNull()
    }
    val archivedLabel = summary.archivedAt?.let {
        runCatching { PlannerEngine.formatPlannerDateLabel(it.substringBefore("T")) }.getOrNull()
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.bone),
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title.ifBlank { "Plan" },
                    fontFamily = fontBody,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (summary.isComplete) pal.goldSoft else pal.cream
                ) {
                    Text(
                        text = if (summary.isComplete) "COMPLETED" else "ENDED",
                        fontFamily = mono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                        color = if (summary.isComplete) pal.gold else pal.inkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${summary.completedDays} of ${summary.totalDays} days \u00B7 " +
                    "${summary.pagesRead} of ${summary.totalPages} pages",
                fontFamily = mono,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
                color = pal.inkMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = PlannerHistory.formatTime(summary.readingSeconds),
                fontFamily = fontBody,
                fontSize = 12.sp,
                color = pal.inkMid
            )
            val footer = when {
                archivedLabel != null -> "Archived on $archivedLabel"
                summary.isComplete && finishedLabel != null -> "Finished on $finishedLabel"
                else -> "In progress \u00B7 tap for details"
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = footer,
                fontFamily = mono,
                fontSize = 9.sp,
                letterSpacing = 0.4.sp,
                color = pal.inkMuted
            )

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(pal.white, RoundedCornerShape(10.dp))
                        .border(1.dp, pal.boneDark, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Started ${plan.startDate} \u00B7 ${plan.durationDays} days",
                        fontFamily = mono,
                        fontSize = 10.sp,
                        color = pal.inkMid
                    )
                    Text(
                        text = "${summary.bookmarkCount} highlighted verse" +
                            if (summary.bookmarkCount == 1) "" else "s",
                        fontFamily = mono,
                        fontSize = 10.sp,
                        color = pal.inkMid
                    )
                    if (summary.reflections.isEmpty()) {
                        Text(
                            text = "No reflections recorded.",
                            fontFamily = fontBody,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = pal.inkMuted
                        )
                    } else {
                        summary.reflections.forEach { entry ->
                            Text(
                                text = "Day ${entry.dayNumber} \u00B7 ${entry.dayTitle}",
                                fontFamily = mono,
                                fontSize = 9.sp,
                                letterSpacing = 0.4.sp,
                                color = pal.teal
                            )
                            Text(
                                text = entry.text,
                                fontFamily = fontBody,
                                fontSize = 12.sp,
                                color = pal.ink
                            )
                        }
                    }
                }
            }
        }
    }
}
