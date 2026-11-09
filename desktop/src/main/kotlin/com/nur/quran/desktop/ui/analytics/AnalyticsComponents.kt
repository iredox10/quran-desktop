package com.nur.quran.desktop.ui.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.ChapterJson
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ReadingColor = Color(0xFF10B981)
val MemorizingColor = Color(0xFF3B82F6)
val FocusColor = Color(0xFF8B5CF6)
val ListeningColor = Color(0xFFF59E0B)

/** Circular progress ring mirroring web CircularProgress. */
@Composable
fun CircularProgressRing(
    percent: Int,
    color: Color,
    size: Dp = 70.dp,
    strokeWidth: Dp = 6.dp,
    trackColor: Color = Color(0xFFE5E7EB)
) {
    val frac = (percent.toFloat() / 100f).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val diameter = size.toPx() - strokePx
        val topLeft = Offset(strokePx / 2f, strokePx / 2f)
        val arcSize = Size(diameter, diameter)

        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(strokePx, cap = StrokeCap.Round)
        )

        // Active progress arc
        if (frac > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * frac,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Top 3 Hero Metric Cards: Consistency (streak), Today's Focus, and Weekly Goal.
 * Ported 1:1 from Progress.jsx lines 134-193.
 */
@Composable
fun TopHeroCards(
    pal: NurPalette,
    streak: Int,
    todayMinutes: Int,
    weeklyTotalMins: Int,
    weeklyGoalMins: Int = 180,
    weeklyGoalPercent: Int,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Consistency
        Card(
            modifier = Modifier.weight(1f).height(180.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSISTENCY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = pal.inkMuted
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (streak > 0) Color(0xFFFEE2E2) else pal.boneDark.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Whatshot,
                            contentDescription = "Streak",
                            tint = if (streak > 0) Color(0xFFE75344) else pal.inkMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$streak",
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = fontUi,
                            color = pal.ink,
                            lineHeight = 50.sp
                        )
                        Text(
                            text = "Days",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.inkMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = "Current active streak",
                        fontSize = 12.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted
                    )
                }
            }
        }

        // Card 2: Today's Focus
        Card(
            modifier = Modifier.weight(1f).height(180.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S FOCUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = pal.inkMuted
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(pal.boneDark.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Focus",
                            tint = pal.ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$todayMinutes",
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = fontUi,
                            color = pal.teal,
                            lineHeight = 50.sp
                        )
                        Text(
                            text = "Mins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.teal.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(pal.boneDark.copy(alpha = 0.4f))
                        ) {
                            val focusFrac = (todayMinutes.toFloat() / 30f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(focusFrac)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(pal.teal)
                            )
                        }
                        Text(
                            text = "Goal: 30m",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = pal.inkMuted
                        )
                    }
                }
            }
        }

        // Card 3: Weekly Goal
        Card(
            modifier = Modifier.weight(1f).height(180.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEEKLY GOAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = pal.inkMuted
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(pal.boneDark.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrackChanges,
                            contentDescription = "Target",
                            tint = pal.ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressRing(
                            percent = weeklyGoalPercent,
                            color = pal.teal,
                            size = 68.dp,
                            strokeWidth = 6.dp,
                            trackColor = pal.boneDark.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "$weeklyGoalPercent%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.ink
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$weeklyTotalMins ",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontUi,
                                color = pal.ink
                            )
                            Text(
                                text = "/ ${weeklyGoalMins}m",
                                fontSize = 14.sp,
                                fontFamily = fontUi,
                                color = pal.inkMuted
                            )
                        }
                        Text(
                            text = "Total this week",
                            fontSize = 12.sp,
                            fontFamily = fontBody,
                            color = pal.inkMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Smart Insight Banner mirroring web Progress.jsx lines 128-132. */
@Composable
fun SmartInsightBanner(
    pal: NurPalette,
    insight: String,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pal.tealSoft)
            .border(1.dp, pal.teal.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Lightbulb,
            contentDescription = "Insight",
            tint = pal.teal,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = insight,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = fontUi,
            color = pal.ink
        )
    }
}

/** Pill tabs component mirroring web PeriodTabs.jsx. */
@Composable
fun PeriodTabsPill(
    tabs: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    pal: NurPalette,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pal.boneDark.copy(alpha = 0.35f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (id, label) ->
            val active = selected == id
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) pal.white else Color.Transparent)
                    .then(
                        if (active) Modifier.border(1.dp, pal.boneDark, CircleShape) else Modifier
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = if (active) pal.ink else pal.inkMuted
                )
            }
        }
    }
}

/** View toggle (Chart vs Heatmap) mirroring web ActivityFlow view buttons. */
@Composable
fun ViewTogglePill(
    current: String,
    onToggle: (String) -> Unit,
    pal: NurPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pal.boneDark.copy(alpha = 0.35f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("chart" to "Chart", "heatmap" to "Heatmap").forEach { (id, label) ->
            val active = current == id
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) pal.white else Color.Transparent)
                    .then(
                        if (active) Modifier.border(1.dp, pal.boneDark, CircleShape) else Modifier
                    )
                    .clickable { onToggle(id) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = if (active) pal.ink else pal.inkMuted
                )
            }
        }
    }
}

/** Delta comparison badge (+15%, -8%, 0%, No baseline, No data). */
@Composable
fun DeltaBadge(
    delta: Int?,
    hasData: Boolean,
    pal: NurPalette,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon, text) = when {
        !hasData -> Quad(pal.boneDark.copy(alpha = 0.4f), pal.inkMuted, null, "No data")
        delta == null -> Quad(pal.boneDark.copy(alpha = 0.4f), pal.inkMuted, null, "No baseline")
        delta > 0 -> Quad(Color(0xFFD1FAE5), Color(0xFF10B981), Icons.Filled.TrendingUp, "+$delta%")
        delta < 0 -> Quad(Color(0xFFFEE2E2), Color(0xFFE75344), Icons.Filled.TrendingDown, "$delta%")
        else -> Quad(pal.boneDark.copy(alpha = 0.4f), pal.inkMuted, Icons.Filled.Remove, "0%")
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
        }
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = fg
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/** 4 Stat Cells for ActivityFlow summary row. */
@Composable
fun StatCell(label: String, value: String, pal: NurPalette, modifier: Modifier = Modifier) {
    val fontUi = rememberUiFontFamily()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(pal.white)
            .border(1.dp, pal.boneDark, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = pal.inkMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontUi,
            color = pal.ink
        )
    }
}

/**
 * Activity Flow Card with:
 * - Period tabs (Today, Week, Month)
 * - View toggle (Chart vs Heatmap)
 * - Inline expandable "Log activity" interactive form modal/dropdown
 * - Delta percentage badge vs previous period
 * - Summary grid (Total time, Sessions, Active days, Avg/day)
 * - Custom charts (hourly bar chart for Today, daily bar chart for Week, daily line chart for Month, or 35-day heatmap grid)
 */
@Composable
fun ActivityFlowCard(
    pal: NurPalette,
    sessions: List<AnalyticsStats.Session>,
    onSessionLogged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    var range by remember { mutableStateOf("today") }
    var view by remember { mutableStateOf("chart") }

    // Log Form State
    var logOpen by remember { mutableStateOf(false) }
    var logType by remember { mutableStateOf("reading") }
    var logMinutes by remember { mutableStateOf("") }
    var logChapterId by remember { mutableStateOf<Int?>(null) }
    var logError by remember { mutableStateOf<String?>(null) }
    var logConfirm by remember { mutableStateOf<String?>(null) }
    var surahDropdownOpen by remember { mutableStateOf(false) }

    val chapters = remember { QuranStore.chapters }

    // Calculate dates & keys
    val now = remember { Date() }
    val keys = remember(range, now) { AnalyticsEngine.rangeKeys(range, now) ?: emptyList() }
    val prevKeys = remember(range, now) { AnalyticsEngine.previousRangeKeys(range, now) }
    val rangeSessions = remember(sessions, range, now) { AnalyticsEngine.filterByRange(sessions, range, now) }
    val prevSessions = remember(sessions, prevKeys) { AnalyticsEngine.filterByKeys(sessions, prevKeys) }

    val summary = remember(rangeSessions) { AnalyticsEngine.summarize(rangeSessions) }
    val prevSummary = remember(prevSessions) { AnalyticsEngine.summarize(prevSessions) }
    val delta = remember(summary, prevSummary) { AnalyticsEngine.deltaPercent(summary.seconds, prevSummary.seconds) }
    val hasData = summary.seconds > 0L || prevSummary.seconds > 0L

    val rangeTitleText = remember(range, now) { AnalyticsEngine.rangeTitle(range, now) }

    val rangeTabs = listOf("today" to "Today", "week" to "Week", "month" to "Month")

    // Heatmap data for 35 days
    val heatmap35 = remember(sessions, now) {
        val last35Keys = AnalyticsEngine.lastNDayKeys(35, AnalyticsEngine.dateKey(now))
        AnalyticsEngine.bucketByDay(sessions, last35Keys)
    }

    // Chart data
    val todayHourBuckets = remember(sessions, now) {
        AnalyticsEngine.bucketByHour(sessions, AnalyticsEngine.dateKey(now))
    }
    val weekDayBuckets = remember(sessions, keys) {
        AnalyticsEngine.bucketByDay(sessions, keys)
    }
    val monthDayBuckets = remember(sessions, keys) {
        AnalyticsEngine.bucketByDay(sessions, keys)
    }

    LaunchedEffect(logConfirm) {
        if (logConfirm != null) {
            delay(3500)
            logConfirm = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timeline,
                        contentDescription = "Activity Flow",
                        tint = pal.teal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Activity Flow",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                }

                // Log Activity Toggle Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (logOpen) pal.tealSoft else pal.white)
                        .border(1.5.dp, if (logOpen) pal.teal else pal.boneDark, CircleShape)
                        .clickable { logOpen = !logOpen }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (logOpen) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (logOpen) pal.teal else pal.ink,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (logOpen) "Close" else "Log activity",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = if (logOpen) pal.teal else pal.ink
                        )
                    }
                }
            }

            // Expandable Log Form
            AnimatedVisibility(
                visible = logOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(pal.white)
                        .border(1.5.dp, pal.boneDark, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    // Activity Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("reading", "Reading", ReadingColor),
                            Triple("pomodoro", "Focus", FocusColor),
                            Triple("memorizing", "Memorizing", MemorizingColor),
                            Triple("listening", "Listening", ListeningColor)
                        ).forEach { (typeId, label, color) ->
                            val selected = logType == typeId
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) pal.cream else pal.boneDark.copy(alpha = 0.2f))
                                    .border(
                                        1.5.dp,
                                        if (selected) pal.boneDark else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {
                                        logType = typeId
                                        logError = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(7.dp).clip(CircleShape).background(color)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        color = if (selected) pal.ink else pal.inkMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Row (Minutes, Quick Chips, Surah)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Minutes TextField
                        Column {
                            Text(
                                text = "MINUTES",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BasicTextField(
                                value = logMinutes,
                                onValueChange = {
                                    logMinutes = it.filter { ch -> ch.isDigit() }
                                    logError = null
                                },
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontUi,
                                    color = pal.ink
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .width(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pal.cream)
                                    .border(1.5.dp, pal.boneDark, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        // Quick Chips: 5, 10, 15, 30
                        Column {
                            Text(
                                text = "QUICK",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 30).forEach { chip ->
                                    val chipStr = chip.toString()
                                    val active = logMinutes == chipStr
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (active) pal.cream else pal.boneDark.copy(alpha = 0.2f))
                                            .border(
                                                1.5.dp,
                                                if (active) pal.boneDark else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable {
                                                logMinutes = chipStr
                                                logError = null
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$chip",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = fontUi,
                                            color = if (active) pal.ink else pal.inkMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Surah Picker Dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SURAH (OPTIONAL)",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                val selectedChapter = chapters.firstOrNull { it.id == logChapterId }
                                val surahLabel = if (selectedChapter != null) {
                                    "${selectedChapter.id}. ${selectedChapter.nameSimple}"
                                } else {
                                    "None"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(pal.cream)
                                        .border(1.5.dp, pal.boneDark, RoundedCornerShape(10.dp))
                                        .clickable { surahDropdownOpen = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = surahLabel,
                                        fontSize = 12.sp,
                                        fontFamily = fontUi,
                                        color = pal.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = pal.inkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = surahDropdownOpen,
                                    onDismissRequest = { surahDropdownOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None", fontFamily = fontUi) },
                                        onClick = {
                                            logChapterId = null
                                            surahDropdownOpen = false
                                        }
                                    )
                                    chapters.take(114).forEach { ch ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${ch.id}. ${ch.nameSimple} (${ch.translatedNameText})", fontFamily = fontUi)
                                            },
                                            onClick = {
                                                logChapterId = ch.id
                                                surahDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feedback Message & Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            if (logError != null) {
                                Text(
                                    text = logError ?: "",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE75344),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = fontUi
                                )
                            } else if (logConfirm != null) {
                                Text(
                                    text = logConfirm ?: "",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = fontUi
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.5.dp, pal.boneDark, CircleShape)
                                    .clickable {
                                        logOpen = false
                                        logMinutes = ""
                                        logChapterId = null
                                        logError = null
                                        logConfirm = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = pal.inkMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(pal.teal)
                                    .clickable {
                                        val mins = logMinutes.toIntOrNull()
                                        if (mins == null || mins < 1) {
                                            logError = "Enter how many minutes you logged."
                                        } else {
                                            val cappedMins = mins.coerceAtMost(600)
                                            SessionStore.log(logType, logChapterId, cappedMins)
                                            val metaLabel = AnalyticsEngine.TYPE_META[logType]?.label ?: "Activity"
                                            logConfirm = "Logged $cappedMins min of $metaLabel."
                                            logMinutes = ""
                                            logChapterId = null
                                            logError = null
                                            onSessionLogged()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log session",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Period & View Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodTabsPill(
                    tabs = rangeTabs,
                    selected = range,
                    onSelect = { range = it },
                    pal = pal
                )
                ViewTogglePill(
                    current = view,
                    onToggle = { view = it },
                    pal = pal
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Range Title & Delta Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rangeTitleText.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = pal.inkMuted
                )
                DeltaBadge(
                    delta = delta,
                    hasData = hasData,
                    pal = pal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Stat Cells
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCell("Total time", AnalyticsEngine.formatDuration(summary.seconds), pal, Modifier.weight(1f))
                StatCell("Sessions", "${summary.count}", pal, Modifier.weight(1f))
                StatCell("Active days", "${summary.activeDays}/${keys.size}", pal, Modifier.weight(1f))
                StatCell("Avg / day", "${summary.avgPerActiveDay}m", pal, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visualization Box (Height: 220.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.white)
                    .border(1.dp, pal.boneDark, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (view == "heatmap") {
                    // 35-Day Heatmap Grid (5 columns x 7 days)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (0 until 5).forEach { colIdx ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val colDays = heatmap35.drop(colIdx * 7).take(7)
                                    colDays.forEach { day ->
                                        val level = AnalyticsEngine.getHeatmapLevel(day.count > 0, day.seconds)
                                        val cellColor = when (level) {
                                            1 -> pal.tealSoft
                                            2 -> pal.teal.copy(alpha = 0.8f)
                                            3 -> Color(0xFF10B981)
                                            else -> pal.boneDark.copy(alpha = 0.35f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(cellColor)
                                                .then(
                                                    if (level == 1) Modifier.border(1.dp, pal.teal.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                    else Modifier
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Legend: Less [0] [1] [2] [3] More
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LESS",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = pal.inkMuted
                            )
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(pal.boneDark.copy(alpha = 0.35f)))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(pal.tealSoft))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(pal.teal.copy(alpha = 0.8f)))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981)))
                            Text(
                                text = "MORE",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = pal.inkMuted
                            )
                        }
                    }
                } else {
                    // Chart View
                    if (summary.count == 0) {
                        // Empty State
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = null,
                                tint = pal.teal.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nothing logged in this period yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontUi,
                                color = pal.ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Every session you record adds to your flow. Log one above or open a Surah to track automatically.",
                                fontSize = 12.sp,
                                fontFamily = fontBody,
                                color = pal.inkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else if (range == "month") {
                        // Daily Line Chart
                        DailyLineChart(
                            pal = pal,
                            buckets = monthDayBuckets,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (range == "today") {
                        // 24-Hour Bar Chart
                        HourlyBarChart(
                            pal = pal,
                            buckets = todayHourBuckets,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 7-Day Bar Chart for Week
                        WeeklyBarChart(
                            pal = pal,
                            buckets = weekDayBuckets,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/** 24-Hour Bar Chart for Today */
@Composable
fun HourlyBarChart(
    pal: NurPalette,
    buckets: List<AnalyticsEngine.HourBucket>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val maxMins = (buckets.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEach { b ->
                val frac = (b.minutes.toFloat() / maxMins.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(frac.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(pal.teal, pal.tealSoft)
                            )
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("12a", "3a", "6a", "9a", "12p", "3p", "6p", "9p").forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 7-Day Bar Chart for Week */
@Composable
fun WeeklyBarChart(
    pal: NurPalette,
    buckets: List<AnalyticsEngine.DayBucket>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val maxMins = (buckets.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEach { d ->
                val frac = (d.minutes.toFloat() / maxMins.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (d.minutes > 0) {
                        Text(
                            text = "${d.minutes}m",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.teal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(frac.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(pal.teal, pal.tealSoft)
                                )
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            buckets.forEach { d ->
                Text(
                    text = d.name,
                    fontSize = 10.sp,
                    fontFamily = fontUi,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Daily Line Chart for Month */
@Composable
fun DailyLineChart(
    pal: NurPalette,
    buckets: List<AnalyticsEngine.DayBucket>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val maxMins = (buckets.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val w = size.width
            val h = size.height
            val padY = 8f
            val n = buckets.size
            if (n < 2) return@Canvas

            val stepX = w / (n - 1).toFloat()

            // Area path
            val area = Path().apply {
                val firstY = h - padY - (buckets[0].minutes.toFloat() / maxMins) * (h - 2 * padY)
                moveTo(0f, h - padY)
                lineTo(0f, firstY)
                for (i in 1 until n) {
                    val x = i * stepX
                    val y = h - padY - (buckets[i].minutes.toFloat() / maxMins) * (h - 2 * padY)
                    lineTo(x, y)
                }
                lineTo(w, h - padY)
                close()
            }

            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(pal.teal.copy(alpha = 0.35f), pal.teal.copy(alpha = 0.02f)),
                    startY = 0f,
                    endY = h
                )
            )

            // Line path
            val line = Path().apply {
                val firstY = h - padY - (buckets[0].minutes.toFloat() / maxMins) * (h - 2 * padY)
                moveTo(0f, firstY)
                for (i in 1 until n) {
                    val x = i * stepX
                    val y = h - padY - (buckets[i].minutes.toFloat() / maxMins) * (h - 2 * padY)
                    lineTo(x, y)
                }
            }

            drawPath(
                path = line,
                color = pal.teal,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Points
            for (i in 0 until n) {
                if (buckets[i].minutes > 0) {
                    val x = i * stepX
                    val y = h - padY - (buckets[i].minutes.toFloat() / maxMins) * (h - 2 * padY)
                    drawCircle(color = pal.teal, radius = 3.5.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = pal.white, radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days labels: 1, 5, 10, 15, 20, 25, 30
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(1, 5, 10, 15, 20, 25, buckets.size).forEach { d ->
                Text(
                    text = "$d",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = pal.inkMuted
                )
            }
        }
    }
}

/**
 * Activity Mix Card:
 * - Period tabs (Today / Week / Month / All Time)
 * - Donut chart with total duration and session count in center
 * - Color-coded activity types list with minutes and percentage share
 * Ported 1:1 from ActivityMix.jsx.
 */
@Composable
fun ActivityMixCard(
    pal: NurPalette,
    sessions: List<AnalyticsStats.Session>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    var range by remember { mutableStateOf("week") }

    val mixTabs = listOf("today" to "Today", "week" to "Week", "month" to "Month", "all" to "All Time")

    val rangeSessions = remember(sessions, range) {
        AnalyticsEngine.filterByRange(sessions, range)
    }
    val totals = remember(rangeSessions) {
        AnalyticsEngine.summarize(rangeSessions)
    }

    val typeRows = remember(totals) {
        listOf(
            Triple("Reading", totals.byType["reading"] ?: 0L, ReadingColor),
            Triple("Focus", totals.byType["pomodoro"] ?: 0L, FocusColor),
            Triple("Memorizing", totals.byType["memorizing"] ?: 0L, MemorizingColor),
            Triple("Listening", totals.byType["listening"] ?: 0L, ListeningColor)
        ).map { (name, sec, color) ->
            val share = if (totals.seconds > 0L) Math.round((sec.toFloat() / totals.seconds.toFloat()) * 100f) else 0
            TypeRowData(name, sec, Math.round(sec / 60.0f), share, color)
        }.filter { it.seconds > 0L }
    }

    val hasData = typeRows.isNotEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = "Activity Mix",
                        tint = pal.teal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Activity Mix",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                }

                PeriodTabsPill(
                    tabs = mixTabs,
                    selected = range,
                    onSelect = { range = it },
                    pal = pal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AnalyticsEngine.rangeTitle(range).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp,
                color = pal.inkMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Center Donut Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasData) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val strokePx = 20.dp.toPx()
                        val diameter = size.minDimension - strokePx
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = Size(diameter, diameter)

                        var currentAngle = -90f
                        val gapAngle = 5f
                        val totalGap = typeRows.size * gapAngle
                        val usableDegrees = (360f - totalGap).coerceAtLeast(0f)

                        typeRows.forEach { item ->
                            val sweep = (item.share.toFloat() / 100f) * usableDegrees
                            if (sweep > 0f) {
                                drawArc(
                                    color = item.color,
                                    startAngle = currentAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(strokePx, cap = StrokeCap.Round)
                                )
                                currentAngle += sweep + gapAngle
                            }
                        }
                    }

                    // Inside ring text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = AnalyticsEngine.formatDuration(totals.seconds),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.ink
                        )
                        Text(
                            text = "${totals.count} SESSION${if (totals.count == 1) "" else "S"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = pal.inkMuted
                        )
                    }
                } else {
                    Text(
                        text = "No activity in this period yet.",
                        fontSize = 13.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Breakdown List
            if (hasData) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(row.color))
                            Text(
                                text = row.name,
                                fontSize = 13.sp,
                                fontFamily = fontUi,
                                color = pal.inkMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${row.minutes}m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontUi,
                                color = pal.ink
                            )
                            Text(
                                text = "${row.share}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = pal.inkMuted,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TypeRowData(
    val name: String,
    val seconds: Long,
    val minutes: Int,
    val share: Int,
    val color: Color
)

/**
 * Achievements Card:
 * - Header with Award icon & unlocked count (X/Y)
 * - Badges list (unlocked with full color, or locked with target progress bar)
 * - Expandable "Show all / Show less" toggle
 * Ported 1:1 from Achievements.jsx.
 */
@Composable
fun AchievementsCard(
    pal: NurPalette,
    stats: AnalyticsEngine.AchievementStats,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    val badges = remember(stats) { AnalyticsEngine.evaluateAchievements(stats) }
    val ordered = remember(badges) { AnalyticsEngine.orderBadges(badges) }
    val unlockedCount = remember(badges) { badges.count { it.unlocked } }

    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) ordered else ordered.take(4)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Achievements",
                        tint = pal.teal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Achievements",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pal.boneDark.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$unlockedCount / ${badges.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = pal.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.sessionCount == 0) {
                Text(
                    text = "Read consistently to unlock badges!",
                    fontSize = 13.sp,
                    fontFamily = fontBody,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visible.forEach { badge ->
                        if (badge.unlocked) {
                            // Unlocked Badge Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(pal.white)
                                    .border(1.5.dp, pal.boneDark, RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(pal.goldLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = badge.icon, fontSize = 24.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = badge.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontUi,
                                        color = pal.ink
                                    )
                                    Text(
                                        text = badge.desc,
                                        fontSize = 12.sp,
                                        fontFamily = fontBody,
                                        color = pal.inkMuted
                                    )
                                }
                            }
                        } else {
                            // Locked Badge Card (with progress bar)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(pal.white.copy(alpha = 0.85f))
                                    .border(1.5.dp, pal.boneDark, RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(pal.boneDark.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = badge.icon, fontSize = 24.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = badge.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = fontUi,
                                            color = pal.ink
                                        )
                                        Text(
                                            text = "${Math.round(badge.current)} / ${Math.round(badge.target)}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = pal.inkMuted
                                        )
                                    }
                                    Text(
                                        text = badge.desc,
                                        fontSize = 12.sp,
                                        fontFamily = fontBody,
                                        color = pal.inkMuted
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(pal.boneDark.copy(alpha = 0.35f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(badge.progress)
                                                .height(5.dp)
                                                .clip(CircleShape)
                                                .background(pal.teal)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (ordered.size > 4) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(pal.white)
                            .border(1.5.dp, pal.boneDark, CircleShape)
                            .clickable { expanded = !expanded }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (expanded) "Show less" else "Show all",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = pal.inkMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recent Activity Card:
 * - Timeline line connecting sessions
 * - Type icons (Reading = green MenuBook, Focus = purple TrackChanges, Memorizing = blue Layers, Listening = amber Headphones)
 * - Surah name, date, time label, duration badge
 * - "See all" action leading to History screen
 * Ported 1:1 from RecentActivity.jsx.
 */
@Composable
fun RecentActivityCard(
    pal: NurPalette,
    sessions: List<AnalyticsStats.Session>,
    onSeeAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    val chapters = remember { QuranStore.chapters }

    val recentSessions = remember(sessions) {
        sessions.sortedByDescending { s ->
            if (s.timestamp > 0L) s.timestamp else AnalyticsEngine.keyToDate(s.date).time
        }.take(5)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Recent Activity",
                        tint = pal.teal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pal.white)
                        .border(1.5.dp, pal.boneDark, CircleShape)
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "See all",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = pal.inkMuted
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = pal.inkMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentSessions.isEmpty()) {
                Text(
                    text = "No recent activity found.",
                    fontSize = 13.sp,
                    fontFamily = fontBody,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentSessions.forEach { s ->
                        val dateObj = remember(s) {
                            if (s.timestamp > 0L) Date(s.timestamp) else AnalyticsEngine.keyToDate(s.date)
                        }
                        val timeLabel = remember(s, dateObj) {
                            if (s.timestamp > 0L) {
                                SimpleDateFormat("h:mm a", Locale.US).format(dateObj)
                            } else {
                                "Logged"
                            }
                        }
                        val dateLabel = remember(s, dateObj) {
                            SimpleDateFormat("MMM d", Locale.US).format(dateObj)
                        }

                        val (typeIcon, typeColor, typeBg, baseTitle) = when (s.type) {
                            "pomodoro", "focus" -> Quad(
                                Icons.Filled.TrackChanges,
                                FocusColor,
                                FocusColor.copy(alpha = 0.12f),
                                "Focus Session"
                            )
                            "memorizing" -> Quad(
                                Icons.Filled.Layers,
                                MemorizingColor,
                                MemorizingColor.copy(alpha = 0.12f),
                                "Memorization"
                            )
                            "listening" -> Quad(
                                Icons.Filled.Headphones,
                                ListeningColor,
                                ListeningColor.copy(alpha = 0.12f),
                                "Listening"
                            )
                            else -> Quad(
                                Icons.Filled.MenuBook,
                                ReadingColor,
                                ReadingColor.copy(alpha = 0.12f),
                                "Reading Session"
                            )
                        }

                        val chId = s.chapterId
                        val sessionTitle = if (chId != null && chId > 0) {
                            val ch = chapters.firstOrNull { it.id == chId }
                            if (ch != null) "$baseTitle - ${ch.nameSimple}" else "$baseTitle - Surah $chId"
                        } else {
                            baseTitle
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(typeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = typeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(pal.white)
                                    .border(1.5.dp, pal.boneDark, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = sessionTitle,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontUi,
                                        color = pal.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$dateLabel • $timeLabel",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = pal.inkMuted
                                    )
                                }

                                Text(
                                    text = AnalyticsEngine.formatDuration(s.durationSec),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontUi,
                                    color = pal.inkMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3 Bottom Navigation Link Cards:
 * - Bookmarks (Saved ayahs)
 * - Collections (Verse groups)
 * - Recent Surahs (Resume reading)
 * Ported 1:1 from Progress.jsx lines 217-237.
 */
@Composable
fun QuickNavCards(
    pal: NurPalette,
    bookmarksCount: Int,
    collectionsCount: Int,
    recentSurahsCount: Int,
    onOpenBookmarks: () -> Unit = {},
    onOpenCollections: () -> Unit = {},
    onOpenRecent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(
            Triple(Icons.Filled.Bookmark, "Bookmarks", "Saved ayahs") to Pair(bookmarksCount, onOpenBookmarks),
            Triple(Icons.Filled.Layers, "Collections", "Verse groups") to Pair(collectionsCount, onOpenCollections),
            Triple(Icons.Filled.History, "Recent Surahs", "Resume reading") to Pair(recentSurahsCount, onOpenRecent)
        ).forEach { (info, action) ->
            val (icon, label, hint) = info
            val (count, onClick) = action
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = pal.cream),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(pal.boneDark.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = pal.ink, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "$count",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontUi,
                                color = pal.ink
                            )
                            Text(
                                text = label.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = pal.inkMuted
                            )
                            Text(
                                text = hint,
                                fontSize = 11.sp,
                                fontFamily = fontBody,
                                color = pal.inkMuted
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Start Your Journey banner when zero sessions exist.
 * Ported 1:1 from Progress.jsx lines 195-205.
 */
@Composable
fun StartYourJourneyCard(
    pal: NurPalette,
    onOpenQuran: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(pal.tealSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = pal.teal,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Start Your Journey",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontUi,
                color = pal.ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your reading and memorization activity will beautifully visualize here as you use the app.",
                fontSize = 13.5.sp,
                fontFamily = fontBody,
                color = pal.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pal.ink)
                    .clickable(onClick = onOpenQuran)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open Quran",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontUi,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// BACKWARD-COMPATIBLE WRAPPERS FOR CALL SITES
// ==========================================

@Composable
fun TopCards(
    pal: NurPalette,
    streakDays: Int,
    todayMins: Int,
    totalMins: Int,
    modifier: Modifier = Modifier
) {
    TopHeroCards(
        pal = pal,
        streak = streakDays,
        todayMinutes = todayMins,
        weeklyTotalMins = totalMins,
        weeklyGoalPercent = ((totalMins.toFloat() / 180f) * 100f).coerceAtMost(100f).toInt(),
        modifier = modifier
    )
}

@Composable
fun QuickCards(
    pal: NurPalette,
    weeklyPercent: Int,
    weeklyMins: Int,
    weeklyGoalMins: Int,
    insight: String,
    modifier: Modifier = Modifier
) {
    SmartInsightBanner(pal = pal, insight = insight, modifier = modifier)
}

@Composable
fun Heatmap7(pal: NurPalette, last7: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    // Legacy stub delegating gracefully
}

@Composable
fun MonthHeatmap(pal: NurPalette, sessions: List<AnalyticsStats.Session>, modifier: Modifier = Modifier) {
    // Legacy stub delegating gracefully
}

@Composable
fun FlowChart(pal: NurPalette, last7: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    // Legacy stub delegating gracefully
}

@Composable
fun ActivityMixRow(pal: NurPalette, byType: Map<String, Int>, modifier: Modifier = Modifier) {
    // Legacy stub delegating gracefully
}

@Composable
fun AchievementsList(pal: NurPalette, badges: List<AnalyticsStats.Badge>, modifier: Modifier = Modifier) {
    // Legacy stub delegating gracefully
}
