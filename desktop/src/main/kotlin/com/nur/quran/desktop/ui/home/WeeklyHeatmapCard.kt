package com.nur.quran.desktop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data model for a single day in the weekly heatmap.
 */
data class WeekDayData(
    val label: String,
    val mins: Long,
    val isToday: Boolean
)

/**
 * Desktop Weekly Heatmap Card mirroring web `Home.jsx` (lines 141-154, 270-299).
 * Displays a 7-day bar chart showing reading/memorization activity for the past 7 days,
 * scaled to the maximum day in the week with 4 color levels (low, med, high, max).
 */
@Composable
fun WeeklyHeatmapCard(pal: NurPalette, modifier: Modifier = Modifier) {
    val fontUi = rememberUiFontFamily()
    val sessions = remember { SessionStore.all() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val dayLabels = remember { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }

    val weekData = remember(sessions) {
        val today = LocalDate.now()
        val result = mutableListOf<WeekDayData>()
        for (i in 6 downTo 0) {
            val d = today.minusDays(i.toLong())
            val ds = d.format(dateFmt)
            val dayMins = sessions.filter { it.date == ds }.sumOf { it.durationSec } / 60
            val label = dayLabels[d.dayOfWeek.value % 7]
            val isToday = (i == 0)
            result.add(WeekDayData(label = label, mins = dayMins, isToday = isToday))
        }
        result
    }

    val weekMax = remember(weekData) {
        maxOf(weekData.maxOfOrNull { it.mins } ?: 1L, 1L)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.5.dp, pal.boneDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            // Header: BarChart icon + "This Week"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = "This Week",
                    tint = pal.ink,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "This Week",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7 Days Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekData.forEach { day ->
                    val pct = if (weekMax > 0) ((day.mins * 100) / weekMax).toInt() else 0
                    val level = when {
                        pct > 80 -> "max"
                        pct > 50 -> "high"
                        pct > 20 -> "med"
                        else -> "low"
                    }

                    val fillColor = when (level) {
                        "max" -> pal.green
                        "high" -> pal.teal
                        "med" -> pal.tealSoft
                        else -> pal.boneDark
                    }

                    val heightPct = if (day.mins > 0) maxOf(15, pct) else 0

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Day Label (e.g. MON, TUE)
                        Text(
                            text = day.label.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) pal.teal else pal.inkMuted,
                            letterSpacing = 0.5.sp
                        )

                        // Bar Track (32dp height, bottom-aligned fill)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(pal.bone),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (day.mins > 0) {
                                val barModifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(heightPct / 100f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(fillColor)
                                    .then(
                                        if (level == "med") {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = pal.teal.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        } else Modifier
                                    )
                                Box(modifier = barModifier)
                            }
                        }

                        // Minutes label (e.g. 15m)
                        Text(
                            text = "${day.mins}m",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) pal.teal else pal.inkMuted
                        )
                    }
                }
            }
        }
    }
}
