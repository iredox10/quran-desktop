package com.nur.quran.desktop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import androidx.compose.foundation.BorderStroke
import com.nur.quran.desktop.data.RecentlyReadStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.desktop.ui.theme.timeAgo

/**
 * "Recently read" horizontal card strip for the homepage — mirrors the
 * Android continue-reading history. Empty when [entries] is empty.
 */
@Composable
fun RecentlyReadRow(
    pal: NurPalette,
    entries: List<RecentlyReadStore.Entry>,
    onOpen: (chapterId: Int, verseKey: String?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val fontUi = rememberUiFontFamily()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = pal.gold,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "RECENTLY READ",
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Clear",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.inkMid,
                fontFamily = fontUi,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClear() }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            entries.forEach { entry ->
                Surface(
                    onClick = { onOpen(entry.chapterId, entry.verseKey) },
                    shape = RoundedCornerShape(14.dp),
                    color = pal.cream,
                    border = BorderStroke(1.dp, pal.boneDark)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(pal.tealSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.chapterId.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.teal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Column {
                            Text(
                                text = entry.chapterName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.ink,
                                fontFamily = fontUi,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(
                                    entry.verseKey?.let { "v$it" },
                                    if (entry.timestamp > 0) timeAgo(entry.timestamp) else null
                                ).joinToString(" · "),
                                fontSize = 10.sp,
                                color = pal.inkMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
