package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.theme.NurPalette

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

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(verses, key = { it.verseKey }) { verse ->
                VerseRow(
                    verse = verse,
                    pal = pal,
                    fontName = readerPrefs.first,
                    fontScale = readerPrefs.second,
                    showTranslation = readerPrefs.third
                )
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
