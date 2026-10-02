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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.hifdh.HifdhBreakdownDialog
import com.nur.quran.desktop.ui.hifdh.HifdhGoalDialog
import com.nur.quran.desktop.ui.hifdh.HifdhTestDialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.HifdhGoal
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
            // ── Goal card: first goal, or "Set a goal" zero state ──
            item(key = "goal") {
                val goal = goals.firstOrNull()
                if (goal == null) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.cream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(pal.bone),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = pal.inkMid,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Set a goal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.ink,
                                    fontFamily = fontUi
                                )
                                Text(
                                    text = "Plan your memorization journey",
                                    fontSize = 12.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                            }
                            Button(
                                onClick = { showGoalDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = pal.gold,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Set goal", fontWeight = FontWeight.Bold, fontFamily = fontUi)
                            }
                        }
                    }
                } else {
                    val target = remember(goal.targetId) { QuranStore.chapter(goal.targetId) }
                    val memCount = memBySurah[goal.targetId] ?: 0
                    val totalVerses = target?.versesCount ?: 1
                    val pct = (memCount * 100 / totalVerses).coerceIn(0, 100)
                    val daysLeft = goal.daysLeft
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.cream),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, pal.gold.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(pal.goldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.gold,
                                    fontFamily = fontBody
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Memorize ${target?.nameSimple ?: "Surah ${goal.targetId}"}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.ink,
                                    fontFamily = fontUi
                                )
                                Text(
                                    text = if (daysLeft > 0) "$daysLeft days left • $memCount / $totalVerses ayahs"
                                    else if (pct >= 100) "Completed ✓ • $memCount / $totalVerses ayahs"
                                    else "Due now! • $memCount / $totalVerses ayahs",
                                    fontSize = 12.sp,
                                    color = if (daysLeft <= 0 && pct < 100) Color(0xFFEF4444) else pal.inkMuted,
                                    fontFamily = fontBody
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                    color = pal.gold,
                                    trackColor = pal.bone
                                )
                            }
                        }
                    }
                }
            }

            // ── Breakdown strip: memorized / due now / strong ──
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
                            countText = "$totalMemorized",
                            label = "MEMORIZED",
                            fontUi = fontUi,
                            fontBody = fontBody,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.width(1.dp).height(32.dp)
                                .background(pal.boneDark)
                        )
                        StatCell(
                            pal = pal,
                            countText = "$dueCount",
                            label = "DUE NOW",
                            accent = dueCount > 0,
                            fontUi = fontUi,
                            fontBody = fontBody,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.width(1.dp).height(32.dp)
                                .background(pal.boneDark)
                        )
                        StatCell(
                            pal = pal,
                            countText = "$strongCount",
                            label = "STRONG",
                            fontUi = fontUi,
                            fontBody = fontBody,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Primary actions: review dues + test ──
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            mostDueSurahId?.let { onOpenHifdhReader(it) }
                        },
                        enabled = dueCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.teal,
                            contentColor = Color.White,
                            disabledContainerColor = pal.bone,
                            disabledContentColor = pal.inkMuted
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (dueCount > 0) "Review due ($dueCount)" else "Nothing due",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi
                        )
                    }
                    Button(
                        onClick = {
                            testFor = mostDueSurahId ?: goals.firstOrNull()?.targetId ?: 114
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
}

@Composable
private fun StatCell(
    pal: NurPalette,
    countText: String,
    label: String,
    fontUi: FontFamily,
    fontBody: FontFamily,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
