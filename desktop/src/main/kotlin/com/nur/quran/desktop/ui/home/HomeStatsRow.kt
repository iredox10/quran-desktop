package com.nur.quran.desktop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats

/**
 * Compact 3-up stats strip for HomeScreen: streak / today / all-time.
 * Cream cards with boneDark border and 20dp corners, mirroring analytics TopCards.
 * Zero-state friendly: shows 0s, never blank.
 */
@Composable
fun HomeStatsRow(pal: NurPalette, modifier: Modifier = Modifier) {
    val sessions = remember { SessionStore.all() }
    val statsSessions = remember(sessions) {
        sessions.map {
            AnalyticsStats.Session(
                date = it.date,
                durationSec = it.durationSec,
                type = it.type,
                chapterId = it.chapterId
            )
        }
    }
    val streakDays = remember(statsSessions) {
        AnalyticsStats.streak(statsSessions.map { it.date }.toSet())
    }
    val todayMins = remember(statsSessions) {
        AnalyticsStats.todayMinutes(statsSessions)
    }
    val allTimeMins = remember(statsSessions) {
        AnalyticsStats.allTimeMinutes(statsSessions)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeStatMiniCard(
            pal = pal,
            label = "STREAK",
            value = "$streakDays",
            unit = "days",
            emoji = "🔥",
            accent = if (streakDays > 0) Color(0xFFE75344) else pal.inkMuted,
            modifier = Modifier.weight(1f)
        )
        HomeStatMiniCard(
            pal = pal,
            label = "TODAY",
            value = "$todayMins",
            unit = "min",
            emoji = "⏱️",
            accent = pal.gold,
            modifier = Modifier.weight(1f)
        )
        HomeStatMiniCard(
            pal = pal,
            label = "TOTAL",
            value = AnalyticsStats.formatMinutes(allTimeMins * 60L),
            unit = "all time",
            emoji = "📖",
            accent = pal.teal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeStatMiniCard(
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
        border = BorderStroke(1.5.dp, pal.boneDark)
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
