package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.audio.MiniPlayerDesktop
import com.nur.quran.desktop.ui.components.PlainVerseText
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.shared.HIZB_STARTS
import com.nur.quran.shared.JUZ_STARTS
import com.nur.quran.shared.getHizbByPage
import com.nur.quran.shared.getJuzByPage
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * Desktop Surah reader mirroring Android `SurahScreen`: top app bar (back,
 * title + surah navigator, reading mode, theme, settings), surah header,
 * Bismillah, verse list with gold dividers / page pills / Juz-Hizb pills,
 * per-verse actions, and prev/next surah buttons.
 */
@Composable
fun SurahScreenDesktop(
    chapterId: Int,
    targetVerseKey: String? = null,
    pal: NurPalette,
    dark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    settingsTick: Int = 0,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> },
    onOpenPage: (Int) -> Unit = {}
) {
    val chapter = remember(chapterId) { QuranStore.chapter(chapterId) }
    val verses = remember(chapterId) { QuranStore.versesOfChapter(chapterId) }
    var readingMode by remember(chapterId) { mutableStateOf(false) }
    var showNavDialog by remember(chapterId) { mutableStateOf(false) }

    val fontName = remember(settingsTick) { PrefsCache.getFont() }
    val arabicScale = remember(settingsTick) { PrefsCache.getArabicScale() }
    val translationScale = remember(settingsTick) { PrefsCache.getTranslationScale() }
    val lineHeightMult = remember(settingsTick) { PrefsCache.getLineHeightMult() }
    val translationOn = remember(settingsTick) { PrefsCache.getReaderTranslationEnabled() }

    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily(fontName)

    // Alternate translation pack (downloaded via Downloads); null = bundled.
    val translationId = remember(settingsTick) {
        PrefsCache.getTranslation().toIntOrNull() ?: 20
    }
    val translationMap = remember(chapterId, translationId, settingsTick) {
        if (translationId == 20) null
        else com.nur.quran.desktop.data.TranslationStore.getChapter(chapterId, translationId)
    }

    // Reading session timer: log minutes spent on this surah when leaving.
    val entryTime = remember(chapterId) { System.currentTimeMillis() }
    androidx.compose.runtime.DisposableEffect(chapterId) {
        onDispose {
            val mins = ((System.currentTimeMillis() - entryTime) / 60000).toInt()
            if (mins >= 1) {
                com.nur.quran.desktop.data.SessionStore.log("reading", chapterId, mins)
            }
        }
    }

    val juzByKey = remember { JUZ_STARTS.associateBy { it.verseKey } }
    val hizbByKey = remember { HIZB_STARTS.associateBy { it.verseKey } }
    val listState = rememberLazyListState()
    val hasBismillah = chapterId != 1 && chapterId != 9

    LaunchedEffect(targetVerseKey, chapterId, readingMode) {
        if (!readingMode && targetVerseKey != null) {
            val idx = verses.indexOfFirst { it.verseKey == targetVerseKey }
            if (idx >= 0) {
                listState.scrollToItem((if (hasBismillah) 2 else 1) + idx)
            }
        }
    }

    if (showNavDialog) {
        SurahNavigationDialog(
            pal = pal,
            fontUi = fontUi,
            fontBody = fontBody,
            currentChapterId = chapterId,
            onDismiss = { showNavDialog = false },
            onOpenSurah = { id, verseKey ->
                showNavDialog = false
                onOpenSurah(id, verseKey)
            },
            onOpenPage = { page ->
                showNavDialog = false
                onOpenPage(page)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(pal.white)) {
        Column(modifier = Modifier.fillMaxSize()) {
        SurahTopBar(
            pal = pal,
            dark = dark,
            title = chapter?.nameSimple ?: "Surah $chapterId",
            fontUi = fontUi,
            readingMode = readingMode,
            onBack = onBack,
            onTitleClick = { showNavDialog = true },
            onToggleReadingMode = { readingMode = !readingMode },
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick
        )

        if (readingMode) {
            ContinuousReadingList(
                pal = pal,
                fontArabic = fontArabic,
                fontName = fontName,
                arabicScale = arabicScale,
                lineHeightMult = lineHeightMult,
                chapterId = chapterId
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp)
            ) {
                item(key = "header") {
                    SurahHeader(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        fontArabic = fontArabic,
                        chapterId = chapterId
                    )
                }
                if (hasBismillah) {
                    item(key = "bismillah") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                fontSize = 28.sp,
                                fontFamily = fontArabic,
                                color = pal.gold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                itemsIndexed(verses, key = { _, v -> v.verseKey }) { index, verse ->
                    val prev = verses.getOrNull(index - 1)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        VerseDivider(pal = pal)
                        if (prev == null || prev.pageNumber != verse.pageNumber) {
                            PageDivider(pal = pal, fontBody = fontBody, page = verse.pageNumber)
                        }
                        val juz = juzByKey[verse.verseKey]
                        val hizb = hizbByKey[verse.verseKey]
                        when {
                            juz != null -> DivisionDivider(
                                pal = pal,
                                fontBody = fontBody,
                                label = "Juz ${juz.id}"
                            )
                            hizb != null -> DivisionDivider(
                                pal = pal,
                                fontBody = fontBody,
                                label = "Hizb ${hizb.id}"
                            )
                        }
                        VerseRow(
                            verse = verse,
                            pal = pal,
                            fontName = fontName,
                            fontScale = arabicScale,
                            translationScale = translationScale,
                            lineHeightMultiplier = lineHeightMult,
                            showTranslation = translationOn,
                            highlighted = verse.verseKey == targetVerseKey ||
                                verse.verseKey == AudioEngine.current?.verseKey,
                            onPlayVerse = { vk -> AudioEngine.playVerse(vk) },
                            translationOverride = translationMap?.get(verse.verseKey)
                        )
                    }
                }
                item(key = "nav") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(1.dp)
                                .background(pal.boneDark)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SurahNavButtons(
                            pal = pal,
                            fontUi = fontUi,
                            chapterId = chapterId,
                            onOpenSurah = { id -> onOpenSurah(id, null) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
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

// ── Top app bar ────────────────────────────────────────────────────────────
@Composable
private fun SurahTopBar(
    pal: NurPalette,
    dark: Boolean,
    title: String,
    fontUi: FontFamily,
    readingMode: Boolean,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTitleClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = fontUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = pal.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select Surah",
                        tint = pal.inkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
                TopBarIconBtn(
                    pal = pal,
                    active = readingMode,
                    description = "Reading mode",
                    onClick = onToggleReadingMode
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoStories,
                        contentDescription = null,
                        tint = if (readingMode) pal.gold else pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                TopBarIconBtn(pal = pal, description = "Toggle theme", onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = if (dark) pal.gold else pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                TopBarIconBtn(pal = pal, description = "Settings", onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = pal.inkMuted,
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
private fun TopBarIconBtn(
    pal: NurPalette,
    active: Boolean = false,
    description: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) pal.goldSoft else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

// ── Surah header ───────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurahHeader(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    fontArabic: FontFamily,
    chapterId: Int
) {
    val chapter = remember(chapterId) { QuranStore.chapter(chapterId) }
    val startPage = chapter?.pages?.firstOrNull() ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = chapter?.nameSimple ?: "Surah $chapterId",
                fontSize = 35.sp,
                fontWeight = FontWeight.ExtraBold,
                color = pal.ink,
                fontFamily = fontUi,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = chapter?.nameArabic ?: "",
                fontSize = 40.sp,
                color = pal.gold,
                fontFamily = fontArabic,
                modifier = Modifier.offset(y = (-6).dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val metaItems = remember(chapter, startPage) {
            buildList {
                add("Surah $chapterId")
                chapter?.translatedNameText?.takeIf { it.isNotBlank() }?.let { add(it) }
                add("${chapter?.versesCount ?: 0} Ayahs")
                chapter?.revelationPlace?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (startPage > 0) {
                    add("Starts Juz ${getJuzByPage(startPage).id} • Hizb ${getHizbByPage(startPage).id}")
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center
        ) {
            metaItems.forEachIndexed { i, item ->
                if (i > 0) {
                    Text(
                        text = "  ●  ",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = pal.inkMuted.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = item.uppercase(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (i == 0) pal.gold else pal.inkMid
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val audioCurrent = AudioEngine.current
        val audioPlaying = AudioEngine.playing
        val isThisChapter = audioCurrent?.chapterId == chapterId
        val showPlaying = isThisChapter && audioPlaying
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (showPlaying) pal.goldSoft else Color.Transparent)
                .border(1.dp, pal.gold, RoundedCornerShape(100.dp))
                .clickable {
                    if (isThisChapter) AudioEngine.togglePlayPause()
                    else AudioEngine.playChapter(chapterId)
                }
                .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = pal.gold,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isThisChapter) AudioEngine.togglePlayPause()
                        else AudioEngine.playChapter(chapterId)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (showPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (showPlaying) "Pause chapter" else "Play chapter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showPlaying) "Playing…" else "Play chapter",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontUi,
                color = if (showPlaying) pal.gold else pal.inkMid
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = pal.boneDark, thickness = 1.dp)
    }
}

// ── Dividers ───────────────────────────────────────────────────────────────
@Composable
internal fun VerseDivider(pal: NurPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            pal.goldLight,
                            pal.boneDark,
                            pal.goldLight,
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(pal.gold.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            pal.goldLight,
                            pal.boneDark,
                            pal.goldLight,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun PageDivider(pal: NurPalette, fontBody: FontFamily, page: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, pal.gold, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(pal.goldLight)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Page $page",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = fontBody
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, pal.gold, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun DivisionDivider(pal: NurPalette, fontBody: FontFamily, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, pal.gold, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(pal.goldLight)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = fontBody,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, pal.gold, Color.Transparent)
                    )
                )
        )
    }
}

// ── Prev/next surah ────────────────────────────────────────────────────────
@Composable
private fun SurahNavButtons(
    pal: NurPalette,
    fontUi: FontFamily,
    chapterId: Int,
    onOpenSurah: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (chapterId < 114) {
            val next = QuranStore.chapter(chapterId + 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, pal.boneDark, RoundedCornerShape(20.dp))
                    .clickable { onOpenSurah(chapterId + 1) }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(pal.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = pal.inkMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Surah ${next?.nameSimple ?: (chapterId + 1)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        if (chapterId > 1) {
            val prev = QuranStore.chapter(chapterId - 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, pal.boneDark, RoundedCornerShape(20.dp))
                    .clickable { onOpenSurah(chapterId - 1) }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Surah ${prev?.nameSimple ?: (chapterId - 1)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(pal.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = pal.inkMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Reading mode: continuous justified pages ───────────────────────────────
@Composable
private fun ContinuousReadingList(
    pal: NurPalette,
    fontArabic: FontFamily,
    fontName: String,
    arabicScale: Float,
    lineHeightMult: Float,
    chapterId: Int
) {
    val verses = remember(chapterId) { QuranStore.versesOfChapter(chapterId) }
    val byPage = remember(verses) { verses.groupBy { it.pageNumber }.toSortedMap() }
    val fontBody = rememberBodyFontFamily()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp)
    ) {
        byPage.forEach { (page, pageVerses) ->
            item(key = "page-$page") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PageDivider(pal = pal, fontBody = fontBody, page = page)
                    val paragraph = remember(pageVerses, fontName) {
                        pageVerses.joinToString(" ") { v ->
                            verseDisplayArabic(v, fontName)
                        }
                    }
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Rtl
                    ) {
                        Text(
                            text = paragraph,
                            fontSize = (26 * arabicScale).sp,
                            lineHeight = (52 * arabicScale * lineHeightMult).sp,
                            color = pal.ink,
                            fontFamily = fontArabic,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── Surah/Ayah/Page navigation dialog ──────────────────────────────────────
@Composable
private fun SurahNavigationDialog(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    currentChapterId: Int,
    onDismiss: () -> Unit,
    onOpenSurah: (Int, String?) -> Unit,
    onOpenPage: (Int) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var surahQuery by remember { mutableStateOf("") }
    var ayahInput by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf("") }
    val chapters = remember { QuranStore.chapters }
    val fontArabic = rememberArabicFontFamily()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = pal.white,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Navigation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = pal.inkMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Surah", "Ayah", "Page").forEachIndexed { i, label ->
                        val selected = tab == i
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { tab = i }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) pal.gold else pal.inkMuted,
                                fontFamily = fontUi
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(2.dp)
                                    .background(
                                        if (selected) pal.gold else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                when (tab) {
                    0 -> {
                        OutlinedTextField(
                            value = surahQuery,
                            onValueChange = { surahQuery = it },
                            placeholder = { Text("Search Surah by name or number...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = pal.inkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val filtered = remember(surahQuery, chapters) {
                            val q = surahQuery.trim().lowercase()
                            if (q.isEmpty()) chapters
                            else chapters.filter {
                                it.nameSimple.lowercase().contains(q) ||
                                    it.id.toString() == q
                            }
                        }
                        LazyColumn(modifier = Modifier.height(320.dp)) {
                            items(filtered, key = { it.id }) { chapter ->
                                val isCurrent = chapter.id == currentChapterId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenSurah(chapter.id, null) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) pal.gold else pal.boneDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chapter.id.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else pal.ink,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chapter.nameSimple,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) pal.gold else pal.ink,
                                            fontFamily = fontUi
                                        )
                                        Text(
                                            text = "${chapter.versesCount} verses",
                                            fontSize = 12.sp,
                                            color = pal.inkMuted
                                        )
                                    }
                                    Text(
                                        text = chapter.nameArabic,
                                        fontSize = 20.sp,
                                        color = pal.gold,
                                        fontFamily = fontArabic
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        val maxAyah = QuranStore.chapter(currentChapterId)?.versesCount ?: 286
                        OutlinedTextField(
                            value = ayahInput,
                            onValueChange = { ayahInput = it.filter(Char::isDigit).take(3) },
                            placeholder = { Text("Ayah Number e.g. 255") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verses 1–$maxAyah",
                            fontSize = 12.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                ayahInput.toIntOrNull()
                                    ?.takeIf { it in 1..maxAyah }
                                    ?.let { onOpenSurah(currentChapterId, "$currentChapterId:$it") }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pal.gold,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(48.dp).fillMaxWidth()
                        ) {
                            Text("Go to Ayah", fontWeight = FontWeight.Bold, fontFamily = fontUi)
                        }
                    }
                    2 -> {
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { pageInput = it.filter(Char::isDigit).take(3) },
                            placeholder = { Text("Page Number e.g. 293") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pages 1–604",
                            fontSize = 12.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                pageInput.toIntOrNull()
                                    ?.takeIf { it in 1..604 }
                                    ?.let(onOpenPage)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pal.gold,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(48.dp).fillMaxWidth()
                        ) {
                            Text("Go to Page", fontWeight = FontWeight.Bold, fontFamily = fontUi)
                        }
                    }
                }
            }
        }
    }
}
