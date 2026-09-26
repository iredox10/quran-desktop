package com.nur.quran.desktop.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.CollectionStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * Bookmarks section: "Bookmarks" title + count chip, bookmark rows
 * (gold mono verseKey, surah name, arabic snippet; click opens, X removes).
 */
@Composable
fun BookmarksSection(
    pal: NurPalette,
    bookmarks: List<String>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val uiFont = rememberUiFontFamily()
    val bodyFont = rememberBodyFontFamily()
    val arabicFont = rememberArabicFontFamily()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Bookmarks",
                fontFamily = uiFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
            if (bookmarks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pal.goldLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${bookmarks.size}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.gold
                    )
                }
            }
        }

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.border, RoundedCornerShape(16.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bookmarks yet. Save your favorite ayahs to see them here.",
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bookmarks.forEach { key ->
                    val chapterId = key.substringBefore(":").toIntOrNull() ?: 0
                    val surahName = QuranStore.chapter(chapterId)?.nameSimple ?: ""
                    val snippet = QuranStore.versesOfChapter(chapterId)
                        .firstOrNull { it.verseKey == key }?.arabic?.take(60).orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(pal.surface)
                            .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                            .clickable { onOpen(key) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = key,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.gold
                                )
                                if (surahName.isNotBlank()) {
                                    Text(
                                        text = surahName,
                                        fontFamily = bodyFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = pal.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (snippet.isNotBlank()) {
                                Text(
                                    text = snippet,
                                    fontFamily = arabicFont,
                                    fontSize = 14.sp,
                                    color = pal.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRemove(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontFamily = bodyFont,
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Collections section: "Collections" title + inline create field,
 * collection cards (inline-editable name + save, count chip, delete),
 * verse rows with open/remove.
 */
@Composable
fun CollectionsSection(
    pal: NurPalette,
    collections: List<CollectionStore.Collection>,
    items: Map<String, List<String>>,
    onOpenVerse: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRemoveItem: (String, String) -> Unit
) {
    val uiFont = rememberUiFontFamily()
    val bodyFont = rememberBodyFontFamily()
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Collections",
                fontFamily = uiFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
            if (collections.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pal.goldLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${collections.size}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.gold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(pal.surface)
                .border(1.dp, pal.border, RoundedCornerShape(14.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (newName.isEmpty()) {
                    Text(
                        text = "New Collection Name...",
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        color = pal.inkMuted
                    )
                }
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        color = pal.ink
                    ),
                    cursorBrush = SolidColor(pal.gold),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(pal.gold)
                    .clickable {
                        if (newName.isNotBlank()) {
                            onCreate(newName.trim())
                            newName = ""
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Create",
                    fontFamily = bodyFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.border, RoundedCornerShape(16.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No collections yet. Group verses together for better hifdh focus.",
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                collections.forEach { collection ->
                    CollectionCard(
                        pal = pal,
                        collection = collection,
                        verses = items[collection.id].orEmpty(),
                        onOpenVerse = onOpenVerse,
                        onRename = { onRename(collection.id, it) },
                        onDelete = { onDelete(collection.id) },
                        onRemoveItem = { onRemoveItem(collection.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    pal: NurPalette,
    collection: CollectionStore.Collection,
    verses: List<String>,
    onOpenVerse: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    val uiFont = rememberUiFontFamily()
    val bodyFont = rememberBodyFontFamily()
    var editing by remember(collection.id) { mutableStateOf(false) }
    var draft by remember(collection.id) { mutableStateOf(collection.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(pal.cream)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = bodyFont,
                                fontSize = 15.sp,
                                color = pal.ink
                            ),
                            cursorBrush = SolidColor(pal.gold),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pal.gold)
                            .clickable {
                                if (draft.isNotBlank()) {
                                    onRename(draft.trim())
                                    editing = false
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save",
                            fontFamily = bodyFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Text(
                    text = collection.name,
                    fontFamily = uiFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { editing = true; draft = collection.name }
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pal.goldLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${verses.size}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.gold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1ADC2626))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗑",
                        fontSize = 14.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        if (verses.isEmpty()) {
            Text(
                text = "No verses in this collection.",
                fontFamily = bodyFont,
                fontSize = 13.sp,
                color = pal.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                verses.forEach { key ->
                    val chapterId = key.substringBefore(":").toIntOrNull() ?: 0
                    val surahName = QuranStore.chapter(chapterId)?.nameSimple ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.cream)
                            .clickable { onOpenVerse(key) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (surahName.isNotBlank()) "$surahName $key" else key,
                            fontFamily = bodyFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = pal.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onRemoveItem(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontFamily = bodyFont,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}
