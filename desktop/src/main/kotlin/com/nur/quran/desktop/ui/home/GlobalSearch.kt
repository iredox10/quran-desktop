package com.nur.quran.desktop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

private const val SEARCH_LIMIT = 30

/**
 * Whole-Quran full-text search box for HomeScreen.
 * In-memory filter is fast, so results are computed directly in remember(query).
 */
@Composable
fun GlobalSearch(
    pal: NurPalette,
    onOpenSurah: (Int, String?) -> Unit,
    onOpenPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()

    val results = remember(query) { QuranStore.searchVerses(query, SEARCH_LIMIT) }
    val isActive = query.trim().length >= 2

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    text = "Search the whole Quran — Arabic or translation…",
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
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
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

        Spacer(modifier = Modifier.height(12.dp))

        if (!isActive) {
            Text(
                text = "Type at least 2 characters to search Arabic text and translations across all 114 surahs.",
                fontSize = 12.sp,
                fontFamily = fontBody,
                color = pal.inkMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        } else if (results.isEmpty()) {
            Text(
                text = "No verses found for \"$query\". Try different words.",
                fontSize = 13.sp,
                fontFamily = fontUi,
                color = pal.inkMuted,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEach { verse ->
                    Surface(
                        onClick = { onOpenSurah(verse.chapterId, verse.verseKey) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = pal.cream,
                        border = BorderStroke(1.5.dp, pal.boneDark)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = verse.verseKey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = pal.gold,
                                modifier = Modifier.width(52.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = com.nur.quran.desktop.ui.components.verseSnippetArabic(verse),
                                    fontFamily = fontArabic,
                                    fontSize = 17.sp,
                                    color = pal.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stripHtml(verse.translation),
                                    fontSize = 12.sp,
                                    fontFamily = fontBody,
                                    color = pal.inkMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pal.bone)
                                    .clickable { onOpenPage(verse.pageNumber) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "p.${verse.pageNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = pal.teal
                                )
                            }
                        }
                    }
                }
                if (results.size >= SEARCH_LIMIT) {
                    Text(
                        text = "${results.size} results — showing first $SEARCH_LIMIT",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = pal.inkMuted,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun stripHtml(html: String): String =
    html.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
