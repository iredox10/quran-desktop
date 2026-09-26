package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Room-free word reference for the desktop Mushaf line layout.
 * Port of the grouping in MushafPageView.kt (groups WordEntity by word.lineNumber).
 */
data class WordRef(
    val text: String,
    val dimmed: Boolean = false
)

/**
 * 15-line page-accurate Mushaf line layout (desktop port).
 *
 * Takes pre-grouped [wordsByLine] keyed by lineNumber (1..15) and renders each
 * line as a justified [Row] (SpaceBetween), mirroring MushafPageView.kt.
 * Falls back to continuous reading when no line numbers are available.
 */
@Composable
fun MushafPageLines(
    wordsByLine: Map<Int, List<WordRef>>,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    if (wordsByLine.isEmpty()) {
        // Fallback to continuous reading when line numbers are unavailable.
        Text(
            text = "Continuous reading fallback — no line numbers available.",
            fontSize = (14 * scale).sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(16.dp)
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (lineNumber in 1..15) {
            val words = wordsByLine[lineNumber] ?: continue
            if (words.isEmpty()) continue
            val isSingleItemLine = words.size <= 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isSingleItemLine) Arrangement.Center else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                words.forEach { word ->
                    Text(
                        text = word.text,
                        fontSize = (20 * scale).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(if (word.dimmed) 0.25f else 1.0f)
                    )
                }
            }
        }
    }
}
