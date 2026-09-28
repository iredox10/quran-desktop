package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.desktop.data.AudioEngine
import com.nur.quran.desktop.data.BackupStore
import com.nur.quran.desktop.ui.audio.ReciterLibraryPanel
import com.nur.quran.shared.Reciters
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import java.io.File
import javax.swing.JFileChooser

private val MUSHAF_ROWS = listOf("Uthmani", "Indopak")

/**
 * Desktop Settings dialog mirroring Android `SettingsDrawer`
 * (General / Reading / Data tabs).
 *
 * Everything persists to [PrefsCache] immediately; [tick] re-reads prefs
 * after import/reset. After dismiss the coordinator re-reads prefs via its
 * own settingsTick — no extra callback needed.
 */
@Composable
fun SettingsDrawerDesktop(
    pal: NurPalette,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var tab by remember { mutableStateOf("General") }
    var tick by remember { mutableStateOf(0) }

    var translation by remember(tick) { mutableStateOf(PrefsCache.getTranslation()) }
    var mushaf by remember(tick) { mutableStateOf(PrefsCache.getMushaf()) }
    var font by remember(tick) { mutableStateOf(PrefsCache.getFont()) }
    var arabicScale by remember(tick) { mutableStateOf(PrefsCache.getArabicScale()) }
    var translationScale by remember(tick) { mutableStateOf(PrefsCache.getTranslationScale()) }
    var showTranslation by remember(tick) { mutableStateOf(PrefsCache.getReaderTranslationEnabled()) }
    var tajweed by remember(tick) { mutableStateOf(PrefsCache.getTajweedEnabled()) }
    var backupStatus by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pal.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).widthIn(min = 340.dp, max = 480.dp)
            ) {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontFamily = fontUi,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = pal.inkMuted)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Tab pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(pal.cream)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("General", "Reading", "Data").forEach { t ->
                        val selected = tab == t
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) pal.gold else pal.cream)
                                .clickable { tab = t }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = t,
                                fontFamily = fontUi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) pal.white else pal.inkMid
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    when (tab) {
                        "General" -> {
                            SectionLabel(pal, "APPEARANCE")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemePill(
                                    pal = pal, fontUi = fontUi, label = "Light",
                                    selected = !dark, modifier = Modifier.weight(1f),
                                    onClick = { if (dark) onToggleTheme() }
                                )
                                ThemePill(
                                    pal = pal, fontUi = fontUi, label = "Dark",
                                    selected = dark, modifier = Modifier.weight(1f),
                                    onClick = { if (!dark) onToggleTheme() }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "App language: English — more languages arrive with Downloads.",
                                fontFamily = fontBody,
                                fontSize = 11.sp,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SectionLabel(pal, "ESSENTIALS")
                            // Mushaf preset
                            SubLabel(pal, fontBody, "Mushaf preset")
                            MUSHAF_ROWS.forEach { option ->
                                SelectRow(
                                    pal = pal, fontUi = fontUi,
                                    title = option,
                                    selected = mushaf.equals(option, ignoreCase = true),
                                    onClick = {
                                        mushaf = option
                                        PrefsCache.putMushaf(option)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Reciter picker (selection drives the audio engine)
                            SubLabel(pal, fontBody, "Reciter")
                            ReciterLibraryPanel(
                                pal = pal,
                                reciters = Reciters.ALL,
                                selectedId = AudioEngine.reciterId,
                                onSelect = { AudioEngine.reciterId = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Translation picker (18 editions; packs download on demand)
                            SubLabel(pal, fontBody, "Translation")
                            TranslationPickerDesktop(
                                pal = pal,
                                selectedId = translation.toIntOrNull() ?: 20,
                                onPick = {
                                    translation = it.toString()
                                    PrefsCache.putTranslation(it.toString())
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Bundled translation: Saheeh International — other packs download on first use.",
                                fontFamily = fontBody,
                                fontSize = 11.sp,
                                color = pal.inkMuted
                            )
                        }

                        "Reading" -> {
                            SectionLabel(pal, "ARABIC FONT")
                            DesktopFonts.names.forEach { name ->
                                SelectRow(
                                    pal = pal, fontUi = fontUi,
                                    title = name,
                                    selected = font == name,
                                    onClick = {
                                        font = name
                                        PrefsCache.putFont(name)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionLabel(pal, "SIZES")
                            SliderRow(
                                pal = pal, fontUi = fontUi, fontBody = fontBody,
                                label = "Arabic size",
                                value = arabicScale,
                                range = 0.5f..2.0f,
                                onChange = {
                                    arabicScale = it
                                    PrefsCache.putArabicScale(it)
                                }
                            )
                            SliderRow(
                                pal = pal, fontUi = fontUi, fontBody = fontBody,
                                label = "Translation size",
                                value = translationScale,
                                range = 0.5f..2.0f,
                                onChange = {
                                    translationScale = it
                                    PrefsCache.putTranslationScale(it)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionLabel(pal, "DISPLAY")
                            SwitchRow(
                                pal = pal, fontUi = fontUi, fontBody = fontBody,
                                title = "Show translation",
                                subtitle = "Show translation under each verse",
                                checked = showTranslation,
                                onChecked = {
                                    showTranslation = it
                                    PrefsCache.putReaderTranslationEnabled(it)
                                }
                            )
                            SwitchRow(
                                pal = pal, fontUi = fontUi, fontBody = fontBody,
                                title = "Tajweed colors",
                                subtitle = "Tajweed colors arrive with the WebView renderer",
                                checked = tajweed,
                                onChecked = {
                                    tajweed = it
                                    PrefsCache.putTajweedEnabled(it)
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Memorize mode: hide the translation and test yourself from the Memorize tab.",
                                fontFamily = fontBody,
                                fontSize = 11.sp,
                                color = pal.inkMuted
                            )
                        }

                        "Data" -> {
                            SectionLabel(pal, "OFFLINE PACKS")
                            PackStatusLine(pal, fontBody, "Quran text", "Bundled — available offline")
                            PackStatusLine(pal, fontBody, "Saheeh International translation", "Bundled — available offline")
                            PackStatusLine(pal, fontBody, "Audio packs", "Streamed — more packs arrive with Downloads")
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionLabel(pal, "BACKUP & RESTORE")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ActionButton(
                                    pal = pal, fontUi = fontUi, label = "Export",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        try {
                                            val chooser = JFileChooser()
                                            chooser.dialogTitle = "Export Quran Nur backup"
                                            chooser.selectedFile = File("quran-nur-backup.json")
                                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                val target = chooser.selectedFile
                                                val ok = runCatching { BackupStore.exportToFile(target) }
                                                    .getOrDefault(false)
                                                backupStatus = if (ok) {
                                                    "Backup saved to ${target.absolutePath}"
                                                } else {
                                                    "Export failed — please try again"
                                                }
                                                if (ok) tick++
                                            }
                                        } catch (e: Exception) {
                                            backupStatus = "Export failed: ${e.message}"
                                        }
                                    }
                                )
                                ActionButton(
                                    pal = pal, fontUi = fontUi, label = "Import",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        try {
                                            val chooser = JFileChooser()
                                            chooser.dialogTitle = "Import Quran Nur backup"
                                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                val source = chooser.selectedFile
                                                val ok = runCatching { BackupStore.importFromFile(source) }
                                                    .getOrDefault(false)
                                                backupStatus = if (ok) {
                                                    "Backup restored from ${source.name}"
                                                } else {
                                                    "Import failed — invalid backup file"
                                                }
                                                if (ok) tick++
                                            }
                                        } catch (e: Exception) {
                                            backupStatus = "Import failed: ${e.message}"
                                        }
                                    }
                                )
                            }
                            if (backupStatus.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = backupStatus,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = pal.inkMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionLabel(pal, "DANGER ZONE")
                            ActionButton(
                                pal = pal, fontUi = fontUi, label = "Reset preferences",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    runCatching { PrefsCache.clear() }
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(pal: NurPalette, text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        color = pal.inkMuted,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SubLabel(pal: NurPalette, fontBody: FontFamily, text: String) {
    Text(
        text = text,
        fontFamily = fontBody,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = pal.inkMid,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ThemePill(
    pal: NurPalette,
    fontUi: FontFamily,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) pal.gold else pal.cream)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) pal.white else pal.inkMid
        )
    }
}

@Composable
private fun SelectRow(
    pal: NurPalette,
    fontUi: FontFamily,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) pal.goldSoft else pal.cream)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = fontUi,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) pal.teal else pal.ink,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = pal.teal, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SliderRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = fontBody,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = pal.ink
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = pal.inkMuted
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SwitchRow(
    pal: NurPalette,
    fontUi: FontFamily,
    fontBody: FontFamily,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onChecked(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = fontUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.ink
            )
            Text(
                text = subtitle,
                fontFamily = fontBody,
                fontSize = 11.sp,
                color = pal.inkMuted
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
    HorizontalDivider(color = pal.boneDark)
}

@Composable
private fun PackStatusLine(pal: NurPalette, fontBody: FontFamily, name: String, status: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(pal.cream)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = name,
            fontFamily = fontBody,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.ink
        )
        Text(
            text = status,
            fontFamily = fontBody,
            fontSize = 11.sp,
            color = pal.inkMuted
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ActionButton(
    pal: NurPalette,
    fontUi: FontFamily,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pal.teal)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = fontUi,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = pal.white
        )
    }
}
