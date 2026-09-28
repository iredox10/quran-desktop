package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.shared.HtmlStripper
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser

/**
 * Share dialog mirroring Android `ShareVerseDialog`: a preview card with the
 * verse text, translation and reference, plus copy-to-clipboard and
 * save-as-PNG actions. The PNG is rendered offscreen with [ImageComposeScene]
 * (same pattern as the headless `ScreenshotTest` renders) reusing [ShareCard],
 * so preview and export always match.
 */
@Composable
fun ShareDialogDesktop(
    pal: NurPalette,
    arabic: String,
    translation: String,
    verseRef: String,
    onDismiss: () -> Unit
) {
    val fontBody = rememberBodyFontFamily()
    val plainTranslation = remember(translation) { HtmlStripper.strip(translation) }
    var copied by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pal.white,
            border = BorderStroke(1.5.dp, pal.boneDark),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp).widthIn(min = 340.dp, max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share verse",
                        fontFamily = fontBody,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = pal.inkMuted)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                ShareCard(
                    pal = pal,
                    arabic = arabic,
                    translation = translation,
                    verseRef = verseRef,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            copied = copyToClipboard("$arabic\n$plainTranslation — $verseRef")
                            status = if (copied) "Copied to clipboard" else "Copy failed"
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.teal,
                            contentColor = pal.cream
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (copied) "Copied" else "Copy text",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val slug = verseRef.lowercase()
                                    .replace(Regex("[^a-z0-9]+"), "-")
                                    .trim('-')
                                    .ifBlank { "verse" }
                                val chooser = JFileChooser()
                                chooser.dialogTitle = "Save verse image"
                                chooser.selectedFile = File("quran-$slug.png")
                                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val target = chooser.selectedFile
                                    renderSharePng(
                                        pal = pal,
                                        arabic = arabic,
                                        translation = translation,
                                        verseRef = verseRef,
                                        target = target
                                    )
                                    status = "Saved to ${target.absolutePath}"
                                    try {
                                        target.parentFile?.let { Desktop.getDesktop().open(it) }
                                    } catch (_: Exception) {
                                        // Opening the folder is best-effort; the file is saved.
                                    }
                                }
                            } catch (e: Exception) {
                                status = "Save failed: ${e.message}"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Save PNG",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.ink
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.inkMuted
                        )
                    }
                }

                if (status.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        fontSize = 12.sp,
                        fontFamily = fontBody,
                        color = pal.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Cream share card with a faint Bismillah watermark top-end (like
 * `VerseOfDayCard`): centered Arabic, translation, and a gold mono reference.
 * Shared by the dialog preview and the offscreen PNG render.
 */
@Composable
private fun ShareCard(
    pal: NurPalette,
    arabic: String,
    translation: String,
    verseRef: String,
    modifier: Modifier = Modifier
) {
    val fontArabic = rememberArabicFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = pal.cream,
        border = BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "﷽",
                fontSize = 96.sp,
                color = pal.gold.copy(alpha = 0.06f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
            )
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = arabic,
                    fontFamily = fontArabic,
                    fontSize = 24.sp,
                    lineHeight = 48.sp,
                    textAlign = TextAlign.Center,
                    color = pal.ink,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TranslationTextDesktop(
                    html = translation,
                    fontScale = 1f,
                    fontFamily = fontBody,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "— $verseRef",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.gold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.65.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Renders [ShareCard] offscreen into [target] as a PNG (1080x1350). */
private fun renderSharePng(
    pal: NurPalette,
    arabic: String,
    translation: String,
    verseRef: String,
    target: File
) {
    val scene = ImageComposeScene(width = 1080, height = 1350) {
        NurTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = pal.white
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShareCard(
                        pal = pal,
                        arabic = arabic,
                        translation = translation,
                        verseRef = verseRef,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    try {
        val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
            ?: error("PNG encode failed")
        target.writeBytes(png.bytes)
    } finally {
        scene.close()
    }
}
