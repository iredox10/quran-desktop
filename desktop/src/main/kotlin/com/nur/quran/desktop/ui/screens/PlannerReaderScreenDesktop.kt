package com.nur.quran.desktop.ui.screens

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.PageWord
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.WordStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.input.AppShortcuts
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.input.swipeToNavigate
import com.nur.quran.desktop.ui.components.MushafPageLines
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.components.WordRef
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.shared.PlannerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Shared per-item completion (web markPlannerItemComplete): union the
    // rangeValue into assignmentCompletedItems, bump assignmentProgress,
    // stamp assignmentCompletedAt when the day becomes complete, recompute
    // completedDays. Used by the per-item button AND page-flip auto-complete.
    fun markItemComplete(rangeValue: String) {
        if (progress.completedRangeValues.contains(rangeValue)) return
        val dayNumber = assignment.dayNumber
        val completedItemsMap = currentPlan.assignmentCompletedItems.toMutableMap()
        val cur = completedItemsMap[dayNumber].orEmpty().toMutableSet()
        cur.add(rangeValue)
        completedItemsMap[dayNumber] = cur.toList()
        val progressMap = currentPlan.assignmentProgress.toMutableMap()
        progressMap[dayNumber] = assignment.items.count { cur.contains(it.rangeValue) }
        val completedAtMap = currentPlan.assignmentCompletedAt.toMutableMap()
        val nowComplete = assignment.items.all { cur.contains(it.rangeValue) }
        if (nowComplete && !completedAtMap.containsKey(dayNumber)) {
            completedAtMap[dayNumber] = PlannerEngine.formatPlannerDate()
        }
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
    }
    val resumePage = remember(currentPlan, assignment) {
        // Web parity (PlannerReader lines 101-107): honor planner.lastReadPage
        // when it lands inside this assignment, else the resume helper.
        val lastRead = currentPlan.lastReadPage
        if (lastRead != null && lastRead >= assignment.pageStart && lastRead <= assignment.pageEnd) {
            lastRead
        } else {
            try {
                PlannerEngine.getAssignmentResumePageNumber(currentPlan, assignment)
            } catch (_: Exception) {
                assignment.pageStart
            }
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
    var isTimerRunning by remember(dayDate) { mutableStateOf(true) }
    // Test-clock-safe ticking: an infinite `delay(1000)` loop never lets the
    // compose test clock go idle (it hangs any waitForIdle / captureToImage
    // on this screen); withInfiniteAnimationFrameNanos is the documented
    // replacement and still accrues one second per second in production.
    LaunchedEffect(dayDate, isTimerRunning) {
        if (!isTimerRunning) return@LaunchedEffect
        var lastTick = withInfiniteAnimationFrameNanos { it }
        while (isTimerRunning) {
            withInfiniteAnimationFrameNanos { now ->
                if (now - lastTick >= 1_000_000_000L) {
                    lastTick = now
                    elapsedSec++
                }
            }
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

    val scope = rememberCoroutineScope()
    // In-day page pager state, clamped to the assignment range.
    var currentPage by remember(assignment) {
        mutableStateOf(resumePage.coerceIn(assignment.pageStart, assignment.pageEnd))
    }
    LaunchedEffect(resumePage) {
        currentPage = resumePage.coerceIn(assignment.pageStart, assignment.pageEnd)
    }
    fun scrollToPage(page: Int) {
        scope.launch {
            val target = verses.indexOfFirst { it.pageNumber >= page }
            if (target >= 0) listState.scrollToItem(target)
        }
    }
    // Reading mode: page-accurate 15-line mushaf vs verse list
    // (web `readingMode` toggle parity).
    var showMushaf by remember(dayDate) { mutableStateOf(false) }

    // Page-read tracking: mark ONLY the page the reader is currently on (web
    // parity — markPlannerPageRead(currentDay, pageNumber) on page change) and
    // persist lastReadPage for the next Resume jump.
    LaunchedEffect(currentPage, assignment) {
        val dayNumber = assignment.dayNumber
        val existingPages = currentPlan.assignmentReadPages[dayNumber].orEmpty().toSet()
        var base = currentPlan
        if (!existingPages.contains(currentPage)) {
            val readMap = currentPlan.assignmentReadPages.toMutableMap()
            readMap[dayNumber] = (existingPages + currentPage).sorted()
            base = currentPlan.copy(assignmentReadPages = readMap)
        }
        val updated = if (base.lastReadPage != currentPage) base.copy(lastReadPage = currentPage) else base
        if (updated !== currentPlan) {
            PlannerStore.saveActivePlan(updated)
            plan = updated
        }
    }

    // Day reflection — web parity: addPlannerReflection(assignment.date) →
    // plannerReflections[plan.id] (the key JournalTab / PlanNotesSection read).
    val reflectionKey = remember(currentPlan.id, dayDate) { "${currentPlan.id}|$dayDate" }
    var reflection by remember(reflectionKey) {
        mutableStateOf(PlannerStore.getReflection(currentPlan.id, dayDate))
    }
    var reflectionSaved by remember(reflectionKey) { mutableStateOf(false) }

    val isDayComplete = progress.isComplete ||
        currentPlan.completedDays.contains(assignment.dayNumber)

    // Desktop keyboard nav (web lines 565-572): ArrowRight = prev page,
    // ArrowLeft = next page — bounded to the assignment range.
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    // Day checklist collapsed by default so the actual Quran text is what the
    // reader sees first (web reader leads with verses, not the item list).
    var showDayItems by remember { mutableStateOf(false) }
    // Swipe left/right turns pages on touchscreens (web useSwipeable parity).
    fun goNextPage(): Boolean {
        if (currentPage < assignment.pageEnd) {
            currentPage = currentPage + 1
            scrollToPage(currentPage)
        }
        return true
    }
    fun goPrevPage(): Boolean {
        if (currentPage > assignment.pageStart) {
            currentPage = currentPage - 1
            scrollToPage(currentPage)
        }
        return true
    }
    Column(
        modifier = Modifier.fillMaxSize().background(pal.white)
            .focusRequester(focusRequester)
            .focusable()
            .swipeToNavigate(onNext = { goNextPage() }, onPrevious = { goPrevPage() })
            .onKeyEvent { event ->
                if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                    return@onKeyEvent false
                }
                // Full reader map: arrows + Space/PgDn/PgUp page turns,
                // Home/End jump to assignment bounds, Esc goes back.
                AppShortcuts.pageTurn(event, onNext = ::goNextPage, onPrevious = ::goPrevPage) ||
                    when (event.key) {
                        androidx.compose.ui.input.key.Key.MoveHome -> {
                            currentPage = assignment.pageStart
                            scrollToPage(currentPage)
                            true
                        }
                        androidx.compose.ui.input.key.Key.MoveEnd -> {
                            currentPage = assignment.pageEnd
                            scrollToPage(currentPage)
                            true
                        }
                        androidx.compose.ui.input.key.Key.Escape -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
            }
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        // ── Header ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.touchTarget()) {
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
            // Current-chapter pill (web line 640-646).
            val currentChapter = remember(currentPage) {
                QuranStore.chapters.firstOrNull { ch ->
                    ch.pages.isNotEmpty() &&
                        currentPage >= ch.pages.first() &&
                        currentPage <= ch.pages.last()
                }
            }
            if (currentChapter != null) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(100.dp))
                        .background(pal.tealSoft)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${currentChapter.id}. ${currentChapter.nameSimple}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.teal
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Status chip.
            Box(
                modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(pal.goldLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.gold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Timer pill — tap to pause/resume (web isTimerRunning toggle).
            Row(
                modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(pal.tealSoft)
                    .clickable { isTimerRunning = !isTimerRunning }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isTimerRunning) "Pause session timer" else "Resume session timer",
                    tint = pal.teal,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(timerLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pal.teal)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // "X% Achieved" (web line 661-663).
            Text(
                "${(progress.completionRatio * 100f + 0.5f).toInt()}% Achieved",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = pal.teal
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Page-audio play pill (web handlePlayClick parity): plays the
            // current page's assigned verses; tap again to pause/resume.
            val pageVerses = remember(currentPage) { verses.filter { it.pageNumber == currentPage } }
            val pageAudioActive = AudioEngine.current != null &&
                pageVerses.any { it.verseKey == AudioEngine.current?.verseKey }
            IconButton(
                onClick = {
                    if (pageAudioActive) {
                        AudioEngine.togglePlayPause()
                    } else if (pageVerses.isNotEmpty()) {
                        AudioEngine.playTracks(
                            pageVerses.map { AudioEngine.Track(it.verseKey, it.chapterId, it.verseNumber) }
                        )
                    }
                },
                enabled = pageVerses.isNotEmpty()
            ) {
                Icon(
                    if (pageAudioActive && AudioEngine.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play page audio",
                    tint = pal.teal
                )
            }
            // Reading-mode toggle: page-accurate mushaf lines vs verse list.
            IconButton(onClick = { showMushaf = !showMushaf }, modifier = Modifier.touchTarget()) {
                Icon(
                    Icons.Filled.MenuBook,
                    contentDescription = if (showMushaf) "Verse list view" else "Mushaf page view",
                    tint = if (showMushaf) pal.gold else pal.teal
                )
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

        // ── Day checklist (collapsed by default; tap header to expand) ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.cream)
                .clickable { showDayItems = !showDayItems }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Day checklist · ${progress.completedCount}/${progress.totalCount} done",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (showDayItems) "Hide" else "Show",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = pal.teal
            )
        }
        if (showDayItems) Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            assignment.items.forEach { item ->
                val done = progress.completedRangeValues.contains(item.rangeValue)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (done) pal.tealSoft else pal.cream)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (done) Icons.Filled.CheckCircle else Icons.Filled.Check,
                        contentDescription = if (done) "Done" else "Not done",
                        tint = if (done) pal.green else pal.inkMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.ink)
                        Text(
                            "Pages ${item.pageStart}–${item.pageEnd}",
                            fontSize = 12.sp,
                            color = pal.inkMuted
                        )
                    }
                    if (done) {
                        Text("Done ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.green)
                    } else {
                        Button(
                            onClick = { markItemComplete(item.rangeValue) },
                            colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
                        ) {
                            Text("Mark Done", color = pal.white, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── In-day page pager (clamped to [pageStart, pageEnd]) ─────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    val target = (currentPage - 1).coerceAtLeast(assignment.pageStart)
                    currentPage = target
                    scrollToPage(target)
                },
                enabled = currentPage > assignment.pageStart,
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
            ) {
                Text("‹ Prev page", color = pal.white, fontSize = 12.sp)
            }
            Text(
                "Page $currentPage of ${assignment.pageEnd}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.inkMuted
            )
            Button(
                onClick = {
                    // Web handleNextPage parity: auto-complete the item being
                    // left when its final page is passed (page-unit plans mark
                    // on every page).
                    val outgoing = assignment.items.find {
                        currentPage >= it.pageStart && currentPage <= it.pageEnd
                    }
                    if (outgoing != null &&
                        !progress.completedRangeValues.contains(outgoing.rangeValue) &&
                        (assignment.unitType == "page" || currentPage >= outgoing.pageEnd)
                    ) {
                        markItemComplete(outgoing.rangeValue)
                    }
                    val target = (currentPage + 1).coerceAtMost(assignment.pageEnd)
                    currentPage = target
                    scrollToPage(target)
                },
                enabled = currentPage < assignment.pageEnd,
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
            ) {
                Text("Next page ›", color = pal.white, fontSize = 12.sp)
            }
        }

        // ── Success card: Alhamdulillah + insight verse + reflection ──
        if (isDayComplete) {
            val insightVerse = remember(assignment.dayNumber, isDayComplete) {
                if (isDayComplete && verses.isNotEmpty()) verses.random() else null
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.teal)
                    .padding(20.dp)
            ) {
                Text(
                    "Alhamdulillah! 🎉",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.white
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "You have completed Day ${assignment.dayNumber}.",
                    fontSize = 13.sp,
                    color = pal.white.copy(alpha = 0.9f)
                )
                if (insightVerse != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.12f))
                            .padding(14.dp)
                    ) {
                        Text(
                            "TAKEAWAY OF THE DAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = pal.white.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            insightVerse.arabic,
                            fontSize = 19.sp,
                            color = pal.white,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            insightVerse.translation,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = pal.white.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "— Surah ${insightVerse.verseKey.replace(":", ", Ayah ")}",
                            fontSize = 11.sp,
                            color = pal.white.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Daily Reflection (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.white.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it; reflectionSaved = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                    placeholder = {
                        Text(
                            "Write a brief reflection for today's reading…",
                            color = pal.inkMuted
                        )
                    },
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pal.white,
                        unfocusedBorderColor = pal.white.copy(alpha = 0.4f),
                        focusedTextColor = pal.white,
                        unfocusedTextColor = pal.white,
                        cursorColor = pal.white
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            PlannerStore.saveReflection(currentPlan.id, dayDate, reflection.trim())
                            reflectionSaved = true
                            onBackToPlanner()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.white,
                            contentColor = pal.teal
                        )
                    ) {
                        Text(
                            if (reflection.trim().isNotEmpty()) "Save & Return"
                            else "Return to Planner",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (reflectionSaved) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saved ✓", fontSize = 12.sp, color = pal.white)
                    }
                }
            }
        }

        if (!showMushaf) {
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
                    highlighted = (verse.pageNumber == resumePage &&
                        verse.verseKey == verses.firstOrNull { it.pageNumber == resumePage }?.verseKey) ||
                        AudioEngine.current?.verseKey == verse.verseKey,
                    onPlayVerse = { vk -> AudioEngine.playVerse(vk) }
                )
            }
        }
        } else {
            // ── Page-accurate 15-line mushaf view (web readingMode parity) ──
            var pageWords by remember { mutableStateOf<List<PageWord>?>(null) }
            LaunchedEffect(currentPage) {
                pageWords = null
                pageWords = withContext(Dispatchers.IO) { WordStore.getPageWords(currentPage) }
            }
            val loaded = pageWords
            when {
                loaded == null -> Text(
                    text = "Loading…",
                    fontSize = 14.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.weight(1f).padding(16.dp)
                )
                loaded.isEmpty() -> Text(
                    text = "Offline — page words unavailable",
                    fontSize = 14.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.weight(1f).padding(16.dp)
                )
                else -> {
                    val wordsByLine = remember(loaded) {
                        loaded.filter { it.lineNumber > 0 }
                            .groupBy({ it.lineNumber }, { WordRef(it.text) })
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MushafPageLines(wordsByLine = wordsByLine)
                    }
                }
            }
        }

        // ── Footer: prev/next day + mark complete ────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                IconButton(onClick = { prevDate?.let(onOpenDay) }, enabled = prevDate != null, modifier = Modifier.touchTarget()) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous day",
                        tint = if (prevDate != null) pal.ink else pal.inkMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = { nextDate?.let(onOpenDay) }, enabled = nextDate != null, modifier = Modifier.touchTarget()) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next day",
                        tint = if (nextDate != null) pal.ink else pal.inkMuted.copy(alpha = 0.4f)
                    )
                }
            }
            val complete = isDayComplete
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
