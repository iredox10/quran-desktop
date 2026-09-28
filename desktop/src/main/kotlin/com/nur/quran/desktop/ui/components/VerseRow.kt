package com.nur.quran.desktop.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.BookmarkStore
import com.nur.quran.desktop.data.DeskVerse
import com.nur.quran.desktop.data.TafsirStore
import com.nur.quran.shared.HtmlStripper
import com.nur.quran.shared.Verse
import com.nur.quran.shared.formatArabicDigits
import com.nur.quran.shared.mushafPlainVerseText
import com.nur.quran.shared.sajdahNumberFor
import com.nur.quran.shared.usesEmbeddedEndMarker
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily

/**
 * Flat verse item mirroring Android `VerseItem`: verse-key header with action
 * icons, Arabic block (shape-safe single-style rendering), translation, and
 * an expandable tafsir card. No cards — flat on the screen background, with a
 * gold-wash highlight when this is the scroll target.
 */
@Composable
fun VerseRow(
    verse: DeskVerse,
    pal: NurPalette,
    fontName: String = PrefsCache.getFont(),
    fontScale: Float = PrefsCache.getArabicScale(),
    translationScale: Float = PrefsCache.getTranslationScale(),
    lineHeightMultiplier: Float = PrefsCache.getLineHeightMult(),
    showTranslation: Boolean = PrefsCache.getReaderTranslationEnabled(),
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fontArabic = rememberArabicFontFamily(fontName)
    val fontBody = rememberBodyFontFamily()
    val mushafId = remember { PrefsCache.getMushaf() }
    val arabic = remember(verse, fontName, mushafId) {
        verseDisplayArabic(verse, fontName, mushafId)
    }
    var bookmarked by remember(verse.verseKey) {
        mutableStateOf(BookmarkStore.isBookmarked(verse.verseKey))
    }
    var tafsirOpen by remember(verse.verseKey) { mutableStateOf(false) }
    val tafsirText = remember(tafsirOpen, verse.verseKey) {
        if (tafsirOpen) TafsirStore.get(verse.verseKey)?.let(HtmlStripper::strip) else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pal.goldLight)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                } else {
                    Modifier.padding(horizontal = 40.dp, vertical = 20.dp)
                }
            )
    ) {
        // Header: verse key (+ sajdah badge) left, actions right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = verse.verseKey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.gold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.65.sp
                )
                sajdahNumberFor(verse.verseKey)?.let { n ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(pal.goldLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "۩ Sajdah $n",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.gold,
                            fontFamily = fontBody
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VerseActionIcon(
                    onClick = { bookmarked = BookmarkStore.toggle(verse.verseKey) },
                    active = bookmarked,
                    pal = pal,
                    description = "Bookmark Verse"
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = if (bookmarked) pal.gold else pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                VerseActionIcon(onClick = {
                    copyToClipboard("${verse.arabic}\n${verse.translation} — ${verse.verseKey}")
                }, pal = pal, description = "Share verse") {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                VerseActionIcon(
                    onClick = { tafsirOpen = !tafsirOpen },
                    active = tafsirOpen,
                    pal = pal,
                    description = "Read Tafsir"
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (tafsirOpen) pal.gold else pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            PlainVerseText(
                plainText = arabic,
                wordRanges = emptyList(),
                fontScale = fontScale,
                lineHeightMultiplier = lineHeightMultiplier,
                fontFamily = fontArabic,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showTranslation && verse.translation.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            TranslationTextDesktop(
                html = verse.translation,
                fontScale = translationScale,
                fontFamily = fontBody,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (tafsirOpen) {
            Spacer(modifier = Modifier.height(16.dp))
            TafsirCard(
                pal = pal,
                fontBody = fontBody,
                text = tafsirText ?: "Loading tafsir…",
                onClose = { tafsirOpen = false }
            )
        }
    }
}

/** 32.dp circular action button, mirroring Android `VerseActionIcon`. */
@Composable
private fun VerseActionIcon(
    onClick: () -> Unit,
    pal: NurPalette,
    active: Boolean = false,
    description: String,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (active) pal.goldLight else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/** Tafsir card with gold left bar, mirroring Android `FootnoteCard`. */
@Composable
private fun TafsirCard(
    pal: NurPalette,
    fontBody: FontFamily,
    text: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .drawGoldBar(pal)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = pal.gold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tafsir — Ibn Kathir",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = pal.ink,
                fontFamily = fontBody
            )
        }
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close tafsir",
            tint = pal.inkMuted,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onClose)
        )
    }
}

private fun Modifier.drawGoldBar(pal: NurPalette): Modifier = this.drawBehind {
    val barWidth = 3.dp.toPx()
    drawRect(
        color = pal.gold,
        topLeft = Offset.Zero,
        size = Size(barWidth, size.height)
    )
}

/**
 * Display-ready Arabic for one verse, mirroring the Android pipeline:
 * mushaf script selection + per-font ornament canonicalization via the shared
 * [mushafPlainVerseText] (this is what strips marks like U+06DF ۟ that have
 * no business on screen), plus exactly one end-of-ayah marker. KFGQPC Hafs
 * shapes bare digits into a medallion via GSUB; every other font needs the
 * U+06DD frame.
 */
internal fun verseDisplayArabic(
    verse: DeskVerse,
    fontName: String,
    mushafId: String = PrefsCache.getMushaf()
): String {
    val v = Verse(
        id = 0,
        verseKey = verse.verseKey,
        chapterId = verse.chapterId,
        verseNumber = verse.verseNumber,
        textUthmani = verse.arabic,
        textIndopak = verse.textIndopak.ifBlank { null },
        textQpcHafs = null,
        pageNumber = verse.pageNumber,
        juzNumber = 0
    )
    val clean = mushafPlainVerseText(v, mushafId, fontName).trimEnd()
    val digits = formatArabicDigits(verse.verseNumber)
    val marker = if (usesEmbeddedEndMarker(fontName)) " $digits" else " \u06DD$digits"
    return "$clean$marker"
}
