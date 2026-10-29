package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan

private data class PlanJournalReflection(
    val dayNumber: Int,
    val date: String,
    val text: String
)

/**
 * JOURNAL tab — web Planner.jsx parity (1234-1303).
 *
 * Web is read-only here: reflections are written from the day reader's
 * completion card and highlights from the reader's bookmark action. This tab
 * lists both for the active plan.
 */
@Composable
fun JournalTab(
    pal: NurPalette,
    plan: ReadingPlan,
    onOpenSurah: (Int, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val reflections = remember(plan.id, plan.assignments) {
        plan.assignments.mapNotNull { assignment ->
            val text = PlannerStore.getReflection(plan.id, assignment.date)
            if (text.isBlank()) null else PlanJournalReflection(assignment.dayNumber, assignment.date, text)
        }.sortedByDescending { it.dayNumber }
    }
    val marks = remember(plan.id) {
        PlannerStore.getMarks(plan.id).sortedByDescending { it.createdAt }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        JournalSection(pal, "Daily Reflections", pal.gold) {
            if (reflections.isEmpty()) {
                JournalEmptyState(
                    pal = pal,
                    icon = { Icon(Icons.Filled.MenuBook, null, tint = pal.inkMuted, modifier = Modifier.size(22.dp)) },
                    text = "No reflections yet. Complete a day to write one!"
                )
            } else {
                reflections.forEach { item ->
                    ReflectionCard(pal, item.dayNumber, item.date, item.text)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        JournalSection(pal, "Plan Highlights", pal.teal) {
            if (marks.isEmpty()) {
                JournalEmptyState(
                    pal = pal,
                    icon = { Icon(Icons.Filled.Diamond, null, tint = pal.inkMuted, modifier = Modifier.size(22.dp)) },
                    text = "No highlighted verses. Use the bookmark icon while reading!"
                )
            } else {
                marks.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { mark ->
                            val chapterId = mark.verseKey.substringBefore(":").toIntOrNull()
                            HighlightCard(
                                pal = pal,
                                surahName = mark.surahName.ifBlank { "Surah" },
                                verseKey = mark.verseKey,
                                createdAt = mark.createdAt,
                                onClick = {
                                    if (chapterId != null) onOpenSurah(chapterId, mark.verseKey)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun JournalSection(
    pal: NurPalette,
    title: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = pal.cream,
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontFamily = fontUi,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ReflectionCard(pal: NurPalette, dayNumber: Int, date: String, text: String) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val dateLabel = remember(date) {
        if (date.isNotBlank()) PlannerEngine.formatPlannerDateLabel(date) else ""
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pal.white,
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(pal.goldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumber.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.gold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "REFLECTION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 0.6.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.inkMuted
                    )
                }
                if (dateLabel.isNotBlank()) {
                    Text(
                        text = dateLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = pal.inkMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\"$text\"",
                fontFamily = fontBody,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = pal.ink
            )
        }
    }
}

@Composable
private fun HighlightCard(
    pal: NurPalette,
    surahName: String,
    verseKey: String,
    createdAt: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val ayahNum = verseKey.substringAfter(":", "")
    val timeLabel = remember(createdAt) {
        if (createdAt > 0L) {
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(createdAt))
        } else ""
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = pal.white,
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = surahName,
                        fontFamily = fontUi,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    if (ayahNum.isNotBlank()) {
                        Text(
                            text = "Ayah $ayahNum",
                            fontFamily = fontBody,
                            fontSize = 11.sp,
                            color = pal.inkMuted
                        )
                    }
                }
                Icon(
                    Icons.Filled.Diamond,
                    contentDescription = null,
                    tint = pal.gold.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = pal.goldSoft) {
                    Text(
                        text = verseKey,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = pal.gold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (timeLabel.isNotBlank()) {
                    Text(
                        text = timeLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = pal.inkMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to read verse \u2192",
                fontFamily = fontBody,
                fontSize = 11.sp,
                color = pal.teal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun JournalEmptyState(
    pal: NurPalette,
    icon: @Composable () -> Unit,
    text: String
) {
    val fontBody = rememberBodyFontFamily()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pal.white.copy(alpha = 0.5f),
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                fontFamily = fontBody,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                fontStyle = FontStyle.Italic,
                color = pal.inkMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
