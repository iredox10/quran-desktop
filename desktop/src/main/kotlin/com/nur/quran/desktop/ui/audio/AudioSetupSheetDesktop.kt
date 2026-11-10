package com.nur.quran.desktop.ui.audio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.Reciters

/**
 * "Audio setup" sheet opened from the mini player gear: reciter picker,
 * repeat mode (off / ayah / chapter), and sleep timer (off / 15 / 30 / 60).
 * Every control writes immediately to [AudioEngine].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSetupSheetDesktop(
    pal: NurPalette,
    onDismiss: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = pal.cream,
        contentColor = pal.ink
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Audio setup",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                fontFamily = fontUi
            )

            // ── Reciter ──
            Text(
                text = "RECITER",
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = FontFamily.Monospace
            )
            ReciterLibraryPanel(
                pal = pal,
                reciters = Reciters.ALL,
                selectedId = AudioEngine.reciterId,
                onSelect = { AudioEngine.reciterId = it },
                modifier = Modifier.height(300.dp)
            )

            // ── Repeat ayah ──
            Text(
                text = "REPEAT AYAH",
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "Once", 2 to "2×", 3 to "3×", 5 to "5×", -1 to "∞").forEach { (count, label) ->
                    Pill(
                        pal = pal,
                        label = label,
                        selected = AudioEngine.repeatAyah == count,
                        onClick = { AudioEngine.repeatAyah = count },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Repeat selection ──
            Text(
                text = "REPEAT SELECTION",
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "Once", 2 to "2×", 3 to "3×", -1 to "∞").forEach { (count, label) ->
                    Pill(
                        pal = pal,
                        label = label,
                        selected = AudioEngine.repeatSelection == count,
                        onClick = { AudioEngine.repeatSelection = count },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Delay between ayahs ──
            Text(
                text = "DELAY BETWEEN AYAHS",
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "None", 1 to "1s", 2 to "2s", 3 to "3s", 5 to "5s").forEach { (seconds, label) ->
                    Pill(
                        pal = pal,
                        label = label,
                        selected = AudioEngine.delaySeconds == seconds,
                        onClick = { AudioEngine.delaySeconds = seconds },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Sleep timer ──
            Text(
                text = "SLEEP TIMER",
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.gold,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 15, 30, 60).forEach { minutes ->
                    Pill(
                        pal = pal,
                        label = if (minutes == 0) "Off" else "$minutes min",
                        selected = AudioEngine.sleepMinutes == minutes,
                        onClick = { AudioEngine.sleepMinutes = minutes },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Pill(
    pal: NurPalette,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) pal.tealSoft else Color.Transparent,
        border = BorderStroke(1.5.dp, if (selected) pal.teal else pal.boneDark),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) pal.teal else pal.inkMid,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
