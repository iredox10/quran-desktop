package com.nur.quran.desktop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.desktop.ui.theme.timeAgo

/**
 * "Recently read" horizontal card strip for the homepage — 1:1 parity with
 * web Home.jsx (BookOpen icon, vertical card format, up to 6 items).
 */
@Composable
fun RecentlyReadRow(
    pal: NurPalette,
    entries: List<RecentlyReadStore.Entry>,
    onOpen: (chapterId: Int, verseKey: String?) -> Unit,
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val fontUi = rememberUiFontFamily()
    val displayEntries = entries.take(6)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = pal.ink,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Recently Read",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.ink,
                fontFamily = fontUi
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val rowScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rowScrollState)
                .dragToScroll(rowScrollState, orientation = Orientation.Horizontal)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            displayEntries.forEach { item ->
                Surface(
                    onClick = { onOpen(item.chapterId, item.verseKey) },
                    shape = RoundedCornerShape(14.dp),
                    color = pal.cream,
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    modifier = Modifier.widthIn(min = 155.dp, max = 180.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(pal.tealSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.chapterId.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.teal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.chapterName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.ink,
                            fontFamily = fontUi,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.verseKey != null) {
                            val verseNum = item.verseKey.substringAfter(":", item.verseKey)
                            Text(
                                text = "Verse $verseNum",
                                fontSize = 11.sp,
                                color = pal.inkMuted
                            )
                        }
                        if (item.timestamp > 0L) {
                            Text(
                                text = timeAgo(item.timestamp),
                                fontSize = 11.sp,
                                color = pal.inkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
