package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.analytics.ActivityLog
import com.nur.quran.desktop.ui.analytics.AnalyticsEngine
import com.nur.quran.desktop.ui.analytics.PeriodTabsPill
import com.nur.quran.desktop.ui.analytics.StatCell
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ReadingColor = Color(0xFF10B981)
private val MemorizingColor = Color(0xFF3B82F6)
private val ListeningColor = Color(0xFFF59E0B)
private val FocusColor = Color(0xFF8B5CF6)

private data class SessionStyle(val icon: ImageVector, val color: Color)

private fun sessionStyle(type: String): SessionStyle = when (type) {
    "pomodoro", "focus" -> SessionStyle(Icons.Filled.TrackChanges, FocusColor)
    "memorizing" -> SessionStyle(Icons.Filled.Layers, MemorizingColor)
    "listening" -> SessionStyle(Icons.Filled.Headphones, ListeningColor)
    else -> SessionStyle(Icons.Filled.MenuBook, ReadingColor)
}

/**
 * Full, searchable session log — desktop port of the web app's
 * `/progress/activity` page (`ActivityHistory.jsx`). Range tabs + a text
 * query filter the [SessionStore] log, grouped by day, capped at
 * [ActivityLog.MAX_ROWS] rows like the web page.
 */
@Composable
fun ActivityHistoryScreenDesktop(
    pal: NurPalette,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> }
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    var range by remember { mutableStateOf("week") }
    var query by remember { mutableStateOf("") }

    val rawSessions = remember { SessionStore.all() }
    val sessions = remember(rawSessions) {
        rawSessions.map {
            AnalyticsStats.Session(
                date = it.date,
                durationSec = it.durationSec,
                type = it.type,
                chapterId = it.chapterId,
                timestamp = it.timestamp
            )
        }
    }

    val now = remember { Date() }
    val filtered = remember(sessions, range, query, now) {
        ActivityLog.filterSessions(sessions, range, query, now)
    }
    val groups = remember(filtered, now) {
        ActivityLog.capRows(ActivityLog.groupSessionsByDay(filtered, now))
    }
    val stats = remember(filtered) { AnalyticsEngine.summarize(filtered) }
    val capped = filtered.size > ActivityLog.MAX_ROWS
    val trimmedQuery = query.trim()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(pal.white)
            .dragToScroll(listState),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth()) {
                // Header with back button and centered title.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(pal.boneDark.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = pal.ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SESSION LOG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = pal.inkMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Activity History",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Every reading, memorizing, focus and listening session — searchable.",
                            fontSize = 13.sp,
                            fontFamily = fontBody,
                            color = pal.inkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(38.dp))
                }
            }
        }

        // Filter card: range tabs, search box, summary chips.
        item {
            Card(
                modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = pal.cream),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PeriodTabsPill(
                            tabs = ActivityLog.HISTORY_TABS,
                            selected = range,
                            onSelect = { range = it },
                            pal = pal
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.width(300.dp),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "Search surah or activity type…",
                                    fontSize = 13.sp,
                                    fontFamily = fontBody,
                                    color = pal.inkMuted
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
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = "Clear search",
                                            tint = pal.inkMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = pal.teal,
                                unfocusedBorderColor = pal.boneDark,
                                focusedTextColor = pal.ink,
                                unfocusedTextColor = pal.ink,
                                focusedContainerColor = pal.white,
                                unfocusedContainerColor = pal.white
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LogSummaryChip(
                            label = "Total time",
                            value = AnalyticsEngine.formatDuration(stats.seconds),
                            pal = pal,
                            modifier = Modifier.weight(1f)
                        )
                        LogSummaryChip(
                            label = "Sessions",
                            value = stats.count.toString(),
                            pal = pal,
                            modifier = Modifier.weight(1f)
                        )
                        LogSummaryChip(
                            label = "Active days",
                            value = stats.activeDays.toString(),
                            pal = pal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Session log card.
        item {
            Card(
                modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = pal.cream),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SESSIONS · ${AnalyticsEngine.rangeTitle(range, now)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            color = pal.inkMuted
                        )
                        Text(
                            text = "${filtered.size} ${if (filtered.size == 1) "result" else "results"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp,
                            color = pal.inkMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        sessions.isEmpty() -> EmptyLogState(
                            icon = Icons.Filled.History,
                            title = "No activity yet",
                            body = "Reading, memorizing, focus and listening sessions you log will show up here.",
                            pal = pal
                        )
                        filtered.isEmpty() && trimmedQuery.isNotEmpty() -> EmptyLogState(
                            icon = Icons.Filled.Search,
                            title = "No results for \u201C$trimmedQuery\u201D",
                            body = "Try a different surah name or activity type.",
                            pal = pal,
                            actionLabel = "Clear search",
                            onAction = { query = "" }
                        )
                        filtered.isEmpty() -> EmptyLogState(
                            icon = Icons.Filled.Search,
                            title = "No activity in this period",
                            body = "Pick another range above or check All Time.",
                            pal = pal
                        )
                        else -> {
                            groups.forEachIndexed { index, group ->
                                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                                SessionGroupHeader(group = group, pal = pal)
                                group.sessions.forEach { session ->
                                    SessionLogRow(
                                        session = session,
                                        pal = pal,
                                        onOpenSurah = onOpenSurah
                                    )
                                }
                            }
                            if (capped) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Showing first ${ActivityLog.MAX_ROWS} of ${filtered.size} " +
                                        "sessions — refine your search to narrow it down",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.8.sp,
                                    color = pal.inkMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, pal.boneDark, RoundedCornerShape(16.dp))
                                        .background(pal.white)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun LogSummaryChip(
    label: String,
    value: String,
    pal: NurPalette,
    modifier: Modifier = Modifier
) {
    StatCell(label = label, value = value, pal = pal, modifier = modifier)
}

@Composable
private fun SessionGroupHeader(group: ActivityLog.DayGroup, pal: NurPalette) {
    val count = group.sessions.size
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.label.uppercase(Locale.US),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp,
            color = pal.inkMuted
        )
        Text(
            text = "${AnalyticsEngine.formatDuration(group.totalSeconds)} · $count ${if (count == 1) "session" else "sessions"}",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
            color = pal.inkMuted
        )
    }
}

@Composable
private fun SessionLogRow(
    session: AnalyticsStats.Session,
    pal: NurPalette,
    onOpenSurah: (Int, String?) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val style = remember(session.type) { sessionStyle(session.type) }
    val chapterId = session.chapterId
    val chapterName = remember(chapterId) {
        chapterId?.takeIf { it > 0 }?.let { QuranStore.chapter(it)?.nameSimple }
    }
    val label = AnalyticsEngine.TYPE_META[session.type]?.label ?: "Session"
    val title = when {
        chapterId == null || chapterId <= 0 -> label
        chapterName != null -> "$label · $chapterName"
        else -> "$label · Surah $chapterId"
    }
    val timeLabel = remember(session.timestamp) {
        if (session.timestamp > 0L) {
            SimpleDateFormat("h:mm a", Locale.US).format(Date(session.timestamp))
        } else {
            "Logged"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (chapterId != null && chapterId > 0) {
                    Modifier.clickable { onOpenSurah(chapterId, null) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(style.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(17.dp)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(pal.white)
                .border(1.5.dp, pal.boneDark.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontUi,
                    color = pal.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeLabel,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = pal.inkMuted
                )
            }
            Text(
                text = AnalyticsEngine.formatDuration(session.durationSec),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontUi,
                color = pal.inkMuted
            )
        }
    }
}

@Composable
private fun EmptyLogState(
    icon: ImageVector,
    title: String,
    body: String,
    pal: NurPalette,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = pal.teal.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontUi,
            color = pal.ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            fontFamily = fontBody,
            color = pal.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 380.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pal.white)
                    .border(1.5.dp, pal.boneDark, CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = actionLabel.uppercase(Locale.US),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = pal.inkMid
                )
            }
        }
    }
}
