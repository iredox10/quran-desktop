package com.nur.quran.desktop.ui.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * Keyboard + touch cheat-sheet. Opened from the sidebar footer (?) so
 * touchscreen users can discover swipe navigation too.
 */
@Composable
fun ShortcutHelpDialog(pal: NurPalette, onDismiss: () -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = pal.white) {
            Column(modifier = Modifier.padding(22.dp).fillMaxWidth()) {
                Text(
                    text = "Keyboard & touch",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Touch: tap everything, swipe left/right to turn pages in readers.",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppShortcuts.cheatSheet.forEach { (keys, action) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pal.cream,
                            ) {
                                Text(
                                    text = keys,
                                    fontFamily = fontUi,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.teal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = action,
                                fontFamily = fontBody,
                                fontSize = 12.sp,
                                color = pal.inkMid,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
