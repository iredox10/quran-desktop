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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.desktop.ui.theme.timeAgo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reading-history page: every visit recorded by [RecentlyReadStore], grouped
 * by day with a two-tap clear-all. Mirrors the mobile history concept.
 */
@Composable
fun HistoryScreenDesktop(
    pal: NurPalette,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> }
) {
    var tick by remember { mutableStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    val danger = Color(0xFFB3413A)
    val fontUi = rememberUiFontFamily()
    val entries = remember(tick) { RecentlyReadStore.all() }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = pal.ink
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reading history",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi
                )
                Text(
                    text = "${entries.size} visit${if (entries.size == 1) "" else "s"}",
                    fontSize = 11.sp,
                    color = pal.inkMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (entries.isNotEmpty()) {
                Surface(
                    onClick = {
                        if (confirmClear) {
                            RecentlyReadStore.clear()
                            confirmClear = false
                            tick++
                        } else {
                            confirmClear = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (confirmClear) danger.copy(alpha = 0.12f) else pal.cream,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (confirmClear) danger else pal.boneDark
                    )
                ) {
                    Text(
                        text = if (confirmClear) "Tap again to confirm" else "Clear all",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (confirmClear) danger else pal.inkMid,
                        fontFamily = fontUi,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = pal.boneDark.copy(alpha = 0.5f), thickness = 1.dp)

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = pal.inkMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No history yet — open any surah to start tracking",
                    fontSize = 14.sp,
                    color = pal.inkMuted,
                    fontFamily = fontUi,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val grouped = remember(tick, entries) { groupByDay(entries) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 12.dp
                )
            ) {
                grouped.forEach { (dayLabel, dayEntries) ->
                    item(key = "header_$dayLabel") {
                        Text(
                            text = dayLabel,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.gold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                        )
                    }
                    items(dayEntries, key = { "${it.timestamp}_${it.chapterId}_${it.verseKey}" }) { entry ->
                        HistoryRow(pal = pal, entry = entry) {
                            onOpenSurah(entry.chapterId, entry.verseKey)
                        }
                        HorizontalDivider(
                            color = pal.boneDark.copy(alpha = 0.35f),
                            thickness = 1.dp
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    pal: NurPalette,
    entry: RecentlyReadStore.Entry,
    onClick: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val snippet = remember(entry.chapterId, entry.verseKey) {
        entry.verseKey?.let { key ->
            QuranStore.versesOfChapter(entry.chapterId)
                .firstOrNull { it.verseKey == key }
                ?.let { com.nur.quran.desktop.ui.components.verseSnippetArabic(it) }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(pal.tealSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.chapterId.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = pal.teal,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.chapterName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink,
                    fontFamily = fontUi,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                entry.verseKey?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.gold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            if (snippet != null) {
                Text(
                    text = snippet,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (entry.timestamp > 0) timeAgo(entry.timestamp) else "",
            fontSize = 10.sp,
            color = pal.inkMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}

/** Groups newest-first entries into Today / Yesterday / "Sep 28" buckets. */
private fun groupByDay(
    entries: List<RecentlyReadStore.Entry>
): List<Pair<String, List<RecentlyReadStore.Entry>>> {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
    val fmt = SimpleDateFormat("MMM d", Locale.ENGLISH)

    val buckets = LinkedHashMap<String, MutableList<RecentlyReadStore.Entry>>()
    entries.forEach { entry ->
        val c = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
        val day = c.get(Calendar.DAY_OF_YEAR) to c.get(Calendar.YEAR)
        val label = when {
            day == today -> "Today"
            day == yesterday -> "Yesterday"
            else -> fmt.format(Date(entry.timestamp)).uppercase()
        }
        buckets.getOrPut(label) { mutableListOf() }.add(entry)
    }
    return buckets.map { it.key to it.value.toList() }
}
