package com.nur.quran.desktop.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.BackupStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import java.io.File
import javax.swing.JFileChooser

/**
 * Desktop local-backup card (no cloud sync on desktop).
 *
 * Cream card with a bone border mirroring the Android
 * `ProfileCloudSyncCard` / `SyncStatusCard` styling: status line plus
 * Export / Import action buttons backed by [BackupStore].
 */
@Composable
fun SyncCard(pal: NurPalette, modifier: Modifier = Modifier) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var status by remember { mutableStateOf("Desktop edition — data stays on this device") }
    var busy by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.5.dp, pal.boneDark)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(pal.goldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        tint = pal.gold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local backup",
                        fontFamily = fontUi,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    Text(
                        text = status,
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        busy = true
                        try {
                            val chooser = JFileChooser()
                            chooser.dialogTitle = "Export Quran Nur backup"
                            chooser.selectedFile = File("quran-nur-backup.json")
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val target = chooser.selectedFile
                                val ok = runCatching { BackupStore.exportToFile(target) }
                                    .getOrDefault(false)
                                status = if (ok) {
                                    "Backup saved ✓"
                                } else {
                                    "Export failed — please try again"
                                }
                            }
                        } catch (e: Exception) {
                            status = "Export failed: ${e.message}"
                        } finally {
                            busy = false
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pal.ink,
                        contentColor = pal.cream
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Export",
                        fontFamily = fontUi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                OutlinedButton(
                    onClick = {
                        busy = true
                        try {
                            val chooser = JFileChooser()
                            chooser.dialogTitle = "Import Quran Nur backup"
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val source = chooser.selectedFile
                                val ok = runCatching { BackupStore.importFromFile(source) }
                                    .getOrDefault(false)
                                status = if (ok) {
                                    "Restore complete — restart to apply all settings"
                                } else {
                                    "Import failed — invalid backup file"
                                }
                            }
                        } catch (e: Exception) {
                            status = "Import failed: ${e.message}"
                        } finally {
                            busy = false
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = pal.ink),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Import",
                        fontFamily = fontUi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Text(
                text = status,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = pal.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
