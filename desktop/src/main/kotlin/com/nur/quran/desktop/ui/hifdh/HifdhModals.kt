package com.nur.quran.desktop.ui.hifdh

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.HifdhGoal
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
    onOpenSurah: (Int, String?) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()
    val chapter = remember(chapterId) { QuranStore.chapter(chapterId) }
    val verseCount = chapter?.versesCount ?: QuranStore.versesOfChapter(chapterId).size

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
                                text = "Self-Test",
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
                        if (chapter != null) {
                            Text(
                                text = chapter.nameArabic,
                                fontSize = 28.sp,
                                color = pal.gold,
                                fontFamily = fontArabic
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = "$verseCount verses",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recite from memory, then check each verse in the Surah reader.",
                            fontSize = 12.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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
