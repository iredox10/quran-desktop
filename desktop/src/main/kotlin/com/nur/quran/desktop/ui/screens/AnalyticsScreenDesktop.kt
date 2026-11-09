package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.BookmarkStore
import com.nur.quran.desktop.data.CollectionStore
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.data.SessionStore
import com.nur.quran.desktop.ui.analytics.AchievementsCard
import com.nur.quran.desktop.ui.analytics.ActivityFlowCard
import com.nur.quran.desktop.ui.analytics.ActivityMixCard
import com.nur.quran.desktop.ui.analytics.AnalyticsEngine
import com.nur.quran.desktop.ui.analytics.QuickNavCards
import com.nur.quran.desktop.ui.analytics.RecentActivityCard
import com.nur.quran.desktop.ui.analytics.SmartInsightBanner
import com.nur.quran.desktop.ui.analytics.StartYourJourneyCard
import com.nur.quran.desktop.ui.analytics.TopHeroCards
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.nav.DesktopRoutes
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.AnalyticsStats
import java.util.Calendar

/**
 * Desktop Analytics dashboard — 1:1 port of web app's `Progress.jsx` page.
 *
 * Sections:
 * 1. Header with dynamic time-of-day greeting & Smart Insight banner.
 * 2. 3 Top Hero Metric Cards: Consistency (streak), Today's Focus, and Weekly Goal ring.
 * 3. Start Your Journey banner (when zero sessions logged).
 * 4. 2-Column Wide Grid: Activity Flow (with period tabs, chart/heatmap switch, and inline log activity)
 *    and Activity Mix (donut breakdown with center duration & shares).
 * 5. 2-Column Wide Grid: Achievements (unlocked/locked target progress) and Recent Activity (timeline list).
 * 6. 3 Bottom Quick Navigation Cards: Bookmarks, Collections, and Recent Surahs.
 */
@Composable
fun AnalyticsScreenDesktop(
    pal: NurPalette,
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onOpenSurah: (Int) -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    var refreshKey by remember { mutableStateOf(0) }

    val rawSessions = remember(refreshKey) { SessionStore.all() }
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

    val bookmarksCount = remember(refreshKey) {
        runCatching { BookmarkStore.all().size }.getOrDefault(0)
    }
    val collectionsCount = remember(refreshKey) {
        runCatching { CollectionStore.listCollections().size }.getOrDefault(0)
    }
    val recentEntries = remember(refreshKey) {
        RecentlyReadStore.all()
    }
    val recentlyReadIds = remember(recentEntries) {
        recentEntries.map { it.chapterId }
    }

    val now = remember { java.util.Date() }
    val todayKey = remember(now) { AnalyticsEngine.dateKey(now) }
    val last7Keys = remember(now) { AnalyticsEngine.lastNDayKeys(7, todayKey) }

    val achievementStats = remember(sessions, recentlyReadIds, now) {
        AnalyticsEngine.computeAchievementStats(sessions, recentlyReadIds, now)
    }

    val streak = achievementStats.currentStreak.toInt()
    val todayTotalSec = remember(sessions, todayKey) {
        sessions.filter { it.date == todayKey }.sumOf { it.durationSec }
    }
    val todayMinutes = Math.round(todayTotalSec / 60.0f)

    val weeklyGoalMins = 180
    val weeklyTotalMins = remember(sessions, last7Keys) {
        Math.round(last7Keys.sumOf { k -> sessions.filter { it.date == k }.sumOf { it.durationSec } } / 60.0f)
    }
    val weeklyGoalPercent = ((weeklyTotalMins.toFloat() / weeklyGoalMins.toFloat()) * 100f).coerceAtMost(100f).toInt()

    val smartInsight = remember(sessions) {
        AnalyticsStats.smartInsight(sessions)
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val hasData = sessions.isNotEmpty()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(pal.white)
            .dragToScroll(listState),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Constrained Content Container (max width 1160.dp matching web max-w-[1200px])
        item {
            Column(
                modifier = Modifier.widthIn(max = 1160.dp).fillMaxWidth()
            ) {
                // Header Row with Back Button & Centered Title
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
                            text = "ANALYTICS DASHBOARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = pal.inkMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your Progress",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontUi,
                            color = pal.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$greeting. Here's the story of your consistency.",
                            fontSize = 14.sp,
                            fontFamily = fontBody,
                            color = pal.inkMuted
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Balance space for back button
                    Spacer(modifier = Modifier.size(38.dp))
                }

                if (hasData) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SmartInsightBanner(pal = pal, insight = smartInsight)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Top 3 Hero Metric Cards
                TopHeroCards(
                    pal = pal,
                    streak = streak,
                    todayMinutes = todayMinutes,
                    weeklyTotalMins = weeklyTotalMins,
                    weeklyGoalMins = weeklyGoalMins,
                    weeklyGoalPercent = weeklyGoalPercent
                )

                if (!hasData) {
                    Spacer(modifier = Modifier.height(20.dp))
                    StartYourJourneyCard(
                        pal = pal,
                        onOpenQuran = { onNavigate(DesktopRoutes.HOME) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2-Column Grid: Activity Flow & Activity Mix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActivityFlowCard(
                        pal = pal,
                        sessions = sessions,
                        onSessionLogged = { refreshKey++ },
                        modifier = Modifier.weight(1f)
                    )
                    ActivityMixCard(
                        pal = pal,
                        sessions = sessions,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2-Column Grid: Achievements & Recent Activity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AchievementsCard(
                        pal = pal,
                        stats = achievementStats,
                        modifier = Modifier.weight(1f)
                    )
                    RecentActivityCard(
                        pal = pal,
                        sessions = sessions,
                        onSeeAll = { onNavigate(DesktopRoutes.HISTORY) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Bottom Quick Navigation Link Cards
                QuickNavCards(
                    pal = pal,
                    bookmarksCount = bookmarksCount,
                    collectionsCount = collectionsCount,
                    recentSurahsCount = recentEntries.size,
                    onOpenBookmarks = { onNavigate(DesktopRoutes.LIBRARY) },
                    onOpenCollections = { onNavigate(DesktopRoutes.LIBRARY) },
                    onOpenRecent = { onNavigate(DesktopRoutes.HISTORY) }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
