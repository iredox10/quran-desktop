package com.nur.quran.desktop.ui.hifdh

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.DeskVerse
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.FsrsRating
import com.nur.quran.shared.FsrsScheduler
import com.nur.quran.shared.HifdhGoal
import com.nur.quran.shared.HifdhHistoryEntry
import com.nur.quran.shared.HifdhQueues
import com.nur.quran.shared.HifdhStore
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun defaultTargetDateString(): String =
    LocalDate.now().plusDays(30).format(DateFmt)

private fun parseDateToMs(text: String): Long? = try {
    LocalDate.parse(text.trim(), DateFmt)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

@Composable
private fun ModalHeader(
    pal: NurPalette,
    title: String,
    onDismiss: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(pal.goldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = pal.gold,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                fontFamily = fontUi
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = pal.inkMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(pal: NurPalette, text: String) {
    val fontBody = rememberBodyFontFamily()
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = pal.inkMuted,
        fontFamily = fontBody
    )
}

/**
 * Goal-creation dialog mirroring Android `HifdhGoalModal` (condensed for
 * desktop: surah number + yyyy-MM-dd date fields instead of a Material
 * date picker).
 */
@Composable
fun HifdhGoalDialog(
    pal: NurPalette,
    defaultChapterId: Int,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var surahText by remember(defaultChapterId) {
        mutableStateOf(defaultChapterId.coerceIn(1, 114).toString())
    }
    var dateText by remember { mutableStateOf(defaultTargetDateString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val surahNum = surahText.trim().toIntOrNull()
    val surahValid = surahNum != null && surahNum in 1..114
    val parsedMs = remember(dateText) { parseDateToMs(dateText) }
    val daysUntil = remember(parsedMs) {
        parsedMs?.let { ((it - System.currentTimeMillis()) / 86_400_000L).toInt() }
    }
    val dateValid = parsedMs != null && (daysUntil ?: -1) >= 0
    val chapter = remember(surahNum) {
        if (surahValid) QuranStore.chapter(surahNum!!) else null
    }

    Dialog(onCloseRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = pal.white,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                ModalHeader(pal = pal, title = "Set Memorization Goal", onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(pal = pal, text = "SURAH NUMBER (1-114)")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = surahText,
                    onValueChange = { surahText = it.filter { c -> c.isDigit() }.take(3); error = null },
                    singleLine = true,
                    isError = !surahValid,
                    supportingText = {
                        Text(
                            text = chapter?.let { "${it.id}. ${it.nameSimple} • ${it.versesCount} ayahs" }
                                ?: "Enter a surah number 1-114",
                            fontSize = 11.sp,
                            color = if (surahValid) pal.inkMuted else Color(0xFFEF4444),
                            fontFamily = fontBody
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(pal = pal, text = "TARGET DATE (YYYY-MM-DD)")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; error = null },
                    singleLine = true,
                    placeholder = { Text("yyyy-MM-dd", color = pal.inkMuted) },
                    isError = !dateValid,
                    supportingText = {
                        Text(
                            text = when {
                                parsedMs == null -> "Use format yyyy-MM-dd"
                                (daysUntil ?: -1) < 0 -> "Pick a future date"
                                else -> "~$daysUntil days away"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dateValid) pal.green else Color(0xFFEF4444),
                            fontFamily = fontBody
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error!!, fontSize = 12.sp, color = Color(0xFFEF4444), fontFamily = fontBody)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (!surahValid) {
                            error = "Enter a surah number 1-114."
                            return@Button
                        }
                        val targetMs = parseDateToMs(dateText)
                        if (targetMs == null || targetMs < System.currentTimeMillis()) {
                            error = "Pick a future date (yyyy-MM-dd)."
                            return@Button
                        }
                        val now = System.currentTimeMillis()
                        val goals = HifdhStore.loadHifdhGoals()
                        HifdhStore.saveHifdhGoals(
                            goals + HifdhGoal(
                                id = now.toString(),
                                targetType = "surah",
                                targetId = surahNum!!,
                                targetDate = targetMs,
                                createdAt = now
                            )
                        )
                        onSaved()
                    },
                    enabled = surahValid && dateValid,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.gold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Create Goal", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = fontUi)
                }
            }
        }
    }
}

/**
 * Per-surah breakdown dialog mirroring Android `HifdhBreakdownModal`:
 * surah name header, memorized/total progress bar, counts rows, Review.
 */
@Composable
fun HifdhBreakdownDialog(
    pal: NurPalette,
    chapterId: Int,
    memorized: Int,
    total: Int,
    due: Int,
    onDismiss: () -> Unit,
    onReview: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()
    val chapter = remember(chapterId) { QuranStore.chapter(chapterId) }
    val safeTotal = total.coerceAtLeast(0)
    val safeMemorized = memorized.coerceIn(0, safeTotal.coerceAtLeast(1))
    val remaining = (safeTotal - safeMemorized).coerceAtLeast(0)
    val progress = if (safeTotal > 0) safeMemorized.toFloat() / safeTotal else 0f

    Dialog(onCloseRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = pal.white,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(pal.goldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = pal.gold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chapter?.let { "${it.id}. ${it.nameSimple}" } ?: "Surah $chapterId",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink,
                                fontFamily = fontUi
                            )
                            if (chapter != null) {
                                Text(
                                    text = chapter.nameArabic,
                                    fontSize = 16.sp,
                                    color = pal.gold,
                                    fontFamily = fontArabic
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = pal.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "$safeMemorized / $safeTotal ayahs memorized",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.inkMid,
                    fontFamily = fontBody
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = pal.gold,
                    trackColor = pal.bone
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BreakdownCount(pal = pal, count = "$safeMemorized", label = "MEMORIZED", modifier = Modifier.weight(1f))
                    BreakdownCount(pal = pal, count = "$due", label = "DUE", modifier = Modifier.weight(1f))
                    BreakdownCount(pal = pal, count = "$remaining", label = "REMAINING", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onReview,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = fontUi)
                }
            }
        }
    }
}

@Composable
private fun BreakdownCount(
    pal: NurPalette,
    count: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pal.ink, fontFamily = fontUi)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pal.inkMuted, fontFamily = fontBody)
        }
    }
}

/**
 * Self-test entry dialog. Desktop has no word-reveal test engine, so this
 * stays an honest handoff: instructions + verse count + open-surah button
 * into the Surah reader where each verse can be checked.
 */
@Composable
fun HifdhTestDialog(
    pal: NurPalette,
    chapterId: Int,
    onDismiss: () -> Unit,
    onOpenSurah: (Int, String?) -> Unit,
    queue: List<String>? = null,
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()
    val chapter = remember(chapterId) { QuranStore.chapter(chapterId) }
    // Queue mode: explicit verse keys ("chapter:verse") tested in random
    // order; null keeps the chapter-handoff behavior below.
    // effectiveKeys = queue ?: derivedList.
    val derivedKeys = remember(chapterId) {
        QuranStore.versesOfChapter(chapterId).map { it.verseKey }
    }
    val verseCount = chapter?.versesCount ?: QuranStore.versesOfChapter(chapterId).size
    val queueMode = queue != null
    val effectiveKeys = queue ?: derivedKeys
    var testedKeys by remember(chapterId, queue) { mutableStateOf(setOf<String>()) }
    var testKey by remember(chapterId, queue) { mutableStateOf(HifdhQueues.randomQueuePick(effectiveKeys)) }
    var revealed by remember(chapterId, queue, testKey) { mutableStateOf(false) }
    var resultRating by remember(chapterId, queue) { mutableStateOf<Int?>(null) }
    val scheduler = remember { FsrsScheduler() }
    // Weak-transition links (web parity): flagged on "Again" grades, cleared on
    // Good/Easy — the same rules grade() persists below.
    val transitionLinks = remember(chapterId, queue) { HifdhStore.loadTransitionLinks() }
    fun currentVerse(): DeskVerse? = testKey?.let { k ->
        val parts = k.split(":")
        val verseChapter = parts.getOrNull(0)?.toIntOrNull() ?: chapterId
        QuranStore.versesOfChapter(verseChapter).firstOrNull { v -> v.verseKey == k }
    }
    fun pickNext() {
        val current = testKey
        if (current != null) testedKeys = testedKeys + current
        val remaining = effectiveKeys.filter { it !in testedKeys }
        val next = HifdhQueues.randomQueuePick(remaining)
        testKey = next
        revealed = false
        resultRating = null
    }
    fun grade(rating: Int) {
        val key = testKey ?: return
        val history = HifdhStore.loadHifdhHistory().toMutableMap()
        val nowMs = System.currentTimeMillis()
        val prevCard = history[key]?.card
        val card = if (prevCard != null) {
            scheduler.next(prevCard, nowMs, rating)
        } else {
            scheduler.next(scheduler.createEmptyCard(nowMs), nowMs, rating)
        }
        val strength = when (rating) {
            FsrsRating.AGAIN -> "weak"
            FsrsRating.EASY -> "strong"
            else -> "medium"
        }
        history[key] = HifdhHistoryEntry(card = card, lastReviewed = nowMs, strength = strength)
        HifdhStore.saveHifdhHistory(history)
        val links = HifdhStore.loadTransitionLinks().toMutableSet()
        when (rating) {
            FsrsRating.AGAIN -> links.add(key)
            FsrsRating.GOOD, FsrsRating.EASY -> links.remove(key)
        }
        HifdhStore.saveTransitionLinks(links)
        resultRating = rating
    }

    Dialog(onCloseRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = pal.white,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(pal.goldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = null,
                                tint = pal.gold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hifdh Test",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink,
                                fontFamily = fontUi
                            )
                            Text(
                                text = chapter?.let { "${it.id}. ${it.nameSimple}" } ?: "Surah $chapterId",
                                fontSize = 11.sp,
                                color = pal.inkMuted,
                                fontFamily = fontBody
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = pal.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                val doneTitle = "Queue Completed!"
                val doneBody = "You have no more ayahs to review in this queue right now."
                if ((queueMode && effectiveKeys.isEmpty()) ||
                    (queue == null && effectiveKeys.isEmpty()) ||
                    (effectiveKeys.isNotEmpty() && testKey == null)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.cream),
                        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (queue == null && effectiveKeys.isEmpty()) {
                                Text(
                                    text = "You haven't memorized any ayahs yet!",
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E).copy(alpha = 0.8f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = doneTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.ink,
                                    fontFamily = fontUi,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = doneBody,
                                    fontSize = 12.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    val verse = currentVerse()
                    val pill = testKey?.let { k ->
                        val parts = k.split(":")
                        val cId = parts.getOrNull(0)?.toIntOrNull() ?: chapterId
                        val name = QuranStore.chapter(cId)?.nameSimple ?: "Surah $cId"
                        "$name, Ayah ${parts.getOrNull(1) ?: ""}"
                    } ?: "Surah $chapterId"
                    val displayArabic = verse?.let { verseDisplayArabic(it, PrefsCache.getFont()) } ?: ""
                    // Weak transition: last 4 words of the previous ayah, shown
                    // before reveal (web: "Start reciting from the end of the
                    // previous Ayah").
                    val weakTail = testKey?.let { k ->
                        val parts = k.split(":")
                        val cId = parts.getOrNull(0)?.toIntOrNull() ?: chapterId
                        val ayahNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        if (k in transitionLinks && ayahNum > 1 && !revealed) {
                            QuranStore.versesOfChapter(cId)
                                .firstOrNull { it.verseNumber == ayahNum - 1 }
                                ?.arabic
                                ?.split(" ")
                                ?.takeLast(4)
                                ?.joinToString(" ")
                        } else null
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.cream),
                        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = RoundedCornerShape(100), color = pal.goldSoft) {
                                Text(
                                    text = pill,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.gold,
                                    fontFamily = fontUi,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (weakTail != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF97316).copy(alpha = 0.10f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, Color(0xFFF97316).copy(alpha = 0.30f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFFF97316),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "WEAK TRANSITION DETECTED",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = Color(0xFFF97316),
                                                fontFamily = fontUi
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Start reciting from the end of the previous Ayah:",
                                            fontSize = 11.sp,
                                            color = pal.inkMuted,
                                            fontFamily = fontBody,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        CompositionLocalProvider(
                                            LocalLayoutDirection provides LayoutDirection.Rtl
                                        ) {
                                            Text(
                                                text = "...$weakTail",
                                                fontSize = 20.sp,
                                                color = pal.ink,
                                                fontFamily = fontArabic,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Right
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Recite this ayah from memory, then tap to verify.",
                                fontSize = 12.sp,
                                color = pal.inkMuted,
                                fontFamily = fontBody,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { revealed = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayArabic,
                                    fontSize = 22.sp,
                                    color = pal.ink,
                                    fontFamily = fontArabic,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().alpha(if (revealed) 1f else 0.05f)
                                )
                                if (!revealed) {
                                    Surface(
                                        shape = RoundedCornerShape(100),
                                        color = pal.gold,
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 14.dp, vertical = 6.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Visibility,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Tap to Reveal",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = fontUi
                                            )
                                        }
                                    }
                                }
                            }
                            if (revealed && resultRating == null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "How well did you remember this?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.ink,
                                    fontFamily = fontUi,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GradeButton("Complete Blank / Again", Color(0xFFDC2626), fontUi, Modifier.weight(1f)) { grade(FsrsRating.AGAIN) }
                                    GradeButton("Needed Prompt / Hard", Color(0xFFEA580C), fontUi, Modifier.weight(1f)) { grade(FsrsRating.HARD) }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GradeButton("Hesitated / Good", Color(0xFF2563EB), fontUi, Modifier.weight(1f)) { grade(FsrsRating.GOOD) }
                                    GradeButton("Perfect / Easy", Color(0xFF16A34A), fontUi, Modifier.weight(1f)) { grade(FsrsRating.EASY) }
                                }
                            }
                            if (revealed && resultRating != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val passed = (resultRating ?: FsrsRating.AGAIN) >= FsrsRating.GOOD
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (passed) Color(0xFF22C55E).copy(alpha = 0.14f)
                                            else Color(0xFFDC2626).copy(alpha = 0.14f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (passed) Icons.Filled.CheckCircle
                                        else Icons.Filled.Refresh,
                                        contentDescription = null,
                                        tint = if (passed) Color(0xFF22C55E) else Color(0xFFDC2626),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (passed) "Mashallah! Keep it up." else "Keep practicing!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (passed) pal.gold else Color(0xFFDC2626),
                                    fontFamily = fontUi,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { pickNext() },
                                    colors = ButtonDefaults.buttonColors(containerColor = pal.gold),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Text("Test Another Ayah", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = fontUi)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onOpenSurah(chapterId, null) },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.gold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Surah", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = fontUi)
                }
            }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    color: Color,
    fontUi: FontFamily,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.14f),
            contentColor = color
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(52.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontFamily = fontUi, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
