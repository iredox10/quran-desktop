package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.TranslationStore
import com.nur.quran.desktop.ui.theme.NurPalette

/**
 * Translation edition picker (wired by the coordinator later).
 *
 * Dropdown listing [TranslationStore.KNOWN_EDITIONS]; each row carries a
 * trailing badge — "Bundled" for id 20, "Cached" when chapter 1 is cached
 * offline, nothing otherwise. Selection is reported via [onPick] as the
 * resource id (persist it with `PrefsCache.putTranslation(id.toString())`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPickerDesktop(
    pal: NurPalette,
    selectedId: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // Snapshot of chapter-1 cache state; re-evaluated when the selection
    // changes (downloads/deletes trigger recomposition from the parent).
    val cachedIds = remember(selectedId) {
        TranslationStore.KNOWN_EDITIONS.mapNotNull { (id, _) ->
            if (id != TranslationStore.BUNDLED_ID && TranslationStore.isCached(1, id)) id else null
        }.toSet()
    }
    val selectedName = remember(selectedId) { TranslationStore.nameOf(selectedId) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "$selectedName ($selectedId)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Translation") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TranslationStore.KNOWN_EDITIONS.forEach { (id, name) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$name ($id)")
                            Spacer(modifier = Modifier.width(8.dp))
                            when {
                                id == TranslationStore.BUNDLED_ID ->
                                    PickerBadge(text = "Bundled", content = pal.green, pal = pal)
                                id in cachedIds ->
                                    PickerBadge(text = "Cached", content = pal.gold, pal = pal)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onPick(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun PickerBadge(text: String, content: Color, pal: NurPalette) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(content.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
