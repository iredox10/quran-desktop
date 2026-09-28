package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.PageWord
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.WordStore
import com.nur.quran.desktop.ui.components.MushafPageLines
import com.nur.quran.desktop.ui.components.WordRef
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.theme.NurPalette
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
    modifier: Modifier = Modifier
) {
    val verses = remember(pageNumber) { QuranStore.versesOfPage(pageNumber) }
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

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
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
            LazyColumn(modifier = Modifier.weight(1f)) {
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
                    ) {
                        MushafPageLines(
                            wordsByLine = wordsByLine,
                            scale = readerPrefs.second
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
