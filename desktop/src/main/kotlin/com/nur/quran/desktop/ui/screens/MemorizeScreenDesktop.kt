package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.ChapterJson
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.hifdh.HifdhBreakdownDialog
import com.nur.quran.desktop.ui.hifdh.HifdhGoalDialog
import com.nur.quran.desktop.ui.hifdh.HifdhTestDialog
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.HifdhQueue
import com.nur.quran.shared.HifdhQueues
import com.nur.quran.shared.HifdhStore
import com.nur.quran.shared.JUZ_STARTS
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Desktop memorization hub mirroring web `MemorizeIndex.jsx` with full parity:
 * - Prominent "MEMORIZATION HUB" title with due badge
 * - Gradient Continue Card with chapter info, last-practiced timing, and audio playback
 * - 3-card stat strip (Surahs / 114, Ayahs / 6236, Progress %)
 * - 2 large action buttons ("Test My Hifdh" & "Set Goal")
 * - 3 colored Spaced Repetition queue cards (Sabaq, Sabqi, Manzil) with direct review buttons
 * - Active Goals section with progress rings and resume ayah indicators
 * - Surahs vs Ajzaa (Juz) view mode toggle
 * - Search bar and "Filter Memorized" toggle
 * - Responsive 2-column card grid with chapter audio playback
 * - Memorized Surahs & Memorized Ayahs modals
 */
@Composable
fun MemorizeScreenDesktop(
    pal: NurPalette,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> },
    onOpenHifdhReader: (Int) -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()

    // State tick for cache busting after hifdh store writes
    var tick by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyMemorized by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("surah") } // "surah" | "juz"

    // Dialogs
    var showGoalDialog by remember { mutableStateOf(false) }
    var showSurahsDialog by remember { mutableStateOf(false) }
    var showAyahsDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testQueue by remember { mutableStateOf<List<String>?>(null) }
    var testQueueTitle by remember { mutableStateOf<String?>(null) }
    var testChapterId by remember { mutableStateOf(114) }
    var breakdownFor by remember { mutableStateOf<Int?>(null) }

    val nowMs = remember(tick) { System.currentTimeMillis() }
    val history = remember(tick) { HifdhStore.loadHifdhHistory() }
    val goals = remember(tick) { HifdhStore.loadHifdhGoals() }
    val chapters = remember { QuranStore.chapters }

    val totalMemorized = history.size
    val memBySurah = remember(history) {
        history.keys.groupingBy { it.substringBefore(":").toIntOrNull() }.eachCount()
    }
    val memorizedSurahCount = remember(memBySurah) {
        memBySurah.count { (surahId, mem) ->
            val total = surahId?.let { QuranStore.chapter(it)?.versesCount } ?: 0
            total > 0 && mem >= total
        }
    }
    val goalPct = ((totalMemorized * 100f) / 6236f).toInt().coerceIn(0, 100)

    val fullyMemorized = remember(memBySurah) {
        memBySurah.mapNotNull { (surahId, mem) ->
            val id = surahId ?: return@mapNotNull null
            val ch = QuranStore.chapter(id) ?: return@mapNotNull null
            if (ch.versesCount > 0 && mem >= ch.versesCount) ch else null
        }.sortedBy { it.id }
    }

    val ayahsBySurah = remember(history) {
        history.keys.mapNotNull { key ->
            val parts = key.split(":")
            val s = parts.getOrNull(0)?.toIntOrNull()
            val v = parts.getOrNull(1)?.toIntOrNull()
            if (s == null || v == null) null else s to v
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, verses) -> verses.distinct().sorted() }
            .toSortedMap()
    }

    // Most recent memorizing session
    val lastSession = remember(tick) {
        SessionStore.all()
            .filter { s ->
                s.type == "memorizing" && s.chapterId != null &&
                    QuranStore.chapter(s.chapterId) != null
            }
            .lastOrNull()
    }
    val lastSessionChapter = remember(lastSession) {
        lastSession?.chapterId?.let { QuranStore.chapter(it) }
    }
    val lastSessionAgo = remember(lastSession) {
        if (lastSession == null) "" else {
            val day = runCatching { LocalDate.parse(lastSession.date) }.getOrNull()
            val today = LocalDate.now()
            val days = if (day == null) 0 else ChronoUnit.DAYS.between(day, today).coerceAtLeast(0)
            if (days <= 0) "0m ago" else "${days}d ago"
        }
    }

    // Spaced Repetition queue split
    val queueSplit = remember(history, lastSession) {
        HifdhQueues.splitQueue(history, lastSession?.chapterId)
    }
    val sabaqTotal = queueSplit[HifdhQueue.SABAQ].orEmpty()
    val sabqiTotal = queueSplit[HifdhQueue.SABQI].orEmpty()
    val manzilTotal = queueSplit[HifdhQueue.MANZIL].orEmpty()

    val dueSabaq = remember(sabaqTotal, history, nowMs) {
        HifdhQueues.dueInQueue(sabaqTotal, history, nowMs)
    }
    val dueSabqi = remember(sabqiTotal, history, nowMs) {
        HifdhQueues.dueInQueue(sabqiTotal, history, nowMs)
    }
    val dueManzil = remember(manzilTotal, history, nowMs) {
        HifdhQueues.dueInQueue(manzilTotal, history, nowMs)
    }
    val allDue = remember(history, nowMs) {
        history.keys.filter { key ->
            val entry = history[key]
            val card = entry?.card
            entry == null || card == null || card.isDue(nowMs)
        }
    }

    fun mostDueIn(keys: List<String>): Int? =
        keys.groupingBy { it.substringBefore(":").toIntOrNull() }
            .eachCount().maxByOrNull { it.value }?.key

    val mostDueSurahId = remember(history, nowMs) {
        history.keys
            .filter { key -> val e = history[key]; e?.card?.isDue(nowMs) ?: true }
            .groupingBy { it.substringBefore(":").toIntOrNull() }
            .eachCount()
            .maxByOrNull { it.value }?.key
    }

    val totalDueCount = allDue.size

    // Juz verse counts and memorized verse counts
    val totalJuzVerses = remember { (1..30).associateWith { QuranStore.versesOfJuz(it).size } }
    val memJuzVerses = remember(history) {
        (1..30).associateWith { j ->
            QuranStore.versesOfJuz(j).count { v -> history.containsKey(v.verseKey) }
        }
    }

    // Filtered chapters for surah mode
    val filteredChapters = remember(chapters, searchQuery, showOnlyMemorized, memBySurah) {
        val q = searchQuery.trim().lowercase()
        chapters.filter { ch ->
            val matchesQuery = q.isEmpty() ||
                ch.nameSimple.lowercase().contains(q) ||
                ch.nameArabic.contains(q) ||
                ch.id.toString() == q ||
                ch.translatedNameText.lowercase().contains(q)
            val memCount = memBySurah[ch.id] ?: 0
            val isMem = ch.versesCount > 0 && memCount >= ch.versesCount
            val matchesFilter = !showOnlyMemorized || isMem
            matchesQuery && matchesFilter
        }
    }

    // Filtered ajzaa for juz mode
    val filteredAjzaa = remember((1..30).toList(), showOnlyMemorized, memJuzVerses, totalJuzVerses) {
        (1..30).filter { j ->
            val total = totalJuzVerses[j] ?: 0
            val mem = memJuzVerses[j] ?: 0
            val isMem = total > 0 && mem >= total
            !showOnlyMemorized || isMem
        }
    }

    val chunkedSurahs = remember(filteredChapters) { filteredChapters.chunked(2) }
    val chunkedAjzaa = remember(filteredAjzaa) { filteredAjzaa.chunked(2) }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        // ── Top Navigation Bar ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.touchTarget().size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = pal.ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MEMORIZATION HUB",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = pal.inkMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Hifdh Tracker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (totalDueCount > 0) pal.goldSoft else pal.bone)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (totalDueCount > 0) "$totalDueCount due" else "All caught up",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalDueCount > 0) pal.gold else pal.inkMuted,
                    fontFamily = fontBody
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val memorizeListState = rememberLazyListState()
        LazyColumn(
            state = memorizeListState,
            modifier = Modifier.weight(1f).fillMaxWidth().dragToScroll(memorizeListState),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Hero Continue Card ──
            if (lastSessionChapter != null) {
                item(key = "continue_hero") {
                    val isThisChapterAudio = AudioEngine.current?.chapterId == lastSessionChapter.id
                    val isAudioPlayingThisChapter = isThisChapterAudio && AudioEngine.playing

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2E4F4A), Color(0xFF3B6760))
                                )
                            )
                            .clickable { onOpenHifdhReader(lastSessionChapter.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Continue: ${lastSessionChapter.nameSimple}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = fontUi
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Last practiced $lastSessionAgo",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .clickable {
                                        if (isThisChapterAudio) {
                                            AudioEngine.togglePlayPause()
                                        } else {
                                            AudioEngine.playChapter(lastSessionChapter.id)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlayingThisChapter) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play Chapter Audio",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 2: Top Stat Strip (Surahs, Ayahs, Progress) ──
            item(key = "stat_strip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Surahs Card
                    StatBox(
                        value = "$memorizedSurahCount",
                        sub = "/114",
                        label = "SURAHS",
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f),
                        onClick = { showSurahsDialog = true }
                    )
                    // Ayahs Card
                    StatBox(
                        value = "$totalMemorized",
                        sub = "/6236",
                        label = "AYAHS",
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f),
                        onClick = { showAyahsDialog = true }
                    )
                    // Progress Card
                    StatBox(
                        value = "$goalPct",
                        sub = "%",
                        label = "PROGRESS",
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f),
                        onClick = null
                    )
                }
            }

            // ── Section 3: Two Big Action Cards (Test My Hifdh & Set Goal) ──
            item(key = "big_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Test My Hifdh Card
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = pal.teal,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable {
                                testQueue = if (allDue.isNotEmpty()) allDue else null
                                testQueueTitle = "All Due Ayahs (${allDue.size})"
                                testChapterId = mostDueSurahId ?: 114
                                showTestDialog = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Test My Hifdh",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = fontUi
                            )
                            Text(
                                text = "REVIEW MISTAKES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Set Goal Card
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = pal.cream,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { showGoalDialog = true }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(pal.bone),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = pal.inkMid,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Set Goal",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink,
                                fontFamily = fontUi
                            )
                            Text(
                                text = "PLAN MEMORIZATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = pal.inkMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ── Section 4: Spaced Repetition Queue Cards (Sabaq, Sabqi, Manzil) ──
            item(key = "queues_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sabaq (New)
                    QueueCard(
                        title = "Sabaq (New)",
                        icon = Icons.Filled.Schedule,
                        dueCount = dueSabaq.size,
                        totalCount = sabaqTotal.size,
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        headerColor = Color(0xFF166534),
                        subColor = Color(0xFF15803D),
                        btnTextColor = Color(0xFF166534),
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f),
                        onReview = {
                            testQueue = if (dueSabaq.isNotEmpty()) dueSabaq else sabaqTotal
                            testQueueTitle = "Sabaq Review (${dueSabaq.size} due)"
                            testChapterId = mostDueIn(dueSabaq.ifEmpty { sabaqTotal }) ?: 114
                            showTestDialog = true
                        }
                    )

                    // Sabqi (Recent)
                    QueueCard(
                        title = "Sabqi (Recent)",
                        icon = Icons.Filled.Bookmark,
                        dueCount = dueSabqi.size,
                        totalCount = sabqiTotal.size,
                        bgColor = Color(0xFFFEFCE8),
                        borderColor = Color(0xFFFEF08A),
                        headerColor = Color(0xFF854D0E),
                        subColor = Color(0xFFA16207),
                        btnTextColor = Color(0xFF854D0E),
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f),
                        onReview = {
                            testQueue = if (dueSabqi.isNotEmpty()) dueSabqi else sabqiTotal
                            testQueueTitle = "Sabqi Review (${dueSabqi.size} due)"
                            testChapterId = mostDueIn(dueSabqi.ifEmpty { sabqiTotal }) ?: 114
                            showTestDialog = true
                        }
                    )

                    // Manzil (Old)
                    QueueCard(
                        title = "Manzil (Old)",
                        icon = Icons.Filled.LocalLibrary,
                        dueCount = dueManzil.size,
                        totalCount = manzilTotal.size,
                        bgColor = Color(0xFFF0F9FF),
                        borderColor = Color(0xFFBAE6FD),
                        headerColor = Color(0xFF075985),
                        subColor = Color(0xFF0369A1),
                        btnTextColor = Color(0xFF075985),
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f),
                        onReview = {
                            testQueue = if (dueManzil.isNotEmpty()) dueManzil else manzilTotal
                            testQueueTitle = "Manzil Review (${dueManzil.size} due)"
                            testChapterId = mostDueIn(dueManzil.ifEmpty { manzilTotal }) ?: 114
                            showTestDialog = true
                        }
                    )
                }
            }

            // ── Section 5: Active Goals (if goals exist) ──
            if (goals.isNotEmpty()) {
                item(key = "active_goals_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrackChanges,
                            contentDescription = null,
                            tint = pal.gold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active Goals",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi
                        )
                    }
                }

                items(goals, key = { it.id }) { goal ->
                    val chapter = QuranStore.chapter(goal.targetId)
                    val curMem = memBySurah[goal.targetId] ?: 0
                    val total = chapter?.versesCount ?: 0
                    val denom = if (total > 0) total else 1
                    val pct = (curMem * 100 / denom).coerceIn(0, 100)
                    val daysLeft = goal.daysLeft
                    val resumeAyah = (1..total).firstOrNull { ayah ->
                        !history.containsKey("${goal.targetId}:$ayah")
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pal.cream,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenHifdhReader(goal.targetId) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gold accent line on left
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .fillMaxHeight()
                                    .background(pal.gold)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Memorize ${chapter?.nameSimple ?: "Target"}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = null,
                                            tint = pal.inkMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (daysLeft > 0) "$daysLeft days left" else "Due now",
                                            fontSize = 12.sp,
                                            color = if (daysLeft <= 0 && pct < 100) Color(0xFFEF4444) else pal.inkMuted,
                                            fontFamily = fontBody
                                        )
                                    }
                                    if (resumeAyah != null && pct < 100) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = pal.gold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Continue from Ayah $resumeAyah",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.gold,
                                                fontFamily = fontBody
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$curMem / $total",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    )
                                    Text(
                                        text = "AYAHS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = pal.inkMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                // Circular Progress Ring
                                Box(
                                    modifier = Modifier.size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(46.dp)) {
                                        val strokeW = 3.5.dp.toPx()
                                        val d = size.minDimension - strokeW
                                        val topL = Offset(strokeW / 2, strokeW / 2)
                                        val arcS = Size(d, d)
                                        drawArc(
                                            color = pal.boneDark,
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = topL,
                                            size = arcS,
                                            style = Stroke(width = strokeW)
                                        )
                                        drawArc(
                                            color = pal.gold,
                                            startAngle = -90f,
                                            sweepAngle = 360f * (pct / 100f),
                                            useCenter = false,
                                            topLeft = topL,
                                            size = arcS,
                                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        text = "$pct%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 6: View Mode Switcher (Surahs vs Ajzaa) ──
            item(key = "view_mode_pills") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(pal.bone)
                        .padding(4.dp)
                ) {
                    Row {
                        TabPill(
                            label = "Surahs",
                            selected = viewMode == "surah",
                            pal = pal,
                            fontUi = fontUi,
                            onClick = { viewMode = "surah" }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TabPill(
                            label = "Ajzaa (Juz)",
                            selected = viewMode == "juz",
                            pal = pal,
                            fontUi = fontUi,
                            onClick = { viewMode = "juz" }
                        )
                    }
                }
            }

            // ── Section 7: Search & Filter Bar ──
            item(key = "search_and_filter") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (viewMode == "surah") "Search surah…" else "Filter by number…",
                                color = pal.inkMuted,
                                fontSize = 14.sp,
                                fontFamily = fontBody
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = pal.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = pal.inkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.teal,
                            unfocusedBorderColor = pal.boneDark,
                            focusedContainerColor = pal.cream,
                            unfocusedContainerColor = pal.cream,
                            cursorColor = pal.teal,
                            focusedTextColor = pal.ink,
                            unfocusedTextColor = pal.ink
                        ),
                        singleLine = true
                    )

                    // Filter Memorized button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (showOnlyMemorized) pal.green.copy(alpha = 0.12f) else pal.cream,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (showOnlyMemorized) pal.green else pal.boneDark
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .clickable { showOnlyMemorized = !showOnlyMemorized }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = if (showOnlyMemorized) pal.green else pal.inkMid,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showOnlyMemorized) "Memorized Only" else "Filter Memorized",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (showOnlyMemorized) pal.green else pal.inkMid,
                                fontFamily = fontUi
                            )
                        }
                    }
                }
            }

            // ── Section 8: Card Grid (Surahs or Ajzaa) ──
            if (viewMode == "surah") {
                items(chunkedSurahs, key = { row -> row.map { it.id }.joinToString("-") }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (chapter in row) {
                            val memCount = memBySurah[chapter.id] ?: 0
                            val isMemorized = chapter.versesCount > 0 && memCount >= chapter.versesCount
                            val hasPartial = !isMemorized && memCount > 0
                            val progress = if (chapter.versesCount > 0) memCount.toFloat() / chapter.versesCount else 0f
                            val isThisChapterAudio = AudioEngine.current?.chapterId == chapter.id
                            val isAudioPlayingThisChapter = isThisChapterAudio && AudioEngine.playing

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = pal.cream),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isMemorized) pal.green else pal.boneDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenHifdhReader(chapter.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Chapter ID Badge
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isMemorized) pal.green.copy(alpha = 0.15f) else pal.bone
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chapter.id.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMemorized) pal.green else pal.inkMid,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    // Middle Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = chapter.nameSimple,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.ink,
                                                fontFamily = fontUi,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isMemorized) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Memorized",
                                                    tint = pal.green,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${chapter.versesCount} Ayahs",
                                                fontSize = 11.sp,
                                                color = pal.inkMuted,
                                                fontFamily = fontBody
                                            )
                                            if (hasPartial) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(pal.goldSoft)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$memCount done",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pal.gold,
                                                        fontFamily = fontBody
                                                    )
                                                }
                                            }
                                        }
                                        if (isMemorized || hasPartial) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { if (isMemorized) 1f else progress.coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                color = if (isMemorized) pal.green else pal.teal,
                                                trackColor = pal.bone
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Play chapter audio button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAudioPlayingThisChapter) pal.teal else pal.bone
                                            )
                                            .clickable {
                                                if (isThisChapterAudio) {
                                                    AudioEngine.togglePlayPause()
                                                } else {
                                                    AudioEngine.playChapter(chapter.id)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAudioPlayingThisChapter) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Play Audio",
                                            tint = if (isAudioPlayingThisChapter) Color.White else pal.inkMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // Juz mode
                items(chunkedAjzaa, key = { row -> row.joinToString("-") }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (juzNumber in row) {
                            val totalAyahs = totalJuzVerses[juzNumber] ?: 0
                            val memAyahs = memJuzVerses[juzNumber] ?: 0
                            val memPct = if (totalAyahs > 0) (memAyahs * 100 / totalAyahs) else 0
                            val isMemorized = totalAyahs > 0 && memAyahs >= totalAyahs
                            val firstChapterId = JUZ_STARTS.getOrNull(juzNumber - 1)?.verseKey?.substringBefore(":")?.toIntOrNull() ?: 1

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = pal.cream),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isMemorized) pal.green else pal.boneDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenHifdhReader(firstChapterId) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Juz Badge
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isMemorized) pal.green.copy(alpha = 0.15f) else pal.bone
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$juzNumber",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMemorized) pal.green else pal.inkMid,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Juz $juzNumber",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.ink,
                                                fontFamily = fontUi
                                            )
                                            if (isMemorized) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Memorized",
                                                    tint = pal.green,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$memAyahs / $totalAyahs Ayahs",
                                                fontSize = 11.sp,
                                                color = pal.inkMuted,
                                                fontFamily = fontBody
                                            )
                                            Text(
                                                text = "$memPct%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = pal.inkMid,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { if (isMemorized) 1f else (memPct / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            color = if (isMemorized) pal.green else pal.teal,
                                            trackColor = pal.bone
                                        )
                                    }
                                }
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ──

    // Goal Dialog
    if (showGoalDialog) {
        HifdhGoalDialog(
            pal = pal,
            defaultChapterId = goals.firstOrNull()?.targetId ?: 114,
            onDismiss = { showGoalDialog = false },
            onSaved = {
                showGoalDialog = false
                tick++
            }
        )
    }

    // Test Dialog
    if (showTestDialog) {
        HifdhTestDialog(
            pal = pal,
            chapterId = testChapterId,
            queue = testQueue,
            queueTitle = testQueueTitle,
            onDismiss = {
                showTestDialog = false
                testQueue = null
                testQueueTitle = null
            },
            onOpenSurah = { id, _ ->
                showTestDialog = false
                testQueue = null
                testQueueTitle = null
                onOpenHifdhReader(id)
            }
        )
    }

    // Memorized Surahs Dialog
    if (showSurahsDialog) {
        Dialog(onCloseRequest = { showSurahsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pal.white,
                shadowElevation = 16.dp,
                modifier = Modifier.widthIn(max = 480.dp).heightIn(max = 520.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Memorized Surahs (${fullyMemorized.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showSurahsDialog = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (fullyMemorized.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No surahs memorized completely yet.",
                                fontSize = 14.sp,
                                color = pal.inkMuted,
                                fontFamily = fontBody
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(fullyMemorized, key = { it.id }) { ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(pal.cream)
                                        .border(1.dp, pal.boneDark, RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(pal.green.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ch.id.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = pal.green,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = ch.nameSimple,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink,
                                        fontFamily = fontUi,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = pal.green,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Memorized Ayahs Dialog
    if (showAyahsDialog) {
        Dialog(onCloseRequest = { showAyahsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pal.white,
                shadowElevation = 16.dp,
                modifier = Modifier.widthIn(max = 480.dp).heightIn(max = 520.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Memorized Ayahs ($totalMemorized)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showAyahsDialog = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (ayahsBySurah.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ayahs memorized yet.",
                                fontSize = 14.sp,
                                color = pal.inkMuted,
                                fontFamily = fontBody
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ayahsBySurah.forEach { (surahId, verses) ->
                                item(key = "ayahs-$surahId") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(pal.cream)
                                            .border(1.dp, pal.boneDark, RoundedCornerShape(14.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = QuranStore.chapter(surahId)?.nameSimple ?: "Surah $surahId",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = pal.ink,
                                                fontFamily = fontUi,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(pal.teal.copy(alpha = 0.12f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "${verses.size} ayahs",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = pal.teal,
                                                    fontFamily = fontBody
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Verses: ${collapseRanges(verses)}",
                                            fontSize = 12.sp,
                                            color = pal.inkMid,
                                            fontFamily = FontFamily.Monospace
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

    // Breakdown Dialog
    breakdownFor?.let { bChapter ->
        val bMem = memBySurah[bChapter] ?: 0
        val bTotal = QuranStore.chapter(bChapter)?.versesCount ?: 0
        val bDue = history.keys.count { key ->
            val e = history[key]
            key.startsWith("$bChapter:") && (e?.card?.isDue(nowMs) ?: true)
        }
        HifdhBreakdownDialog(
            pal = pal,
            chapterId = bChapter,
            memorized = bMem,
            total = bTotal,
            due = bDue,
            onDismiss = { breakdownFor = null },
            onReview = {
                breakdownFor = null
                onOpenHifdhReader(bChapter)
            }
        )
    }
}

@Composable
private fun StatBox(
    value: String,
    sub: String,
    label: String,
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = pal.cream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi
                )
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    fontFamily = fontBody,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = pal.inkMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun QueueCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    dueCount: Int,
    totalCount: Int,
    bgColor: Color,
    borderColor: Color,
    headerColor: Color,
    subColor: Color,
    btnTextColor: Color,
    fontUi: FontFamily,
    fontBody: FontFamily,
    modifier: Modifier = Modifier,
    onReview: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        fontFamily = fontUi
                    )
                }
                Text(
                    text = "$dueCount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    fontFamily = fontUi
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalCount TOTAL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = subColor,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onReview,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.75f),
                    contentColor = btnTextColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text(
                    text = "Review Due",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontUi
                )
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    pal: NurPalette,
    fontUi: FontFamily,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) pal.white else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) pal.ink else pal.inkMuted,
            fontFamily = fontUi
        )
    }
}

private fun collapseRanges(verses: List<Int>): String {
    if (verses.isEmpty()) return ""
    val sorted = verses.distinct().sorted()
    val parts = mutableListOf<String>()
    var start = sorted[0]
    var prev = sorted[0]
    for (i in 1 until sorted.size) {
        val v = sorted[i]
        if (v == prev + 1) {
            prev = v
        } else {
            parts.add(if (start == prev) "$start" else "$start-$prev")
            start = v
            prev = v
        }
    }
    parts.add(if (start == prev) "$start" else "$start-$prev")
    return parts.joinToString(", ")
}
