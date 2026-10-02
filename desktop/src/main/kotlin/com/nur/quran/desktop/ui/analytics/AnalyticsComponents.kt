package com.nur.quran.desktop.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats

private val ReadingColor = Color(0xFF10B981)
private val MemorizingColor = Color(0xFF3B82F6)
private val FocusColor = Color(0xFF8B5CF6)
private val ListeningColor = Color(0xFFF59E0B)

/** Heatmap bar color mirroring Android levels (gold soft → gold → emerald). */
private fun heatColor(pal: NurPalette, mins: Int): Color = when (
    AnalyticsStats.heatmapLevel(mins > 0, mins * 60L)
) {
    1 -> pal.goldSoft
    2 -> pal.gold.copy(alpha = 0.8f)
    3 -> pal.green
    else -> pal.bone
}

/**
 * Top metric cards: streak days / today minutes / total time.
 * Teal/gold accents on cream cards, like Android ConsistencyCard/TodayFocusCard.
 */
@Composable
fun TopCards(
    pal: NurPalette,
    streakDays: Int,
    todayMins: Int,
    totalMins: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricMiniCard(
            pal = pal,
            label = "STREAK",
            value = "$streakDays",
            unit = "days",
            emoji = "🔥",
            accent = if (streakDays > 0) Color(0xFFE75344) else pal.inkMuted,
            modifier = Modifier.weight(1f)
        )
        MetricMiniCard(
            pal = pal,
            label = "TODAY",
            value = "$todayMins",
            unit = "min",
            emoji = "⏱️",
            accent = pal.gold,
            modifier = Modifier.weight(1f)
        )
        MetricMiniCard(
            pal = pal,
            label = "TOTAL",
            value = AnalyticsStats.formatMinutes(totalMins * 60L),
            unit = "all time",
            emoji = "📖",
            accent = pal.teal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricMiniCard(
    pal: NurPalette,
    label: String,
    value: String,
    unit: String,
    emoji: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = pal.inkMuted
                )
                Text(text = emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = fontUi,
                color = accent,
                maxLines = 1
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                fontFamily = fontUi,
                color = pal.inkMuted
            )
        }
    }
}

/**
 * Weekly goal ring (Canvas arc) + smart insight text,
 * mirroring Android WeeklyGoalCard + SmartInsightBanner.
 */
@Composable
fun QuickCards(
    pal: NurPalette,
    weeklyPercent: Int,
    weeklyMins: Int,
    weeklyGoalMins: Int,
    insight: String,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                    val track = pal.boneDark
                    val gold = pal.gold
                    val frac = (weeklyPercent.toFloat() / 100f).coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.size(84.dp)) {
                        val stroke = 10.dp.toPx()
                        drawArc(
                            color = track,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        if (frac > 0f) {
                            drawArc(
                                color = gold,
                                startAngle = -90f,
                                sweepAngle = 360f * frac,
                                useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = "$weeklyPercent%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WEEKLY GOAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = pal.inkMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$weeklyMins / ${weeklyGoalMins}m",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                    Text(
                        text = "Total this week",
                        fontSize = 12.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted
                    )
                }
            }
            if (insight.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(pal.goldSoft)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "💡", fontSize = 16.sp)
                    Text(
                        text = insight,
                        fontSize = 13.sp,
                        fontFamily = fontBody,
                        color = pal.ink
                    )
                }
            }
        }
    }
}

/**
 * Last-7-days bars colored by [AnalyticsStats.heatmapLevel],
 * mirroring the Android heatmap level palette.
 */
@Composable
fun Heatmap7(
    pal: NurPalette,
    last7: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val maxMins = (last7.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📊  ACTIVITY — LAST 7 DAYS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                last7.forEach { (label, mins) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = if (mins > 0) "${mins}m" else "",
                            fontSize = 10.sp,
                            fontFamily = fontUi,
                            color = pal.inkMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val frac = mins.toFloat() / maxMins.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((110f * frac).coerceAtLeast(6f).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(heatColor(pal, mins))
                                .border(
                                    1.dp,
                                    if (mins > 0) pal.gold.copy(alpha = 0.25f) else pal.boneDark,
                                    RoundedCornerShape(6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = fontUi,
                            color = pal.inkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 30-day activity grid ("This month"): small day cells in rows of 7,
 * colored with the same [AnalyticsStats.heatmapLevel] mapping as [Heatmap7].
 * Empty months still render the full grid (no zero-state special casing).
 */
@Composable
fun MonthHeatmap(
    pal: NurPalette,
    sessions: List<AnalyticsStats.Session>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val days = remember(sessions) {
        val keyFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val numFmt = java.text.SimpleDateFormat("d", java.util.Locale.US)
        val todayStr = keyFmt.format(java.util.Date())
        (29 downTo 0).map { i ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DATE, -i)
            val dateStr = keyFmt.format(cal.time)
            MonthDay(
                dateStr = dateStr,
                dayNum = numFmt.format(cal.time),
                mins = AnalyticsStats.dayMinutes(sessions, dateStr),
                isToday = dateStr == todayStr
            )
        }
    }
    val totalMins = remember(days) { days.sumOf { it.mins } }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "\uD83D\uDCC5  THIS MONTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = pal.inkMuted
                )
                Text(
                    text = "${totalMins}m total",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontUi,
                    color = pal.ink
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            // Chunked Rows (not LazyVerticalGrid) to avoid nested-scroll issues
            // inside the parent LazyColumn.
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { day ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(heatColor(pal, day.mins))
                                .border(
                                    if (day.isToday) 2.dp else 1.dp,
                                    if (day.isToday) pal.gold
                                    else if (day.mins > 0) pal.gold.copy(alpha = 0.25f)
                                    else pal.boneDark,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = day.dayNum,
                                fontSize = 10.sp,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = fontUi,
                                color = if (day.mins > 0) Color.White else pal.inkMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private data class MonthDay(
    val dateStr: String,
    val dayNum: String,
    val mins: Int,
    val isToday: Boolean
)

/**
 * Cumulative flow (area + line) of the week's minutes — mirrors the Android
 * AnalyticsFlowChart. Empty weeks render a muted placeholder line instead.
 */
@Composable
fun FlowChart(
    pal: NurPalette,
    last7: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📈  WEEKLY FLOW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (last7.all { it.second <= 0 }) {
                Text(
                    text = "No activity this week",
                    fontSize = 13.sp,
                    fontFamily = fontUi,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                )
            } else {
                // Cumulative totals per day (flow view of progress).
                var run = 0
                val cum = last7.map { (label, mins) ->
                    run += mins
                    label to run
                }
                val maxCum = cum.last().second.coerceAtLeast(1)
                val points = cum.mapIndexed { i, _ ->
                    val x = if (cum.size == 1) 0.5f else i.toFloat() / (cum.size - 1).toFloat()
                    val frac = cum[i].second.toFloat() / maxCum.toFloat()
                    x to frac
                }
                Column {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val pad = 6f
                        fun px(i: Int) = points[i].first * (w - 2 * pad) + pad
                        fun py(f: Float) = (h - pad) - f * (h - 2 * pad)
                        // Area fill
                        val area = androidx.compose.ui.graphics.Path().apply {
                            moveTo(px(0), h - pad)
                            points.forEachIndexed { i, _ -> lineTo(px(i), py(points[i].second)) }
                            lineTo(px(points.size - 1), h - pad)
                            close()
                        }
                        drawPath(
                            area,
                            color = pal.tealSoft
                        )
                        // Line
                        val line = androidx.compose.ui.graphics.Path().apply {
                            moveTo(px(0), py(points[0].second))
                            points.drop(1).forEachIndexed { i, _ ->
                                lineTo(px(i + 1), py(points[i + 1].second))
                            }
                        }
                        drawPath(
                            line,
                            color = pal.teal,
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )
                        // Dots
                        points.forEachIndexed { i, p ->
                            drawCircle(
                                color = pal.teal,
                                radius = 4f,
                                center = androidx.compose.ui.geometry.Offset(px(i), py(p.second))
                            )
                            drawCircle(
                                color = pal.cream,
                                radius = 2f,
                                center = androidx.compose.ui.geometry.Offset(px(i), py(p.second))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        cum.forEach { (label, mins) ->
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontFamily = fontUi,
                                color = pal.inkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        cum.forEach { (_, mins) ->
                            Text(
                                text = "${mins}m",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = pal.gold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minutes breakdown by reading / memorizing / focus / listening
 * with proportional gold/teal bars, mirroring Android ActivityMix.
 */
@Composable
fun ActivityMixRow(
    pal: NurPalette,
    byType: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val rows = listOf(
        Triple("Reading", byType["reading"] ?: 0, ReadingColor),
        Triple("Memorizing", byType["memorizing"] ?: 0, MemorizingColor),
        Triple("Focus", byType["focus"] ?: 0, FocusColor),
        Triple("Listening", byType["listening"] ?: 0, ListeningColor)
    )
    val total = rows.sumOf { it.second }.coerceAtLeast(1)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🧩  ACTIVITY MIX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (rows.all { it.second <= 0 }) {
                Text(
                    text = "No activity data yet.",
                    fontSize = 13.sp,
                    fontFamily = fontUi,
                    color = pal.inkMuted
                )
            } else {
                rows.filter { it.second > 0 }.forEach { (name, mins, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontFamily = fontUi,
                            color = pal.inkMuted,
                            modifier = Modifier.width(92.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(pal.bone)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((mins.toFloat() / total.toFloat()).coerceIn(0.02f, 1f))
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                        Text(
                            text = "${mins}m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.ink
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unlocked achievement badges (emoji + title + desc),
 * mirroring Android AchievementsSection.
 */
@Composable
fun AchievementsList(
    pal: NurPalette,
    badges: List<AnalyticsStats.Badge>,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🏆  ACHIEVEMENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (badges.isEmpty()) {
                Text(
                    text = "Read consistently to unlock badges!",
                    fontSize = 13.sp,
                    fontFamily = fontBody,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    badges.forEach { badge ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(pal.white)
                                .border(1.5.dp, pal.boneDark, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(pal.goldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = badge.icon, fontSize = 22.sp)
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
                    }
                }
            }
        }
    }
}
