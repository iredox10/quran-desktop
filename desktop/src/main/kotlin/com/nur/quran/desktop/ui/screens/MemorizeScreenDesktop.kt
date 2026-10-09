package com.nur.quran.desktop.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.hifdh.HifdhBreakdownDialog
import com.nur.quran.desktop.ui.hifdh.HifdhGoalDialog
import com.nur.quran.desktop.ui.hifdh.HifdhTestDialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.HifdhGoal
import com.nur.quran.shared.HifdhQueue
import com.nur.quran.shared.HifdhQueues
import com.nur.quran.shared.HifdhStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Desktop memorization hub mirroring Android `MemorizeScreen`:
 * hero goal card, compact stat strip, review/test actions, activity chart
 * (only when there is data), and a clean per-surah list where tapping a row
 * starts review.
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

    // Bumped after every HifdhStore write so load*() results refresh.
    var tick by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showGoalDialog by remember { mutableStateOf(false) }
    var breakdownFor by remember { mutableStateOf<Int?>(null) }
    var testFor by remember { mutableStateOf<Int?>(null) }
    var showSurahsDialog by remember { mutableStateOf(false) }
    var showAyahsDialog by remember { mutableStateOf(false) }
    val nowMs = remember(tick) { System.currentTimeMillis() }
    val history = remember(tick) { HifdhStore.loadHifdhHistory() }
    val goals = remember(tick) { HifdhStore.loadHifdhGoals() }
    val chapters = remember { QuranStore.chapters }

    val totalMemorized = history.size
    val dueCount = remember(history, nowMs) {
        history.count { (_, e) -> e.card?.isDue(nowMs) ?: true }
    }
    val strongCount = remember(history) {
        history.count { (_, e) -> e.strength.equals("strong", ignoreCase = true) }
    }
    // Surah id -> memorized verse count (history keys are "chapter:verse").
    val memBySurah = remember(history) {
        history.keys.groupingBy { it.substringBefore(":").toIntOrNull() }.eachCount()
    }
    // Last 30 days (oldest → today) with review counts bucketed by yyyy-MM-dd.
    val activityDays = remember(history) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
        val counts = IntArray(30)
        for (entry in history.values) {
            if (entry.lastReviewed <= 0L) continue
            val day = Instant.ofEpochMilli(entry.lastReviewed).atZone(zone).toLocalDate()
            val idx = 29 - ChronoUnit.DAYS.between(day, today).toInt()
            if (idx in 0..29) counts[idx]++
        }
        days.zip(counts.toList())
    }
    val activityTotal = remember(activityDays) { activityDays.sumOf { it.second } }
    // Surah with the most due verses — test-entry target.
    val mostDueSurahId = remember(history, nowMs) {
        history.keys
            .filter { key -> val e = history[key]; e?.card?.isDue(nowMs) ?: true }
            .groupingBy { it.substringBefore(":").toIntOrNull() }
            .eachCount()
            .maxByOrNull { it.value }?.key
    }
    // CHUNK C — Continue card source: most recent memorizing session that has a
    // valid chapter (Session has date only, so the last logged entry wins).
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
    // Web-exact relative label adapted to date granularity: same-day shows
    // "0m ago" (web shows "0m ago" when mins == 0, no 'just now' edge).
    val lastSessionAgo = remember(lastSession) {
        if (lastSession == null) "" else {
            val day = runCatching { LocalDate.parse(lastSession.date) }.getOrNull()
            val today = LocalDate.now()
            val days = if (day == null) 0 else ChronoUnit.DAYS.between(day, today).coerceAtLeast(0)
            if (days <= 0) "0m ago" else "${days}d ago"
        }
    }
    // CHUNK D — Sabaq/Sabqi/Manzil queues (web section 5 parity). Requires
    // lastSession (declared above) for the Sabqi classification.
    val queueSplit = remember(history, lastSession) {
        HifdhQueues.splitQueue(history, lastSession?.chapterId)
    }
    val dueByQueue = remember(queueSplit, history, nowMs) {
        HifdhQueue.entries.associateWith { q ->
            HifdhQueues.dueInQueue(queueSplit[q].orEmpty(), history, nowMs)
        }
    }
    fun mostDueIn(keys: List<String>): Int? =
        keys.groupingBy { it.substringBefore(":").toIntOrNull() }
            .eachCount().maxByOrNull { it.value }?.key
    // Web stat strip: fully-memorized surahs / 114, ayahs / 6236, goal %.
    val memorizedSurahCount = remember(memBySurah) {
        memBySurah.count { (surahId, mem) ->
            val total = surahId?.let { QuranStore.chapter(it)?.versesCount } ?: 0
            total > 0 && mem >= total
        }
    }
    val goalPct = ((totalMemorized * 100f) / 6236f).toInt().coerceIn(0, 100)
    // CHUNK D — Global list sources for the SURAH / AYAH stat modals.
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
    val filteredChapters = remember(chapters, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) chapters
        else chapters.filter {
            it.nameSimple.lowercase().contains(q.lowercase()) ||
                it.nameArabic.contains(q) ||
                it.id.toString() == q ||
                it.translatedNameText.lowercase().contains(q.lowercase())
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        // ── Header row: back, title + due count ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = pal.ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Memorize",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                fontFamily = fontUi,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (dueCount > 0) pal.goldSoft else pal.bone)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (dueCount > 0) "$dueCount due" else "All caught up",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dueCount > 0) pal.gold else pal.inkMuted,
                    fontFamily = fontBody
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── CHUNK C — Continue card (web §1): last memorizing session only ──
            val continueId = lastSession?.chapterId
            if (continueId != null) {
                item(key = "continue") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = pal.cream,
                        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onOpenHifdhReader(continueId) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Continue: ${lastSessionChapter?.nameSimple ?: "Surah $continueId"}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.ink,
                                    fontFamily = fontUi
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Last practiced $lastSessionAgo",
                                    fontSize = 11.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(pal.gold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "▶",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── Active Goals list (web MemorizeIndex section 6) ──
            if (goals.isNotEmpty()) {
                item(key = "active_goals_header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                                .background(pal.gold)
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
                        shape = RoundedCornerShape(18.dp),
                        color = pal.cream,
                        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Memorize ${chapter?.nameSimple ?: "Target"}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.ink,
                                    fontFamily = fontUi
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (daysLeft > 0) "$daysLeft days left • $curMem / $denom ayahs"
                                    else if (pct >= 100) "Completed ✓ • $curMem / $denom ayahs"
                                    else "Due now! • $curMem / $denom ayahs",
                                    fontSize = 11.sp,
                                    color = if (daysLeft <= 0 && pct < 100) Color(0xFFEF4444) else pal.inkMid,
                                    fontFamily = fontBody
                                )
                                if (resumeAyah != null && pct < 100) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Continue from Ayah $resumeAyah",
                                        fontSize = 11.sp,
                                        color = pal.gold,
                                        fontFamily = fontBody
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$curMem / $denom",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.ink,
                                    fontFamily = fontUi
                                )
                                Text(
                                    text = "Ayahs",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                            }
                        }
                    }
                }
            }

            // ── CHUNK C — Web stat strip: SURAH / AYAH / GOAL (3 cells) ──
            item(key = "breakdown") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = pal.cream,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCell(
                            pal = pal,
                            countText = "$memorizedSurahCount / 114",
                            label = "SURAH",
                            fontUi = fontUi,
                            fontBody = fontBody,
                            modifier = Modifier.weight(1f),
                            onClick = { showSurahsDialog = true }
                        )
                        Box(
                            modifier = Modifier.width(1.dp).height(32.dp)
                                .background(pal.boneDark)
                        )
                        StatCell(
                            pal = pal,
                            countText = "$totalMemorized / 6236",
                            label = "AYAH",
                            fontUi = fontUi,
                            fontBody = fontBody,
                            modifier = Modifier.weight(1f),
                            onClick = { showAyahsDialog = true }
                        )
                        Box(
                            modifier = Modifier.width(1.dp).height(32.dp)
                                .background(pal.boneDark)
                        )
                        StatCell(
                            pal = pal,
                            countText = "$goalPct%",
                            label = "GOAL",
                            fontUi = fontUi,
                            fontBody = fontBody,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── CHUNK D — Sabaq/Sabqi/Manzil review cards + Test/Set Goal ──
            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HifdhQueue.entries.forEach { queue ->
                        val total = queueSplit[queue].orEmpty()
                        val due = dueByQueue[queue].orEmpty()
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = pal.cream,
                            border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = HifdhQueues.queueLabel(queue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    )
                                    Text(
                                        text = "${due.size} due",
                                        fontSize = 12.sp,
                                        color = pal.gold,
                                        fontFamily = fontBody
                                    )
                                    Text(
                                        text = "${total.size} total",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = pal.inkMuted,
                                        fontFamily = fontBody
                                    )
                                }
                                Button(
                                    onClick = {
                                        testFor = mostDueIn(due.ifEmpty { total })
                                            ?: mostDueSurahId
                                            ?: goals.firstOrNull()?.targetId ?: 114
                                    },
                                    enabled = total.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pal.teal,
                                        contentColor = Color.White,
                                        disabledContainerColor = pal.bone,
                                        disabledContentColor = pal.inkMuted
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = "Review",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontUi
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                testFor = mostDueSurahId ?: 114
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pal.tealSoft,
                                contentColor = pal.teal
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test me",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontUi
                            )
                        }
                        Button(
                            onClick = { showGoalDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pal.gold,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set Goal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontUi
                            )
                        }
                    }
                }
                if (testFor != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                testFor?.let { testChapter ->
                    HifdhTestDialog(
                        pal = pal,
                        chapterId = testChapter,
                        onDismiss = { testFor = null },
                        onOpenSurah = { id, _ ->
                            testFor = null
                            onOpenHifdhReader(id)
                        }
                    )
                }
            }

            // ── Memorization activity: only worth the space once it exists ──
            if (activityTotal > 0) {
                item(key = "activity") {
                val maxCount = activityDays.maxOfOrNull { it.second } ?: 0
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = pal.cream),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVITY — LAST 30 DAYS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.inkMuted,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$activityTotal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            activityDays.forEachIndexed { index, (day, count) ->
                                val isToday = index == activityDays.lastIndex
                                val fraction =
                                    if (maxCount > 0) count.toFloat() / maxCount else 0f
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(pal.bone)
                                            .then(
                                                if (isToday) Modifier.border(
                                                    1.dp,
                                                    pal.gold,
                                                    RoundedCornerShape(6.dp)
                                                ) else Modifier
                                            ),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        if (fraction > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(fraction)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(pal.teal)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if ((index + 1) % 5 == 0) "${day.dayOfMonth}" else "",
                                        fontSize = 8.sp,
                                        color = pal.inkMuted,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.height(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // ── Surah list header + chapter search ──
            item(key = "list-header") {
                Text(
                    text = "YOUR SURAHS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = pal.inkMuted,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search surah…",
                            color = pal.inkMuted,
                            fontSize = 14.sp,
                            fontFamily = fontBody
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = pal.inkMuted, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = pal.inkMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
            }

            // ── Surah list rows: tap a row to review ──
            items(filteredChapters, key = { it.id }) { chapter ->
                val memCount = memBySurah[chapter.id] ?: 0
                val isMemorized = chapter.versesCount > 0 && memCount >= chapter.versesCount
                val progress = if (chapter.versesCount > 0) memCount.toFloat() / chapter.versesCount else 0f
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = pal.cream),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isMemorized) pal.green.copy(alpha = 0.5f) else pal.boneDark
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenHifdhReader(chapter.id) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                                    fontFamily = fontBody
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.nameSimple,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.ink,
                                    fontFamily = fontUi,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$memCount / ${chapter.versesCount} ayahs",
                                    fontSize = 11.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                            }
                            Text(
                                text = chapter.nameArabic,
                                fontSize = 20.sp,
                                color = pal.gold,
                                fontFamily = fontArabic,
                                maxLines = 1
                            )
                            IconButton(
                                onClick = { breakdownFor = chapter.id },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Surah breakdown",
                                    tint = pal.inkMuted.copy(alpha = 0.55f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        if (progress > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = if (isMemorized) pal.green else pal.teal,
                                trackColor = pal.bone
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Goal dialog (goal card click; default = first goal target or 114) ──
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

    // ── CHUNK D — Memorized Surahs global list (web section 8) ──
    if (showSurahsDialog) {
        Dialog(onCloseRequest = { showSurahsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pal.white,
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Memorized Surahs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showSurahsDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (fullyMemorized.isEmpty()) {
                        Text(
                            text = "No surahs memorized completely yet.",
                            fontSize = 13.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(fullyMemorized, key = { it.id }) { ch ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(pal.green.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ch.id.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = pal.green,
                                            fontFamily = fontBody
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Per-surah breakdown dialog (hosted at screen level) ──
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

    // ── CHUNK D — Memorized Ayahs global list (web section 9) ──
    if (showAyahsDialog) {
        Dialog(onCloseRequest = { showAyahsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pal.white,
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Memorized Ayahs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showAyahsDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (ayahsBySurah.isEmpty()) {
                        Text(
                            text = "No ayahs memorized yet.",
                            fontSize = 13.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ayahsBySurah.forEach { (surahId, verses) ->
                                item(key = "ayahs-$surahId") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = QuranStore.chapter(surahId)?.nameSimple
                                                    ?: "Surah $surahId",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.ink,
                                                fontFamily = fontUi,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(pal.bone),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${verses.size} ayahs",
                                                    fontSize = 11.sp,
                                                    color = pal.inkMid,
                                                    fontFamily = fontBody,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Verses: ${collapseRanges(verses)}",
                                            fontSize = 12.sp,
                                            color = pal.inkMid,
                                            fontFamily = fontBody
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

@Composable
private fun StatCell(
    pal: NurPalette,
    countText: String,
    label: String,
    fontUi: FontFamily,
    fontBody: FontFamily,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = countText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent) pal.gold else pal.ink,
            fontFamily = fontUi
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = pal.inkMuted,
            fontFamily = fontBody
        )
    }
}
