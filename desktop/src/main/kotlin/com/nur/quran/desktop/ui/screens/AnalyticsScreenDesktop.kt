package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.analytics.AchievementsList
import com.nur.quran.desktop.ui.analytics.ActivityMixRow
import com.nur.quran.desktop.ui.analytics.Heatmap7
import com.nur.quran.desktop.ui.analytics.QuickCards
import com.nur.quran.desktop.ui.analytics.TopCards
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Desktop analytics dashboard, mirroring Android AnalyticsScreen sections
 * top-to-bottom: TopCards → QuickCards (goal ring + insight) → Heatmap →
 * ActivityMix → AchievementsRecent. Stats delegate to shared [AnalyticsStats].
 */
@Composable
fun AnalyticsScreenDesktop(pal: NurPalette, onBack: () -> Unit = {}) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    val sessions = remember { SessionStore.all() }
    val lastRead = remember { PrefsCache.getLastRead() }

    val stats = remember(sessions) {
        sessions.map {
            AnalyticsStats.Session(
                date = it.date,
                durationSec = it.durationSec,
                type = it.type,
                chapterId = it.chapterId
            )
        }
    }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val last7Keys = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        (6 downTo 0).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -i)
            sdf.format(cal.time)
        }
    }

    val last7 = remember(stats) { AnalyticsStats.last7Days(stats) }
    val streak = remember(stats) { AnalyticsStats.streak(stats.map { it.date }.toSet()) }
    val todayMins = remember(stats, todayStr) { AnalyticsStats.todayMinutes(stats, todayStr) }
    val allTimeMins = remember(stats) { AnalyticsStats.allTimeMinutes(stats) }
    val byType = remember(stats) { AnalyticsStats.minutesByType(stats) }
    val weeklyGoalMins = 180
    val weeklyMins = remember(stats, last7Keys) {
        AnalyticsStats.weeklyTotalMinutes(stats, last7Keys)
    }
    val weeklyPercent = remember(weeklyMins) {
        AnalyticsStats.weeklyGoalPercent(weeklyMins, weeklyGoalMins)
    }
    val insight = remember(stats) { AnalyticsStats.smartInsight(stats) }
    val badges = remember(streak, allTimeMins, sessions, lastRead) {
        AnalyticsStats.achievements(
            streakDays = streak,
            allTimeMins = allTimeMins,
            sessionChapterIds = sessions.map { it.chapterId },
            recentlyReadIds = listOfNotNull(lastRead?.chapterId)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pal.white),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = pal.ink
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Analytics",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi,
                        color = pal.ink
                    )
                    Text(
                        text = "Your progress at a glance",
                        fontSize = 13.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted
                    )
                }
            }
        }

        item {
            TopCards(
                pal = pal,
                streakDays = streak,
                todayMins = todayMins,
                totalMins = allTimeMins
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = pal.teal),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.tealMid)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🌙", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Start Your Journey",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Read, memorize, or plan to start tracking.",
                            fontSize = 14.sp,
                            fontFamily = fontBody,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                QuickCards(
                    pal = pal,
                    weeklyPercent = weeklyPercent,
                    weeklyMins = weeklyMins,
                    weeklyGoalMins = weeklyGoalMins,
                    insight = insight
                )
            }
            item {
                Heatmap7(pal = pal, last7 = last7)
            }
            item {
                ActivityMixRow(pal = pal, byType = byType)
            }
            item {
                AchievementsList(pal = pal, badges = badges)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
