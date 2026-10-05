package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.components.copyToClipboard
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan

/**
 * WEB planner extras port: revision CTA, share-progress row and success metrics.
 *
 * DEVIATION (reported): the brief first specified
 * `RevisionCtaCard(pal, plan, onAdoptRevision)` but `buildRevisionPlanner`
 * requires `chapters: List<PlanChapter>` and the card cannot map chapters
 * itself — so this ships as
 * `RevisionCtaCard(pal, plan, chapters, onAdoptRevision)`.
 */

/** Shown only when the plan window is finished; offers a half-pace revision plan. */
@Composable
fun RevisionCtaCard(
    pal: NurPalette,
    plan: ReadingPlan,
    chapters: List<PlanChapter>,
    onAdoptRevision: (ReadingPlan) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    // VERIFIED: PlannerOverview.isFinishedWindow exists (elapsedDays >= durationDays).
    val overview = remember(plan) { PlannerEngine.getPlannerOverview(plan) }
    val completedCount = overview?.completedCount ?: plan.completedDays.size
    // Fallback parity: finished window, else every day completed.
    val finished = overview?.isFinishedWindow ?: (completedCount >= plan.durationDays)
    if (!finished) return

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = pal.goldSoft),
        border = BorderStroke(1.5.dp, pal.gold),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "PLAN COMPLETE — KEEP GOING?",
                fontFamily = fontBody,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = pal.teal
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start a revision pass",
                fontFamily = fontUi,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You finished \"${plan.title}\". Adopt a half-pace revision plan over the same range to consolidate what you read.",
                fontFamily = fontBody,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = pal.inkMid
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onAdoptRevision(PlannerEngine.buildRevisionPlanner(plan, chapters)) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Adopt Revision Plan",
                    fontFamily = fontUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = pal.white,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/** Shareable one-liner plus a Copy button with transient "copied" state. */
@Composable
fun ShareProgressRow(
    pal: NurPalette,
    plan: ReadingPlan,
    completedCount: Int
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var copied by remember(plan.id, completedCount) { mutableStateOf(false) }
    val message = "I've completed $completedCount days of my ${plan.durationDays}-day plan \"${plan.title}\" on Quran Nur!"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = pal.white,
        border = BorderStroke(1.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                fontFamily = fontBody,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = pal.inkMid,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = {
                    copied = copyToClipboard(message)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, pal.gold)
            ) {
                Text(
                    text = if (copied) "Copied" else "Copy",
                    fontFamily = fontUi,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.gold
                )
            }
        }
    }
}

/** Three mini stat boxes: on-time %, consistency streak, catch-up (late) days. */
@Composable
fun SuccessMetricsRow(
    pal: NurPalette,
    plan: ReadingPlan
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    // VERIFIED: PlannerSuccessMetrics { completedCount, onTimeCount, lateCount,
    // dueCount, dueCompletedCount, successRate, consistencyStreak }.
    // Catch-ups = lateCount == PlannerAnalytics.catchUpDays (VERIFIED).
    val metrics = remember(plan) { PlannerEngine.getPlannerSuccessMetrics(plan) }
    val boxes = listOf(
        "ON-TIME" to "${metrics?.successRate ?: 0}%",
        "STREAK" to "${metrics?.consistencyStreak ?: 0} days",
        "CATCH-UPS" to "${metrics?.lateCount ?: 0} days"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        boxes.forEach { (label, value) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = pal.white,
                border = BorderStroke(1.dp, pal.boneDark),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = label,
                        fontFamily = fontBody,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = pal.inkMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        fontFamily = fontUi,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.ink
                    )
                }
            }
        }
    }
}

/** Plan reflections + highlighted verses for the active plan (journal add-on). */
@Composable
fun PlanNotesSection(
    pal: com.nur.quran.desktop.ui.theme.NurPalette,
    plan: com.nur.quran.shared.ReadingPlan,
    onOpenSurah: (Int, String?) -> Unit
) {
    val reflections = remember(plan.id) {
        plan.assignments.mapNotNull { a ->
            val text = com.nur.quran.desktop.data.PlannerStore.getReflection(plan.id, a.date)
            if (text.isBlank()) null else a.date to text
        }
    }
    val marks = remember(plan.id) {
        com.nur.quran.desktop.data.PlannerStore.getMarks(plan.id)
    }
    if (reflections.isEmpty() && marks.isEmpty()) return
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.dp, pal.boneDark),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            Text(
                text = "Plan notes",
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = pal.ink,
                fontFamily = com.nur.quran.desktop.ui.theme.rememberUiFontFamily()
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            reflections.forEach { (date, text) ->
                Text(
                    text = "\"$text\"",
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = pal.inkMid,
                    fontFamily = com.nur.quran.desktop.ui.theme.rememberBodyFontFamily()
                )
                Text(
                    text = "— day $date",
                    fontSize = 11.sp,
                    color = pal.inkMuted
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
            }
            marks.forEach { mark ->
                val chapterId = mark.verseKey.substringBefore(":").toIntOrNull()
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (chapterId != null) onOpenSurah(chapterId, mark.verseKey)
                    }
                ) {
                    Text(
                        text = "🔖 ${mark.surahName} ${mark.verseKey} — tap to read →",
                        fontSize = 13.sp,
                        color = pal.gold
                    )
                }
            }
        }
    }
}
