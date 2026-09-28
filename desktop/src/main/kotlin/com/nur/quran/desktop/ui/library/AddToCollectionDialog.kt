package com.nur.quran.desktop.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.data.CollectionStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * "Add to collection" dialog for one verse: tap a collection row to toggle
 * membership, create a new collection inline. Mirrors the mobile sheet.
 */
@Composable
fun AddToCollectionDialog(
    pal: NurPalette,
    verseKey: String,
    onDismiss: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    var tick by remember { mutableStateOf(0) }
    var newName by remember { mutableStateOf("") }
    val collections = remember(tick) { CollectionStore.listCollections() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = pal.cream,
            border = BorderStroke(1.5.dp, pal.boneDark),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add to collection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = verseKey,
                    fontSize = 11.sp,
                    color = pal.gold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (collections.isEmpty()) {
                    Text(
                        text = "No collections yet — create one below.",
                        fontSize = 13.sp,
                        color = pal.inkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(collections.size) { i ->
                            val collection = collections[i]
                            val member = CollectionStore.items(collection.id).contains(verseKey)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (member) pal.tealSoft else Color.Transparent)
                                    .clickable {
                                        if (member) {
                                            CollectionStore.removeItem(collection.id, verseKey)
                                        } else {
                                            CollectionStore.addItem(collection.id, verseKey)
                                        }
                                        tick++
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = collection.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (member) pal.teal else pal.ink,
                                        fontFamily = fontUi,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${CollectionStore.items(collection.id).size} verses",
                                        fontSize = 10.sp,
                                        color = pal.inkMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (member) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "In collection",
                                        tint = pal.teal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Inline create
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = {
                            Text("New collection name…", color = pal.inkMuted, fontSize = 13.sp)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.teal,
                            unfocusedBorderColor = pal.boneDark,
                            focusedContainerColor = pal.cream,
                            unfocusedContainerColor = pal.cream,
                            cursorColor = pal.teal,
                            focusedTextColor = pal.ink,
                            unfocusedTextColor = pal.ink
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val canCreate = newName.trim().length >= 2
                    Surface(
                        onClick = {
                            if (canCreate) {
                                val created = CollectionStore.createCollection(newName.trim())
                                CollectionStore.addItem(created.id, verseKey)
                                newName = ""
                                tick++
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (canCreate) pal.teal else pal.bone,
                        enabled = canCreate
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Create",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = fontUi
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.inkMid,
                        fontFamily = fontUi,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
