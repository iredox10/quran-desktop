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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan

/**
 * Journal tab — web Planner.jsx parity (§1235-1303).
 *
 * Web: Daily Reflections list (plannerReflections[plan.id], desc by day) +
 * Plan Highlights grid (plannerBookmarks[plan.id], jump-to-read links).
 * Desktop: reflections keyed plan.id + assignment date (same key
 * PlanNotesSection reads, so editor + list stay in sync); marks via
 * PlannerStore.getMarks. Adds date prev/next navigation over the plan's
 * assignment dates with a per-day reflection editor on top.
 */
@Composable
fun JournalTab(
    pal: NurPalette,
    plan: ReadingPlan,
    journalDate: String,
    onJournalDateChange: (String) -> Unit,
    onOpenSurah: (Int, String?) -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val dates = remember(plan.id) { plan.assignments.map { it.date }.sorted().distinct() }
    val idx = remember(dates, journalDate) { dates.indexOf(journalDate) }
    val prevDate = if (idx > 0) dates[idx - 1] else null
    val nextDate = if (idx >= 0 && idx < dates.lastIndex) dates[idx + 1] else null
    val dateLabel = remember(journalDate) {
        runCatching { PlannerEngine.formatPlannerDateLabel(journalDate) }.getOrNull() ?: journalDate
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Date navigation + day reflection editor ──
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { prevDate?.let(onJournalDateChange) }, enabled = prevDate != null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day", tint = pal.teal)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dateLabel, fontFamily = fontUi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = pal.ink)
                        Text(
                            "DAILY REFLECTION",
                            fontFamily = fontBody, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, color = pal.inkMuted
                        )
                    }
                    IconButton(onClick = { nextDate?.let(onJournalDateChange) }, enabled = nextDate != null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day", tint = pal.teal)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                key(plan.id, journalDate) {
                    JournalDayEditor(pal = pal, plan = plan, journalDate = journalDate, onSaved = onSaved)
                }
            }
        }

        // ── Plan Highlights (bookmarks with jump-to-read) ──
        JournalHighlights(pal = pal, plan = plan, onOpenSurah = onOpenSurah)

        // ── All reflections + marks (web §1246/1279 lists consolidated) ──
        PlanNotesSection(pal = pal, plan = plan, onOpenSurah = onOpenSurah)
    }
}

/** Per-day reflection editor writing PlannerStore reflections (planId|date key). */
@Composable
private fun JournalDayEditor(
    pal: NurPalette,
    plan: ReadingPlan,
    journalDate: String,
    onSaved: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var text by remember { mutableStateOf(PlannerStore.getReflection(plan.id, journalDate)) }
    var savedTick by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
    androidx.compose.material3.OutlinedTextField(
        value = text,
        onValueChange = { text = it; savedTick = false },
        placeholder = { Text("Write your reflection…", fontFamily = fontBody, color = pal.inkMuted) },
        minLines = 4,
        maxLines = 10,
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = pal.teal,
            unfocusedBorderColor = pal.boneDark,
            cursorColor = pal.teal,
            focusedTextColor = pal.ink,
            unfocusedTextColor = pal.ink
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.Button(
        onClick = {
            PlannerStore.saveReflection(plan.id, journalDate, text.trim())
            savedTick = true
            onSaved()
        },
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = pal.teal),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (savedTick) "Saved ✓" else "Save Reflection",
            fontFamily = fontUi, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = pal.white
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text(
            if (expanded) "Hide reflections" else "View all reflections",
            fontFamily = fontBody, fontSize = 12.sp,
            color = if (expanded) pal.inkMuted else pal.teal
        )
    }
    if (expanded) {
        val all = remember(plan.id, savedTick) {
            plan.assignments.mapNotNull { a ->
                val t = PlannerStore.getReflection(plan.id, a.date)
                if (t.isBlank()) null else a.date to t
            }.sortedByDescending { it.first }
        }
        if (all.isEmpty()) {
            Text(
                "No reflections yet. Complete a day to write one!",
                fontFamily = fontBody, fontStyle = FontStyle.Italic,
                fontSize = 12.5.sp, color = pal.inkMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val visible = if (showAll) all else all.take(5)
            visible.forEach { (date, t) ->
                val label = remember(date) {
                    runCatching { PlannerEngine.formatPlannerDateLabel(date) }.getOrNull() ?: date
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pal.white,
                    border = BorderStroke(1.dp, pal.boneDark),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "DAY $label · REFLECTION",
                            fontFamily = fontBody, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp, color = pal.inkMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\"$t\"", fontFamily = fontBody, fontSize = 13.sp, lineHeight = 20.sp, color = pal.ink)
                    }
                }
            }
            if (!showAll && all.size > 5) {
                TextButton(onClick = { showAll = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Show all ${all.size}", fontFamily = fontBody, fontSize = 12.sp, color = pal.teal)
                }
            }
        }
    }
    }
}

/** Planner bookmarks grid with jump-to-read, mirroring web Plan Highlights. */
@Composable
private fun JournalHighlights(
    pal: NurPalette,
    plan: ReadingPlan,
    onOpenSurah: (Int, String?) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    key(plan.id) {
        val marks = remember { PlannerStore.getMarks(plan.id).sortedByDescending { it.createdAt } }
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(1.5.dp, pal.boneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Plan Highlights", fontFamily = fontUi, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${marks.size} highlighted verse${if (marks.size == 1) "" else "s"}",
                    fontFamily = fontBody, fontSize = 11.sp, color = pal.inkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (marks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pal.white,
                        border = BorderStroke(1.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔖", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "No highlighted verses. Use the bookmark icon while reading!",
                                fontFamily = fontBody, fontSize = 12.5.sp, color = pal.inkMuted
                            )
                        }
                    }
                } else {
                    marks.forEach { mark ->
                        val chapterId = mark.verseKey.substringBefore(":").toIntOrNull()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = pal.white,
                            border = BorderStroke(1.dp, pal.boneDark),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        mark.surahName.ifBlank { "Surah" },
                                        fontFamily = fontUi, fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, color = pal.ink
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(shape = RoundedCornerShape(6.dp), color = pal.goldSoft) {
                                        Text(
                                            mark.verseKey,
                                            fontFamily = fontBody, fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, color = pal.gold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { if (chapterId != null) onOpenSurah(chapterId, mark.verseKey) },
                                    enabled = chapterId != null
                                ) {
                                    Text("Tap to read →", fontFamily = fontBody, fontSize = 12.sp, color = pal.teal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

