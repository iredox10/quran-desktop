package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.BookmarkStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.ui.home.BrowseItem
import com.nur.quran.desktop.ui.home.HomeBookmarkCard
import com.nur.quran.desktop.ui.home.HomeOnboardingProgressCard
import com.nur.quran.desktop.ui.home.RecentlyReadRow
import com.nur.quran.desktop.ui.home.WeeklyHeatmapCard
import com.nur.quran.desktop.ui.home.buildBrowseItems
import com.nur.quran.desktop.ui.home.filterBrowseItems
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.desktop.ui.theme.timeAgo
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 1:1 Parity Desktop implementation of web `Home.jsx`:
 * - Top Navbar with logo, title, theme toggle, and settings
 * - Greeting hero with time-of-day sub-greeting, formatted date, and continue reading card
 * - Onboarding Progress card (5 steps with teal-gold gradient bar, dismissible)
 * - Weekly Heatmap ("This Week") with 7 vertical day columns and 4 color tiers
 * - Pinned Bookmark card in gold styling
 * - Recently Read horizontal card row (capped at 6 items with 28x28 badges)
 * - Browse the Quran with 4 mode pills (Surah, Page, Juz, Hizb), integrated search, and 3-column card grid
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreenDesktop(
    pal: NurPalette,
    dark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenSurah: (chapterId: Int, verseKey: String?) -> Unit = { _, _ -> },
    onOpenPage: (pageNumber: Int) -> Unit = {},
    onOpenMemorizeChapter: (chapterId: Int) -> Unit = {},
    onOpenPlanner: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()

    var browseMode by remember { mutableStateOf("surah") }
    var searchQuery by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }

    val chapters = remember { QuranStore.chapters }
    val browseItems = remember(browseMode, chapters) { buildBrowseItems(browseMode, chapters) }
    val filteredItems = remember(browseItems, searchQuery) { filterBrowseItems(browseItems, searchQuery) }
    val chunkedItems = remember(filteredItems) { filteredItems.chunked(3) }

    // Last read entry for continue hero
    val recentEntries = remember(tick) { RecentlyReadStore.all() }
    val lastRead = remember(recentEntries) {
        recentEntries.firstOrNull() ?: PrefsCache.getLastRead()?.let {
            RecentlyReadStore.Entry(
                chapterId = it.chapterId,
                chapterName = it.chapterName,
                verseKey = it.verseKey,
                timestamp = it.timestamp
            )
        }
    }

    // Active bookmark for gold card
    val bookmarkKey = remember(tick) { BookmarkStore.all().firstOrNull() }

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pal.cream)
    ) {
        // ─── Top Navbar ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = pal.cream,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
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
                            color = pal.ink,
                            fontFamily = fontUi
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                                .size(48.dp)
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

        // ─── Main Content (centered max-w 1200px) ───
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1200.dp)
                    .dragToScroll(listState, orientation = Orientation.Vertical),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── 1. Greeting Hero Section ───
                item(key = "greeting_hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val greeting = remember { getHomeGreeting() }
                        val formattedDate = remember { getFormattedDate() }

                        Text(
                            text = greeting.first,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = greeting.second,
                            fontSize = 13.5.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDate.uppercase(),
                            fontSize = 10.sp,
                            color = pal.inkMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )

                        if (lastRead != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                onClick = { onOpenSurah(lastRead.chapterId, lastRead.verseKey) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 440.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(listOf(pal.teal, pal.tealMid))
                                        )
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Continue: ${lastRead.chapterName}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontFamily = fontUi
                                        )
                                        val subText = if (lastRead.verseKey != null) {
                                            val verseNum = lastRead.verseKey.substringAfter(":", lastRead.verseKey)
                                            "Verse $verseNum · ${timeAgo(lastRead.timestamp)}"
                                        } else {
                                            "From the beginning · ${timeAgo(lastRead.timestamp)}"
                                        }
                                        Text(
                                            text = subText,
                                            fontSize = 11.5.sp,
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontFamily = FontFamily.Monospace
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
                    }
                }

                // ─── 2. Onboarding Progress Card ───
                item(key = "onboarding_progress") {
                    HomeOnboardingProgressCard(
                        pal = pal,
                        onOpenHome = { /* Already on Home */ },
                        onOpenSurah = onOpenSurah,
                        onOpenMemorize = onOpenMemorizeChapter,
                        onOpenPlanner = onOpenPlanner,
                        onOpenLibrary = onOpenLibrary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── 3. Weekly Heatmap ("This Week") ───
                item(key = "weekly_heatmap") {
                    WeeklyHeatmapCard(
                        pal = pal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ─── 4. Bookmark Card ───
                if (bookmarkKey != null) {
                    item(key = "home_bookmark") {
                        HomeBookmarkCard(
                            pal = pal,
                            bookmarkKey = bookmarkKey,
                            onOpenSurah = onOpenSurah,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ─── 5. Recently Read Horizontal Row ───
                if (recentEntries.isNotEmpty()) {
                    item(key = "recently_read_row") {
                        RecentlyReadRow(
                            pal = pal,
                            entries = recentEntries,
                            onOpen = { chId, vk ->
                                tick++
                                onOpenSurah(chId, vk)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ─── 6. Browse the Quran Section ───
                item(key = "browse_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = pal.ink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Browse the Quran",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Select a Surah, Page, Juz, or Hizb to begin.",
                                    fontSize = 13.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                            }

                            // Mode pills
                            val modes = listOf(
                                Triple("surah", "Surah", Icons.AutoMirrored.Filled.MenuBook),
                                Triple("page", "Page", Icons.AutoMirrored.Filled.Article),
                                Triple("juz", "Juz", Icons.AutoMirrored.Filled.LibraryBooks),
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
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (selected) pal.teal else pal.boneDark
                                        ),
                                        modifier = Modifier.touchTarget(44.dp)
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
                                                color = if (selected) pal.teal else pal.inkMuted,
                                                fontFamily = fontUi
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Integrated Search Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = pal.cream,
                            border = BorderStroke(1.5.dp, pal.boneDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = pal.inkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    ),
                                    cursorBrush = SolidColor(pal.teal),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search $browseMode...",
                                                fontSize = 15.sp,
                                                color = pal.inkMuted,
                                                fontFamily = fontUi
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = pal.inkMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── 7. Responsive 3-Column Browse Grid ───
                if (filteredItems.isNotEmpty()) {
                    items(chunkedItems, key = { row -> row.first().key }) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in row) {
                                BrowseCard(
                                    item = item,
                                    pal = pal,
                                    fontUi = fontUi,
                                    fontBody = fontBody,
                                    fontArabic = fontArabic,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (item.chapterId != null) {
                                            onOpenSurah(item.chapterId, null)
                                        } else if (item.pageNumber != null) {
                                            onOpenPage(item.pageNumber)
                                        }
                                    }
                                )
                            }
                            // Fill empty cells if row has fewer than 3 items
                            if (row.size < 3) {
                                for (i in 0 until (3 - row.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // Empty search state
                    item(key = "empty_search_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No results matching your search.",
                                fontSize = 14.sp,
                                fontFamily = fontBody,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = pal.inkMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun BrowseCard(
    item: BrowseItem,
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    fontArabic: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = pal.cream,
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = modifier
            .fillMaxWidth()
            .touchTarget(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prefix badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.tealSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.prefix?.toString() ?: "#",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.teal,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.ink,
                        fontFamily = fontUi,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.arabic != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.arabic,
                            fontFamily = fontArabic,
                            fontSize = 18.sp,
                            color = pal.gold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.subtitle,
                        fontSize = 11.5.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

private fun getHomeGreeting(): Pair<String, String> {
    val h = LocalTime.now().hour
    return when {
        h in 5..11 -> Pair("Assalamu Alaikum", "May your morning be blessed")
        h in 12..16 -> Pair("Assalamu Alaikum", "Wishing you a productive afternoon")
        h in 17..20 -> Pair("Assalamu Alaikum", "May your evening be peaceful")
        else -> Pair("Assalamu Alaikum", "May your night be filled with barakah")
    }
}

private fun getFormattedDate(): String {
    return LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
    )
}
