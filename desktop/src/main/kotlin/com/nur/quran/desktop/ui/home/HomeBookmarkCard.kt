package com.nur.quran.desktop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * Desktop Bookmark card mirroring web `Home.jsx` (lines 301-312):
 * - Gold card with `pal.gold` border, `pal.goldSoft` background
 * - Bookmark icon with 12% gold background
 * - Surah name in gold `fontUi`
 * - "Verse X · Resume reading" in `fontBody`
 * - ArrowRight icon in gold
 */
@Composable
fun HomeBookmarkCard(
    pal: NurPalette,
    bookmarkKey: String,
    onOpenSurah: (chapterId: Int, verseKey: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bookmarkKey.isBlank()) return

    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    val parts = bookmarkKey.split(":")
    val chapterId = parts.getOrNull(0)?.toIntOrNull() ?: 1
    val verseNumber = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val surah = QuranStore.chapter(chapterId)
    val surahName = surah?.nameSimple ?: "Surah $chapterId"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenSurah(chapterId, bookmarkKey) },
        shape = RoundedCornerShape(14.dp),
        color = pal.goldSoft,
        border = BorderStroke(1.5.dp, pal.gold),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.gold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "Bookmark",
                    tint = pal.gold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surahName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.gold,
                    fontFamily = fontUi
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Verse $verseNumber · Resume reading",
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    fontFamily = fontBody
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Resume reading",
                tint = pal.gold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
