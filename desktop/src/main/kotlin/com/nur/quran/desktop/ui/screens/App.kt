package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.nur.quran.desktop.ui.input.dragToScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.shared.FsrsCard
import com.nur.quran.shared.HifdhStore
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.data.DeskVerse
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.DesktopFonts
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.desktop.ui.components.SettingsDrawerDesktop
import com.nur.quran.desktop.ui.components.TranslationTextDesktop
import com.nur.quran.desktop.ui.components.copyToClipboard
import com.nur.quran.desktop.ui.home.BrowseItem
import com.nur.quran.desktop.ui.input.AppShortcuts
import com.nur.quran.desktop.ui.input.ShortcutHelpDialog
import com.nur.quran.desktop.ui.home.GlobalSearch
import com.nur.quran.desktop.ui.home.HomeStatsRow
import com.nur.quran.desktop.ui.home.WeeklyHeatmapCard
import com.nur.quran.desktop.ui.home.RecentlyReadRow
import com.nur.quran.desktop.ui.home.buildBrowseItems
import com.nur.quran.desktop.ui.home.filterBrowseItems
import com.nur.quran.desktop.ui.nav.AppSidebar
import com.nur.quran.desktop.ui.nav.DesktopRoutes
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.desktop.ui.theme.getGreeting
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.desktop.ui.theme.timeAgo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App root with simple state-based navigation (no navigation library).
 *
 * Routes (plain strings):
 * - "home"                 : homepage
 * - "memorize" / "memorize/{id}" : memorization index + hifdh reader
 * - "planner" / "planner_reader/{yyyy-MM-dd}" : reading plans
 * - "analytics", "library", "downloads", "profile"
 * - "surah/{id}" / "surah/{id}/{verseKey}" : surah reader
 * - "page/{number}"        : mushaf page reader
 */
@Composable
fun App() {
    var dark by remember { mutableStateOf(PrefsCache.getDarkTheme()) }
    var route by remember { mutableStateOf(DesktopRoutes.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsTick by remember { mutableStateOf(0) }
    var welcomed by remember { mutableStateOf(isWelcomed()) }

    fun toggleTheme() {
        dark = !dark
        PrefsCache.putDarkTheme(dark)
    }

    fun openSurah(chapterId: Int, verseKey: String? = null) {
        val chapter = QuranStore.chapter(chapterId)
        val name = chapter?.nameSimple ?: "Surah $chapterId"
        PrefsCache.putLastRead(chapterId, name, verseKey)
        RecentlyReadStore.record(chapterId, name, verseKey)
        route = if (verseKey == null) "surah/$chapterId" else "surah/$chapterId/$verseKey"
    }

    fun openPage(page: Int) {
        val first = QuranStore.versesOfPage(page).firstOrNull()
        if (first != null) {
            val chapter = QuranStore.chapter(first.chapterId)
            val name = chapter?.nameSimple ?: "Surah ${first.chapterId}"
            PrefsCache.putLastRead(first.chapterId, name, first.verseKey)
            RecentlyReadStore.record(first.chapterId, name, first.verseKey)
        }
        route = "page/$page"
    }

    NurTheme(dark = dark) {
        val pal = remember(dark) { NurPalette(dark) }
        if (showSettings) {
            SettingsDrawerDesktop(
                pal = pal,
                dark = dark,
                onToggleTheme = ::toggleTheme,
                onDismiss = {
                    showSettings = false
                    settingsTick++
                },
                onOpenDownloads = {
                    showSettings = false
                    settingsTick++
                    route = DesktopRoutes.DOWNLOADS
                }
            )
        }
        if (!welcomed) {
            WelcomeScreenDesktop(pal = pal) { welcomed = true }
        } else {
            var showShortcutHelp by remember { mutableStateOf(false) }
            // App-wide keyboard support: Ctrl/Cmd+1..8 jumps sections.
            // NOTE: no shell focus request here — readers own focus for arrows/Esc.
            // The shell Box has no focusable() so it only sees bubbled (unconsumed)
            // keys via onKeyEvent, never stealing from focused readers.
            val topRoutes = listOf(
                DesktopRoutes.HOME,
                DesktopRoutes.MEMORIZE,
                DesktopRoutes.PLANNER,
                DesktopRoutes.SAUKA,
                DesktopRoutes.ANALYTICS,
                DesktopRoutes.LIBRARY,
                DesktopRoutes.DOWNLOADS,
                DesktopRoutes.HISTORY,
            )
            Row(
                modifier = Modifier.fillMaxSize().background(pal.white)
                    .onKeyEvent { event ->
                        if (showSettings || showShortcutHelp) return@onKeyEvent false
                        // Only section jumps bubble here (Ctrl+digit). Readers consume
                        // arrows/Esc/Space themselves, so those never reach us.
                        AppShortcuts.sectionJump(
                            event,
                            topLevelRoute = { index -> topRoutes.getOrNull(index)?.let { route = it } },
                        )
                    }
            ) {
                AppSidebar(
                    pal = pal,
                    current = route,
                    onNavigate = { route = it },
                    onShowShortcuts = { showShortcutHelp = true },
                    modifier = Modifier.fillMaxHeight()
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        route == DesktopRoutes.HOME -> HomeScreenDesktop(
                            pal = pal,
                            dark = dark,
                            onToggleTheme = ::toggleTheme,
                            onSettingsClick = { showSettings = true },
                            onOpenSurah = ::openSurah,
                            onOpenPage = ::openPage,
                            onOpenMemorizeChapter = { id -> route = "memorize/$id" },
                            onOpenPlanner = { route = DesktopRoutes.PLANNER },
                            onOpenLibrary = { route = DesktopRoutes.LIBRARY }
                        )
                        route == DesktopRoutes.MEMORIZE -> MemorizeScreenDesktop(
                            pal = pal,
                            onBack = { route = DesktopRoutes.HOME },
                            onOpenSurah = ::openSurah,
                            onOpenHifdhReader = { id -> route = "memorize/$id" }
                        )
                        route.startsWith("memorize/") -> {
                            val id = route.removePrefix("memorize/").toIntOrNull() ?: 114
                            HifdhReaderScreenDesktop(
                                chapterId = id,
                                pal = pal,
                                onBack = { route = DesktopRoutes.MEMORIZE },
                                onOpenSurah = ::openSurah
                            )
                        }
                        route == DesktopRoutes.PLANNER -> PlannerScreenDesktop(
                            pal = pal,
                            onBack = { route = DesktopRoutes.HOME },
                            onOpenSurah = ::openSurah,
                            onOpenPage = ::openPage,
                            onOpenDay = { date -> route = "planner_reader/$date" }
                        )
                        route.startsWith("planner_reader/") -> {
                            val date = route.removePrefix("planner_reader/")
                            PlannerReaderScreenDesktop(
                                dayDate = date,
                                pal = pal,
                                onBack = { route = DesktopRoutes.PLANNER },
                                onBackToPlanner = { route = DesktopRoutes.PLANNER },
                                onOpenSurah = ::openSurah,
                                onOpenDay = { d -> route = "planner_reader/$d" }
                            )
                        }
                        route == DesktopRoutes.ANALYTICS -> AnalyticsScreenDesktop(
                            pal = pal,
                            onBack = { route = DesktopRoutes.HOME }
                        )
                        route == DesktopRoutes.LIBRARY -> LibraryScreenDesktop(
                            pal = pal,
                            onBack = { route = DesktopRoutes.HOME },
                            onOpenSurah = ::openSurah
                        )
                        route == DesktopRoutes.HISTORY -> HistoryScreenDesktop(
                            pal = pal,
                            onBack = { route = DesktopRoutes.HOME },
                            onOpenSurah = ::openSurah
                        )
                        route == DesktopRoutes.SAUKA -> SaukaIndexDesktop(
                            pal = pal,
                            onOpenGroup = { id -> route = "sauka/$id" }
                        )
                        route.startsWith("sauka/") -> {
                            val groupId = route.removePrefix("sauka/")
                            SaukaGroupDesktop(
                                pal = pal,
                                groupId = groupId,
                                onBack = { route = DesktopRoutes.SAUKA },
                                onOpenSurah = ::openSurah,
                                onOpenPage = ::openPage
                            )
                        }
                        route == DesktopRoutes.DOWNLOADS -> DownloadsScreenDesktop(
                            pal = pal,
                            onBack = { route = DesktopRoutes.HOME }
                        )
                        route == DesktopRoutes.PROFILE -> ProfileScreenDesktop(
                            pal = pal,
                            dark = dark,
                            onToggleTheme = ::toggleTheme,
                            onBack = { route = DesktopRoutes.HOME },
                            onOpenLibrary = { route = DesktopRoutes.LIBRARY },
                            onOpenDownloads = { route = DesktopRoutes.DOWNLOADS },
                            onOpenPlanner = { route = DesktopRoutes.PLANNER }
                        )
                        route.startsWith("surah/") -> {
                            val rest = route.removePrefix("surah/").split("/")
                            val chapterId = rest.getOrNull(0)?.toIntOrNull() ?: 1
                            val verseKey = rest.getOrNull(1)
                            SurahScreenDesktop(
                                chapterId = chapterId,
                                targetVerseKey = verseKey,
                                pal = pal,
                                dark = dark,
                                onToggleTheme = ::toggleTheme,
                                onSettingsClick = { showSettings = true },
                                settingsTick = settingsTick,
                                onBack = { route = DesktopRoutes.HOME },
                                onOpenSurah = ::openSurah,
                                onOpenPage = ::openPage
                            )
                        }
                        route.startsWith("page/") -> {
                            val page = route.removePrefix("page/").toIntOrNull() ?: 1
                            Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
                                Button(
                                    onClick = { route = DesktopRoutes.HOME },
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                                ) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Back")
                                }
                                PageScreenDesktop(
                                    pageNumber = page.coerceIn(1, 604),
                                    pal = pal,
                                    settingsTick = settingsTick,
                                    onPageChange = { next -> openPage(next) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                if (showShortcutHelp) {
                    ShortcutHelpDialog(pal = pal, onDismiss = { showShortcutHelp = false })
                }
            }
        }
    }
}

private fun isWelcomed(): Boolean = try {
    java.util.prefs.Preferences.userRoot().node("welcome_prefs").getBoolean("seen", false)
} catch (_: Exception) {
    true
}
