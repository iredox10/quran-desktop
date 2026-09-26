package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.shared.PlannerEngine
import kotlinx.coroutines.delay

/**
 * Desktop planner day reader mirroring Android `PlannerReaderScreen`
 * (day assignment header, assigned verses, session timer, complete-day
 * action, prev/next day navigation).
 *
 * The Android screen is keyed by `dayNumber`; the desktop entry point is
 * keyed by calendar date (`yyyy-MM-dd`), so the assignment is resolved with
 * `activePlan.assignments.find { it.date == dayDate }` — matching Android's
 * `PlannerAssignment.date` field (populated by `PlannerEngine` as
 * `getReadingDate(startDate, index, excludeDays)`).
 */
@Composable
fun PlannerReaderScreenDesktop(
    dayDate: String /* yyyy-MM-dd */,
    pal: NurPalette,
    onBack: () -> Unit = {},
    onBackToPlanner: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> },
    onOpenDay: (String) -> Unit = {}
) {
    var plan by remember(dayDate) { mutableStateOf(PlannerStore.getActivePlan()) }

    // ── Zero state: no active plan ────────────────────────────────────
    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize().background(pal.white), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active plan — create one in Planner", fontSize = 15.sp, color = pal.inkMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBackToPlanner,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
                ) {
                    Text("Back to Planner", color = pal.white)
                }
            }
        }
        return
    }

    val currentPlan = plan!!
    val assignment = remember(currentPlan, dayDate) {
        currentPlan.assignments.find { it.date == dayDate }
    }

    // ── Assignment missing for this date ─────────────────────────────
    if (assignment == null) {
        Box(modifier = Modifier.fillMaxSize().background(pal.white), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No reading assigned for $dayDate", fontSize = 15.sp, color = pal.inkMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBackToPlanner,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
                ) {
                    Text("Back to Planner", color = pal.white)
                }
            }
        }
        return
    }

    val progress = remember(currentPlan, assignment) {
        PlannerEngine.getAssignmentProgress(currentPlan, assignment)
    }
    val status = remember(currentPlan, assignment) {
        PlannerEngine.getAssignmentStatus(currentPlan, assignment)
    }
    val dateLabel = remember(dayDate) {
        try {
            PlannerEngine.formatPlannerDateLabel(dayDate)
        } catch (_: Exception) {
            dayDate
        }
    }
    val resumePage = remember(currentPlan, assignment) {
        try {
            PlannerEngine.getAssignmentResumePageNumber(currentPlan, assignment)
        } catch (_: Exception) {
            assignment.pageStart
        }
    }

    // Verses for the assignment's page range (mirrors Android's page-based reader).
    val verses = remember(assignment) {
        val out = mutableListOf<com.nur.quran.desktop.data.DeskVerse>()
        for (p in assignment.pageStart..assignment.pageEnd) {
            out += QuranStore.versesOfPage(p)
        }
        out
    }
    val firstChapterId = verses.firstOrNull()?.chapterId

    // ── Session timer (mm:ss ticking) ────────────────────────────────
    var elapsedSec by remember(dayDate) { mutableStateOf(0) }
    LaunchedEffect(dayDate) {
        while (true) {
            delay(1000)
            elapsedSec++
        }
    }
    val timerLabel = remember(elapsedSec) {
        "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
    }

    // ── Prev/next day navigation (by assignment date order) ──────────
    val sorted = remember(currentPlan) { currentPlan.assignments.sortedBy { it.dayNumber } }
    val idx = remember(sorted, assignment) { sorted.indexOfFirst { it.dayNumber == assignment.dayNumber } }
    val prevDate = if (idx > 0) sorted[idx - 1].date else null
    val nextDate = if (idx >= 0 && idx < sorted.lastIndex) sorted[idx + 1].date else null

    val listState = rememberLazyListState()
    // Jump to the resume page's first verse on load.
    LaunchedEffect(resumePage, assignment) {
        val target = verses.indexOfFirst { it.pageNumber >= resumePage }
        if (target > 0) listState.scrollToItem(target)
    }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        // ── Header ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = pal.ink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(dateLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.gold)
                Text(
                    "Day ${assignment.dayNumber} · ${assignment.title}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Text(
                    "Pages ${assignment.pageStart}–${assignment.pageEnd} · ${assignment.subtitle}",
                    fontSize = 13.sp,
                    color = pal.inkMuted
                )
            }
            // Status chip.
            Box(
                modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(pal.goldLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.gold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Timer pill.
            Box(
                modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(pal.tealSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(timerLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pal.teal)
            }
        }

        // Progress bar.
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            LinearProgressIndicator(
                progress = { progress.completionRatio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(100.dp)),
                color = pal.gold,
                trackColor = pal.bone
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${progress.completedCount}/${progress.totalCount} items · ${progress.readPagesCount}/${progress.totalPagesCount} pages",
                fontSize = 12.sp,
                color = pal.inkMuted
            )
        }

        // ── Verse list ───────────────────────────────────────────────
        var lastChapter = -1
        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
            items(verses, key = { it.verseKey }) { verse ->
                if (verse.chapterId != lastChapter) {
                    lastChapter = verse.chapterId
                    val chapterName = QuranStore.chapter(verse.chapterId)?.nameSimple ?: "Surah ${verse.chapterId}"
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(pal.cream)
                            .clickable { onOpenSurah(verse.chapterId, null) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(chapterName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.teal)
                        Text("Page ${verse.pageNumber}", fontSize = 12.sp, color = pal.inkMuted)
                    }
                }
                VerseRow(
                    verse = verse,
                    pal = pal,
                    highlighted = verse.pageNumber == resumePage &&
                        verse.verseKey == verses.firstOrNull { it.pageNumber == resumePage }?.verseKey
                )
            }
        }

        // ── Footer: prev/next day + mark complete ────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                IconButton(onClick = { prevDate?.let(onOpenDay) }, enabled = prevDate != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous day",
                        tint = if (prevDate != null) pal.ink else pal.inkMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = { nextDate?.let(onOpenDay) }, enabled = nextDate != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next day",
                        tint = if (nextDate != null) pal.ink else pal.inkMuted.copy(alpha = 0.4f)
                    )
                }
            }
            val complete = progress.isComplete ||
                currentPlan.completedDays.contains(assignment.dayNumber)
            Button(
                onClick = {
                    // Mirror Android `markAssignmentCompleted(dayNumber)` →
                    // `setPlannerAssignmentProgress(day, items.size)`: union ALL
                    // item rangeValues into completed items, clamp progress to
                    // full, stamp completion date, recompute completedDays.
                    val dayNumber = assignment.dayNumber
                    val allItems = assignment.items.map { it.rangeValue }
                    val completedAtMap = currentPlan.assignmentCompletedAt.toMutableMap()
                    if (!completedAtMap.containsKey(dayNumber)) {
                        completedAtMap[dayNumber] = PlannerEngine.formatPlannerDate()
                    }
                    val completedItemsMap = currentPlan.assignmentCompletedItems.toMutableMap()
                    completedItemsMap[dayNumber] = allItems
                    val progressMap = currentPlan.assignmentProgress.toMutableMap()
                    progressMap[dayNumber] = assignment.items.size
                    val completedDays = currentPlan.assignments
                        .filter { (completedItemsMap[it.dayNumber]?.size ?: 0) >= it.items.size && it.items.isNotEmpty() }
                        .map { it.dayNumber }
                        .sorted()
                    val updated = currentPlan.copy(
                        assignmentProgress = progressMap,
                        assignmentCompletedItems = completedItemsMap,
                        assignmentCompletedAt = completedAtMap,
                        completedDays = completedDays
                    )
                    PlannerStore.saveActivePlan(updated)
                    plan = updated
                    // Bridge study time into session logs (mirrors Android's
                    // planner→reading_sessions bridge + study-session log).
                    val elapsedMin = maxOf(1, elapsedSec / 60)
                    SessionStore.log("reading", firstChapterId, elapsedMin)
                    PlannerStore.logStudySession(dayDate, elapsedMin)
                },
                enabled = !complete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = pal.green,
                    disabledContainerColor = pal.bone
                )
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = pal.white,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (complete) "Day Complete ✓" else "Mark day complete",
                    color = pal.white,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
