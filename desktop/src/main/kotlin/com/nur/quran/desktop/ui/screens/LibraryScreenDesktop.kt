package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.nur.quran.desktop.data.BookmarkStore
import com.nur.quran.desktop.data.CollectionStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.HtmlStripper
import java.io.File
import javax.swing.JFileChooser

/**
 * Desktop Library screen mirroring Android `LibraryScreen` +
 * `ui/components/library/BookmarksSection.kt` / `CollectionsSection.kt`.
 *
 * - Bookmarks section: verse rows with gold mono key + arabic snippet + surah name.
 * - Collections section: cards with create / rename / delete + verse rows with open/remove,
 *   plus a "quick-add" row pulling from bookmarked verses not yet in the collection.
 */
@Composable
fun LibraryScreenDesktop(
    pal: NurPalette,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> }
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val fontArabic = rememberArabicFontFamily()
    var bookmarkTick by remember { mutableStateOf(0) }
    var collectionTick by remember { mutableStateOf(0) }

    val bookmarks: List<String> = remember(bookmarkTick) {
        runCatching { BookmarkStore.all().toList().sorted() }.getOrDefault(emptyList())
    }
    val collections: List<CollectionStore.Collection> = remember(collectionTick) {
        runCatching { CollectionStore.listCollections() }.getOrDefault(emptyList())
    }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
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
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Library",
                    fontFamily = fontUi,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Text(
                    text = "${bookmarks.size} bookmarks • ${collections.size} collections",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item {
                    BookmarksSectionDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        fontArabic = fontArabic,
                        bookmarks = bookmarks,
                        onOpenSurah = onOpenSurah,
                        onRemove = { key ->
                            runCatching { BookmarkStore.toggle(key) }
                            bookmarkTick++
                            collectionTick++
                        }
                    )
                }
                item {
                    CollectionsSectionDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        fontArabic = fontArabic,
                        collections = collections,
                        bookmarks = bookmarks,
                        onOpenSurah = onOpenSurah,
                        onMutated = { collectionTick++ }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Bookmarks ────────────────────────────────────────────────────────────────

@Composable
private fun BookmarksSectionDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    fontArabic: FontFamily,
    bookmarks: List<String>,
    onOpenSurah: (Int, String?) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Bookmarks",
                fontFamily = fontUi,
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(pal.cream)
                    .border(1.dp, pal.boneDark, RoundedCornerShape(20.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bookmarks yet — tap the bookmark icon on any verse",
                    fontFamily = fontBody,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bookmarks.forEach { key ->
                    BookmarkRowDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        fontArabic = fontArabic,
                        verseKey = key,
                        onOpen = {
                            val chapterId = key.substringBefore(":").toIntOrNull() ?: 1
                            onOpenSurah(chapterId, key)
                        },
                        onRemove = { onRemove(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkRowDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    fontArabic: FontFamily,
    verseKey: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val chapterId = verseKey.substringBefore(":").toIntOrNull()
    val chapter = chapterId?.let { runCatching { QuranStore.chapter(it) }.getOrNull() }
    val verse = chapterId?.let {
        runCatching { QuranStore.versesOfChapter(it).firstOrNull { v -> v.verseKey == verseKey } }.getOrNull()
    }
    val arabic = verse?.arabic.orEmpty().take(60)
    val surahName = chapter?.nameSimple ?: "Surah $chapterId"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = verseKey,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = pal.gold,
                modifier = Modifier.width(52.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                if (arabic.isNotEmpty()) {
                    Text(
                        text = arabic,
                        fontFamily = fontArabic,
                        fontSize = 16.sp,
                        color = pal.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = surahName,
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open verse",
                tint = pal.gold,
                modifier = Modifier.size(16.dp)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove bookmark",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Collections ──────────────────────────────────────────────────────────────

@Composable
private fun CollectionsSectionDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    fontArabic: FontFamily,
    collections: List<CollectionStore.Collection>,
    bookmarks: List<String>,
    onOpenSurah: (Int, String?) -> Unit,
    onMutated: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Collections",
                fontFamily = fontUi,
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

        // Create bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(pal.cream)
                .border(1.dp, pal.boneDark, RoundedCornerShape(14.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (newName.isEmpty()) {
                    Text(
                        text = "New collection name...",
                        fontFamily = fontBody,
                        fontSize = 13.sp,
                        color = pal.inkMuted
                    )
                }
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = fontBody, fontSize = 13.sp, color = pal.ink),
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
                            runCatching { CollectionStore.createCollection(newName.trim()) }
                            newName = ""
                            onMutated()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Create",
                        fontFamily = fontBody,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(pal.cream)
                    .border(1.dp, pal.boneDark, RoundedCornerShape(20.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No collections yet. Group verses together for better hifdh focus.",
                    fontFamily = fontBody,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                collections.forEach { collection ->
                    CollectionCardDesktop(
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        fontArabic = fontArabic,
                        collection = collection,
                        bookmarks = bookmarks,
                        onOpenSurah = onOpenSurah,
                        onMutated = onMutated
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionCardDesktop(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    fontArabic: FontFamily,
    collection: CollectionStore.Collection,
    bookmarks: List<String>,
    onOpenSurah: (Int, String?) -> Unit,
    onMutated: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draftName by remember(collection.id) { mutableStateOf(collection.name) }
    var exportStatus by remember(collection.id) { mutableStateOf("") }

    val items: List<String> = remember(collection.id, bookmarks) {
        runCatching { CollectionStore.items(collection.id) }.getOrDefault(emptyList())
    }
    val quickAdd: List<String> = remember(items, bookmarks) {
        bookmarks.filter { it !in items }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    if (editing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pal.white)
                                    .border(1.dp, pal.boneDark, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                BasicTextField(
                                    value = draftName,
                                    onValueChange = { draftName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = fontBody,
                                        fontSize = 14.sp,
                                        color = pal.ink
                                    ),
                                    cursorBrush = SolidColor(pal.gold),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (draftName.isNotBlank()) {
                                        runCatching {
                                            CollectionStore.renameCollection(
                                                collection.id,
                                                draftName.trim()
                                            )
                                        }
                                        editing = false
                                        onMutated()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Save name",
                                    tint = pal.teal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    editing = false
                                    draftName = collection.name
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel rename",
                                    tint = pal.inkMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = collection.name,
                            fontFamily = fontUi,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.ink
                        )
                    }
                    Text(
                        text = "${items.size} verses",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = pal.inkMuted
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (items.isEmpty()) {
                                exportStatus = "Nothing to export"
                            } else {
                                try {
                                    val safeName = collection.name
                                        .replace(Regex("[\\\\/:*?\"<>|]"), "-")
                                        .trim()
                                        .ifBlank { "collection" }
                                    val chooser = JFileChooser()
                                    chooser.dialogTitle = "Export ${collection.name}"
                                    chooser.selectedFile = File("$safeName.txt")
                                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                        var target = chooser.selectedFile
                                        if (target.extension.isBlank()) {
                                            target = File(target.parentFile, "${target.name}.txt")
                                        }
                                        val sb = StringBuilder()
                                        items.forEachIndexed { index, key ->
                                            val chapterId = key.substringBefore(":").toIntOrNull()
                                            val chapter = chapterId?.let {
                                                runCatching { QuranStore.chapter(it) }.getOrNull()
                                            }
                                            val verse = chapterId?.let {
                                                runCatching {
                                                    QuranStore.versesOfChapter(it)
                                                        .firstOrNull { v -> v.verseKey == key }
                                                }.getOrNull()
                                            }
                                            val surahName = chapter?.nameSimple ?: "Surah $chapterId"
                                            sb.appendLine("$surahName $key")
                                            sb.appendLine(verse?.arabic.orEmpty())
                                            sb.appendLine(HtmlStripper.strip(verse?.translation.orEmpty()))
                                            if (index < items.size - 1) sb.appendLine()
                                        }
                                        target.writeText(sb.toString())
                                        exportStatus = "Saved to ${target.absolutePath}"
                                    }
                                } catch (e: Exception) {
                                    exportStatus = "Export failed: ${e.message}"
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Export",
                            fontFamily = fontBody,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.ink
                        )
                    }
                    if (!editing) {
                        IconButton(
                            onClick = {
                                draftName = collection.name
                                editing = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Rename collection",
                                tint = pal.inkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            runCatching { CollectionStore.deleteCollection(collection.id) }
                            onMutated()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete collection",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "No verses in this collection.",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { key ->
                        CollectionVerseRowDesktop(
                            pal = pal,
                            fontBody = fontBody,
                            verseKey = key,
                            onOpen = {
                                val chapterId = key.substringBefore(":").toIntOrNull() ?: 1
                                onOpenSurah(chapterId, key)
                            },
                            onRemove = {
                                runCatching { CollectionStore.removeItem(collection.id, key) }
                                onMutated()
                            }
                        )
                    }
                }
            }

            if (quickAdd.isNotEmpty()) {
                Text(
                    text = "Bookmark list quick-add",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickAdd.forEach { key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(pal.white)
                                .border(1.dp, pal.boneDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = key,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.gold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pal.teal)
                                    .clickable {
                                        runCatching { CollectionStore.addItem(collection.id, key) }
                                        onMutated()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add $key to ${collection.name}",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                }
            }
            if (exportStatus.isNotBlank()) {
                Text(
                    text = exportStatus,
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
        }
    }
}

@Composable
private fun CollectionVerseRowDesktop(
    pal: NurPalette,
    fontBody: FontFamily,
    verseKey: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val chapterId = verseKey.substringBefore(":").toIntOrNull()
    val chapterName = chapterId?.let {
        runCatching { QuranStore.chapter(it)?.nameSimple }.getOrNull()
    } ?: "Surah $chapterId"
    val ayahNum = verseKey.substringAfter(":", verseKey)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(pal.white)
            .border(1.dp, pal.boneDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$chapterName $ayahNum",
            fontFamily = fontBody,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = pal.ink,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpen, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Read verse",
                    tint = pal.gold,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove verse",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
