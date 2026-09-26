package com.nur.quran.desktop.ui.audio

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.Reciter

/**
 * Currently chosen reciter — honest desktop version.
 *
 * Teal card with a Tune glyph, "Recitation" label, the reciter name and a
 * Change button. No playback controls: the desktop app has no audio backend
 * yet, so the choice is only saved for a future update.
 */
@Composable
fun ChosenReciterCard(
    pal: NurPalette,
    reciterName: String,
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(pal.teal)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "♫", fontSize = 22.sp, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Recitation",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = reciterName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontUi,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Saved for future audio streaming",
                fontSize = 12.sp,
                fontFamily = fontBody,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        TextButton(
            onClick = onChange,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Text(
                text = "Change",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontUi,
                color = Color.White
            )
        }
    }
}

/**
 * Searchable reciter list. Rows show a number badge + name with a check mark
 * on the selected row. Selecting a row only calls [onSelect] — the caller
 * persists the choice. No preview/download/play controls (no audio backend).
 */
@Composable
fun ReciterLibraryPanel(
    pal: NurPalette,
    reciters: List<Reciter>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, reciters) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) reciters
        else reciters.filter { it.name.lowercase().contains(q) }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search reciters…",
                    color = pal.inkMuted,
                    fontFamily = fontBody,
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        filtered.forEachIndexed { index, reciter ->
            val selected = reciter.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) pal.goldSoft else pal.cream)
                    .border(
                        1.dp,
                        if (selected) pal.gold else pal.boneDark,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(reciter.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (selected) pal.gold.copy(alpha = 0.2f) else pal.bone),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (selected) pal.gold else pal.inkMuted
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciter.name,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = fontUi,
                        color = if (selected) pal.gold else pal.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reciter.style,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = pal.inkMuted
                    )
                }
                if (selected) {
                    Text(
                        text = "✓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.gold
                    )
                }
            }
        }
    }
}

/** Honest placeholder: no dead play buttons, just what the choice is for. */
@Composable
fun AudioComingSoonNote(
    pal: NurPalette,
    modifier: Modifier = Modifier
) {
    val fontBody = rememberBodyFontFamily()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.goldSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Audio streaming arrives in a future update — reciter choice is saved for then.",
            fontSize = 13.sp,
            fontFamily = fontBody,
            color = pal.ink
        )
    }
}
