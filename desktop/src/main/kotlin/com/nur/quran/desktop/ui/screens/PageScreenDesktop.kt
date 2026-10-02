package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.PageWord
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.WordStore
import com.nur.quran.desktop.ui.audio.MiniPlayerDesktop
import com.nur.quran.desktop.ui.components.MushafPageLines
import com.nur.quran.desktop.ui.components.WordRef
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.getHizbByPage
import com.nur.quran.shared.getJuzByPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop Page reader (mushaf pages 1..604): every verse on [pageNumber] with
 * translation, plus a bottom page navigator.
 */
@Composable
fun PageScreenDesktop(
    pageNumber: Int,
    pal: NurPalette,
    settingsTick: Int = 0,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> }
) {
    val verses = remember(pageNumber) { QuranStore.versesOfPage(pageNumber) }
    val fontUi = rememberUiFontFamily()
    // Re-read reader prefs whenever the settings dialog closes.
    val readerPrefs = remember(settingsTick) {
        Triple(
            com.nur.quran.desktop.PrefsCache.getFont(),
            com.nur.quran.desktop.PrefsCache.getArabicScale(),
            com.nur.quran.desktop.PrefsCache.getReaderTranslationEnabled()
        )
    }
    // Alternate translation pack per verse chapter (null = bundled).
    val translationId = remember(settingsTick) {
        com.nur.quran.desktop.PrefsCache.getTranslation().toIntOrNull() ?: 20
    }
    val translationMaps = remember(verses, translationId, settingsTick) {
        if (translationId == 20) emptyMap()
        else verses.map { it.chapterId }.toSet().associateWith { cid ->
            com.nur.quran.desktop.data.TranslationStore.getChapter(cid, translationId).orEmpty()
        }
    }

    // Page audio state: active when the engine's current verse is on this page.
    val pageKeys = remember(verses) { verses.map { it.verseKey }.toSet() }
    val nowPlayingKey = AudioEngine.current?.verseKey
    val isPageActive = nowPlayingKey in pageKeys
    val isPagePlaying = isPageActive && AudioEngine.playing

    Box(
        modifier = modifier.fillMaxSize().background(pal.white)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageTopBar(
                pal = pal,
                fontUi = fontUi,
                pageNumber = pageNumber,
                dark = dark,
                isPlaying = isPagePlaying,
                onBack = onBack,
                onTogglePlayPage = {
                    if (isPageActive) {
                        AudioEngine.togglePlayPause()
                    } else {
                        AudioEngine.playTracks(
                            verses.map { v ->
                                AudioEngine.Track(v.verseKey, v.chapterId, v.verseNumber)
                            }
                        )
                    }
                },
                onToggleTheme = onToggleTheme
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // List/Mushaf toggle (pills mirror the browse mode pills in App.kt).
                var showMushaf by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewPill(label = "Verses", selected = !showMushaf, pal = pal) { showMushaf = false }
                    ViewPill(label = "Mushaf page", selected = showMushaf, pal = pal) { showMushaf = true }
                }

                if (!showMushaf) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(verses, key = { it.verseKey }) { verse ->
                            VerseRow(
                                verse = verse,
                                pal = pal,
                                fontName = readerPrefs.first,
                                fontScale = readerPrefs.second,
                                showTranslation = readerPrefs.third,
                                translationOverride = translationMaps[verse.chapterId]?.get(verse.verseKey)
                            )
                        }
                    }
                } else {
                    var pageWords by remember { mutableStateOf<List<PageWord>?>(null) }
                    LaunchedEffect(pageNumber, showMushaf) {
                        if (!showMushaf) return@LaunchedEffect
                        pageWords = null
                        pageWords = withContext(Dispatchers.IO) { WordStore.getPageWords(pageNumber) }
                    }
                    val loaded = pageWords
                    when {
                        loaded == null -> {
                            Text(
                                text = "Loading…",
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f).padding(16.dp)
                            )
                        }
                        loaded.isEmpty() && verses.isNotEmpty() -> {
                            Text(
                                text = "Offline — page words unavailable",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).padding(16.dp)
                            )
                        }
                        else -> {
                            val wordsByLine = remember(loaded) {
                                loaded.filter { it.lineNumber > 0 }
                                    .groupBy({ it.lineNumber }, { WordRef(it.text) })
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 88.dp)
                            ) {
                                MushafPageLines(
                                    wordsByLine = wordsByLine,
                                    scale = readerPrefs.second
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (pageNumber > 1) onPageChange(pageNumber - 1) },
                    enabled = pageNumber > 1
                ) {
                    Text("← Previous")
                }
                Text(text = "Page $pageNumber / 604", fontSize = 14.sp)
                Button(
                    onClick = { if (pageNumber < 604) onPageChange(pageNumber + 1) },
                    enabled = pageNumber < 604
                ) {
                    Text("Next →")
                }
            }
        }
        val nowPlayingTrack = AudioEngine.current
        if (nowPlayingTrack != null) {
            MiniPlayerDesktop(
                pal = pal,
                track = nowPlayingTrack,
                playing = AudioEngine.playing,
                onToggle = { AudioEngine.togglePlayPause() },
                onNext = { AudioEngine.next() },
                onPrev = { AudioEngine.prev() },
                onClose = { AudioEngine.stop() },
                onOpenVerse = { vk ->
                    vk.substringBefore(":").toIntOrNull()?.let { ch ->
                        onOpenSurah(ch, vk)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ── Top app bar (matches SurahTopBar styling) ────────────────────────────────
@Composable
private fun PageTopBar(
    pal: NurPalette,
    fontUi: FontFamily,
    pageNumber: Int,
    dark: Boolean,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onTogglePlayPage: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val juz = remember(pageNumber) { getJuzByPage(pageNumber) }
    val hizb = remember(pageNumber) { getHizbByPage(pageNumber) }
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = pal.cream.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = pal.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Page $pageNumber",
                        fontFamily = fontUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = pal.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Juz ${juz.id} • Hizb ${hizb.id}",
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = pal.gold,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlayPage,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause page" else "Play page",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                PageTopBarIconBtn(pal = pal, description = "Toggle theme", onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = if (dark) pal.gold else pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            HorizontalDivider(color = pal.boneDark.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

@Composable
private fun PageTopBarIconBtn(
    pal: NurPalette,
    description: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/** Small toggle pill mirroring the browse mode pills in App.kt. */
@Composable
private fun ViewPill(
    label: String,
    selected: Boolean,
    pal: NurPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) pal.tealSoft else Color.Transparent,
        border = BorderStroke(1.5.dp, if (selected) pal.teal else pal.boneDark)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = if (selected) pal.teal else pal.inkMuted
        )
    }
}
