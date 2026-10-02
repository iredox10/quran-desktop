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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.nur.quran.desktop.ui.home.GlobalSearch
import com.nur.quran.desktop.ui.home.HomeStatsRow
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
                }
            )
        }
        if (!welcomed) {
            WelcomeScreenDesktop(pal = pal) { welcomed = true }
        } else {
            Row(modifier = Modifier.fillMaxSize().background(pal.white)) {
                AppSidebar(
                    pal = pal,
                    current = route,
                    onNavigate = { route = it },
                    modifier = Modifier.fillMaxHeight()
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        route == DesktopRoutes.HOME -> HomeScreen(
                            pal = pal,
                            dark = dark,
                            onToggleTheme = ::toggleTheme,
                            onSettingsClick = { showSettings = true },
                            onOpenSurah = ::openSurah,
                            onOpenPage = ::openPage,
                            onOpenMemorizeChapter = { id -> route = "memorize/$id" }
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
            }
        }
    }
}

private fun isWelcomed(): Boolean = try {
    java.util.prefs.Preferences.userRoot().node("welcome_prefs").getBoolean("seen", false)
} catch (_: Exception) {
    true
}

/**
 * Chapter with the most FSRS-due hifdh verses, or null when nothing is due.
 * Entries without a card (never reviewed) are treated as due immediately.
 */
private fun topDueChapter(): Pair<Int, Int>? {
    val now = System.currentTimeMillis()
    val neverDue = FsrsCard(
        due = 0L, stability = 0.0, difficulty = 0.0,
        elapsedDays = 0.0, scheduledDays = 0, reps = 0,
        lapses = 0, learningSteps = 0, state = 0
    )
    val counts = HashMap<Int, Int>()
    HifdhStore.loadHifdhHistory().forEach { (key, entry) ->
        val chapterId = key.substringBefore(":").toIntOrNull() ?: return@forEach
        if (chapterId in 1..114 && (entry.card ?: neverDue).isDue(now)) {
            counts.merge(chapterId, 1, Int::plus)
        }
    }
    return counts.entries.maxByOrNull { it.value }?.let { it.key to it.value }
}

// ── Top navbar (matches Android TopNavbar / web Layout header) ─────────────
@Composable
private fun TopNavbar(
    pal: NurPalette,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        color = pal.cream.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource("drawable/ic_logo.png"),
                        contentDescription = "Quran Nur Logo",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Quran Nur",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleTheme() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = pal.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = pal.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = pal.boneDark.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

// ── Homepage ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(
    pal: NurPalette,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenSurah: (Int, String?) -> Unit,
    onOpenPage: (Int) -> Unit,
    onOpenMemorizeChapter: (Int) -> Unit = {}
) {
    var browseMode by remember { mutableStateOf("surah") }
    var searchQuery by remember { mutableStateOf("") }
    var lastReadTick by remember { mutableStateOf(0) }
    var historyTick by remember { mutableStateOf(0) }

    val chapters = remember { QuranStore.chapters }
    val browseItems = remember(browseMode, chapters, searchQuery) {
        filterBrowseItems(buildBrowseItems(browseMode, chapters), searchQuery)
    }
    val greeting = remember { getGreeting() }
    val todayDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH).format(Date()).uppercase()
    }
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val lastRead = remember(lastReadTick) { PrefsCache.getLastRead() }
    val verseOfDay = remember { QuranStore.verseOfDay() }
    val verseOfDaySurah = remember(verseOfDay) {
        QuranStore.chapter(verseOfDay.chapterId)?.nameSimple ?: "Surah ${verseOfDay.chapterId}"
    }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        TopNavbar(
            pal = pal,
            dark = dark,
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick
        )
        // Large-screen adaptation (web max-w-[1200px]): center a capped column.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxW = maxWidth
            val columns = when {
                maxW < 600.dp -> 1
                maxW < 980.dp -> 2
                else -> 3
            }
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1100.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                // ── Greeting hero + Continue ──
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = greeting.first,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greeting.second,
                            fontSize = 13.sp,
                            color = pal.inkMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = todayDate,
                            fontSize = 10.sp,
                            color = pal.inkMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (lastRead != null) {
                            ContinueReadingCard(
                                pal = pal,
                                fontUi = fontUi,
                                chapterName = lastRead.chapterName,
                                verseKey = lastRead.verseKey,
                                timestamp = lastRead.timestamp,
                                onClick = { onOpenSurah(lastRead.chapterId, lastRead.verseKey) }
                            )
                        } else {
                            ZeroStateContinueCard(
                                pal = pal,
                                fontUi = fontUi,
                                onClick = { onOpenSurah(1, null) }
                            )
                        }
                    }
                }

                // ── Recently read ──
                item {
                    val recentEntries = remember(lastReadTick, historyTick) { RecentlyReadStore.all() }
                    if (recentEntries.isNotEmpty()) {
                        RecentlyReadRow(
                            pal = pal,
                            entries = recentEntries,
                            onOpen = { id, vk ->
                                lastReadTick++
                                historyTick++
                                onOpenSurah(id, vk)
                            },
                            onClear = {
                                RecentlyReadStore.clear()
                                lastReadTick++
                                historyTick++
                            }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // ── Due for review ──
                item {
                    val topDue = remember(lastReadTick, historyTick) { topDueChapter() }
                    if (topDue != null) {
                        DueReviewCard(
                            pal = pal,
                            fontUi = fontUi,
                            chapterId = topDue.first,
                            chapterName = QuranStore.chapter(topDue.first)?.nameSimple
                                ?: "Surah ${topDue.first}",
                            dueCount = topDue.second,
                            onReview = { onOpenMemorizeChapter(topDue.first) }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // ── Verse of the day ──
                item {
                    val vodArabic = remember(verseOfDay) {
                        verseDisplayArabic(verseOfDay, PrefsCache.getFont())
                    }
                    VerseOfDayCard(
                        pal = pal,
                        verseArabic = vodArabic,
                        verseTranslation = verseOfDay.translation,
                        verseRef = "$verseOfDaySurah ${verseOfDay.verseKey}",
                        onOpenVerse = {
                            PrefsCache.putLastRead(
                                verseOfDay.chapterId,
                                verseOfDaySurah,
                                verseOfDay.verseKey
                            )
                            lastReadTick++
                            historyTick++
                            onOpenSurah(verseOfDay.chapterId, verseOfDay.verseKey)
                        }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── Reading stats strip ──
                item {
                    HomeStatsRow(pal = pal)
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── Full-text search ──
                item {
                    GlobalSearch(
                        pal = pal,
                        onOpenSurah = onOpenSurah,
                        onOpenPage = onOpenPage
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── Browse header + mode pills ──
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Browse the Quran",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Select a Surah, Page, Juz, or Hizb to begin.",
                            fontSize = 13.sp,
                            color = pal.inkMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val modes = listOf(
                            Triple("surah", "Surah", Icons.Filled.AutoStories),
                            Triple("page", "Page", Icons.Filled.Article),
                            Triple("juz", "Juz", Icons.Filled.LibraryBooks),
                            Triple("hizb", "Hizb", Icons.Filled.Layers)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            modes.forEach { (mode, label, icon) ->
                                val selected = browseMode == mode
                                Surface(
                                    onClick = {
                                        browseMode = mode
                                        searchQuery = ""
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selected) pal.tealSoft else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (selected) pal.teal else pal.boneDark
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (selected) pal.teal else pal.inkMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) pal.teal else pal.inkMuted
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ── Search ──
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search ${browseMode.replaceFirstChar { it.uppercase() }}...",
                                color = pal.inkMuted,
                                fontSize = 15.sp,
                                fontFamily = fontBody
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = pal.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear Search",
                                        tint = pal.inkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.teal,
                            unfocusedBorderColor = pal.boneDark,
                            focusedContainerColor = pal.cream,
                            unfocusedContainerColor = pal.cream,
                            cursorColor = pal.teal,
                            focusedTextColor = pal.ink,
                            unfocusedTextColor = pal.ink
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Browse grid (web sm:2 / md:3 parity) ──
                val rows = browseItems.chunked(columns)
                items(rows, key = { row -> row.first().key }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { item ->
                            BrowseItemCard(
                                item = item,
                                pal = pal,
                                fontUi = fontUi,
                                fontBody = fontBody,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    lastReadTick++
                                    historyTick++
                                    when {
                                        item.pageNumber != null && browseMode != "surah" ->
                                            onOpenPage(item.pageNumber)
                                        item.chapterId != null -> onOpenSurah(item.chapterId, null)
                                        item.pageNumber != null -> onOpenPage(item.pageNumber)
                                    }
                                }
                            )
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (browseItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = pal.inkMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "No results found for \"$searchQuery\" in $browseMode"
                                } else {
                                    "No results matching your search."
                                },
                                fontSize = 14.sp,
                                fontFamily = fontUi,
                                color = pal.ink,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotEmpty()) {
                                Surface(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(12.dp),
                                    color = pal.cream,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Clear Search",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Continue reading (teal gradient) ───────────────────────────────────────
@Composable
private fun ContinueReadingCard(
    pal: NurPalette,
    fontUi: FontFamily,
    chapterName: String,
    verseKey: String?,
    timestamp: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(pal.teal, pal.tealMid)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue: $chapterName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = fontUi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val verseLabel = verseKey?.split(":")?.getOrNull(1)?.let { "Verse $it" }
                    ?: "From the beginning"
                Text(
                    text = "$verseLabel · ${timeAgo(timestamp)}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.3.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ZeroStateContinueCard(
    pal: NurPalette,
    fontUi: FontFamily,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = pal.tealSoft,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.teal.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.teal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = pal.teal,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Begin Your Journey",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.ink,
                fontFamily = fontUi
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Start reading with Surah Al-Fatihah (The Opening).",
                fontSize = 12.sp,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = pal.teal,
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Start Reading",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = fontUi
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Due for review (teal, FSRS-backed) ──────────────────────────────
@Composable
private fun DueReviewCard(
    pal: NurPalette,
    fontUi: FontFamily,
    chapterId: Int,
    chapterName: String,
    dueCount: Int,
    onReview: () -> Unit
) {
    Surface(
        onClick = onReview,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(pal.teal, pal.tealMid)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🧠",
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dueCount verses due for review",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = fontUi
                )
                Text(
                    text = "$chapterName · $chapterId — keep your hifdh sharp",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontFamily = FontFamily.Monospace
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Text(
                    text = "Review now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = fontUi,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// ── Verse of the day (web parity) ──────────────────────────────────────────
@Composable
private fun VerseOfDayCard(
    pal: NurPalette,
    verseArabic: String,
    verseTranslation: String,
    verseRef: String,
    onOpenVerse: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val fontArabic = rememberArabicFontFamily()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = pal.cream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "﷽",
                fontSize = 96.sp,
                color = pal.gold.copy(alpha = 0.06f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "VERSE OF THE DAY",
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.gold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = verseArabic,
                    fontFamily = fontArabic,
                    fontSize = 26.sp,
                    lineHeight = 52.sp,
                    textAlign = TextAlign.Center,
                    color = pal.ink,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TranslationTextDesktop(
                    html = verseTranslation,
                    fontScale = 1f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— $verseRef",
                    fontSize = 11.sp,
                    color = pal.inkMuted,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        onClick = {
                            copied = copyToClipboard("$verseArabic\n$verseTranslation — $verseRef")
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (copied) pal.green else pal.boneDark
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = null,
                                tint = if (copied) pal.green else pal.inkMid,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copied) "Copied" else "Copy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (copied) pal.green else pal.inkMid
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onOpenVerse,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = pal.inkMid,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.inkMid
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Browse item card (unified Surah/Page/Juz/Hizb, matching Android/web) ───
@Composable
private fun BrowseItemCard(
    item: BrowseItem,
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fontArabic = rememberArabicFontFamily()
    Surface(
        onClick = onClick,
        modifier = modifier.padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = pal.cream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.tealSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (item.prefix ?: "#").toString(),
                    fontWeight = FontWeight.Bold,
                    color = pal.teal,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = pal.ink,
                        fontFamily = fontUi,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.arabic != null) {
                        Text(
                            text = item.arabic,
                            fontFamily = fontArabic,
                            fontSize = 20.sp,
                            color = pal.gold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.subtitle,
                        fontSize = 11.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pal.bone)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.meta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.inkMuted
                        )
                    }
                }
            }
        }
    }
}

