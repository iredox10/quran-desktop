package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.SaukaGroup
import com.nur.quran.desktop.data.SaukaStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.DivisionStart
import com.nur.quran.shared.HIZB_STARTS
import com.nur.quran.shared.JUZ_STARTS

/**
 * LOCAL-only group-khatmah index (desktop Sauka).
 * Mirrors the web `SaukaIndex` — create form + group cards — with no backend.
 */
@Composable
fun SaukaIndexDesktop(
    pal: NurPalette,
    onOpenGroup: (String) -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var refresh by remember { mutableStateOf(0) }
    val groups = remember(refresh) { SaukaStore.listGroups() }

    var title by remember { mutableStateOf("") }
    var intention by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("juz") }
    var deadline by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(pal.white),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = pal.teal,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sauka",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi
                    )
                    Text(
                        text = "Group khatmah · stored on this device only",
                        fontSize = 13.sp,
                        color = pal.inkMuted,
                        fontFamily = fontBody
                    )
                }
            }
        }

        item(key = "create") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.cream)
                    .border(1.dp, pal.boneDark, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "New group khatmah",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Group title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = intention,
                    onValueChange = { intention = it },
                    label = { Text("Intention (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("juz" to "Juz (30)", "hizb" to "Hizb (60)").forEach { (value, label) ->
                        val selected = division == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (selected) pal.teal else Color.Transparent)
                                .border(1.dp, if (selected) pal.teal else pal.boneDark, RoundedCornerShape(100.dp))
                                .clickable { division = value }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else pal.inkMid,
                                fontFamily = fontUi
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (e.g. Ramadan, yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        SaukaStore.createGroup(title, intention, division, deadline)
                        title = ""
                        intention = ""
                        deadline = ""
                        refresh++
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pal.teal,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Create group", fontWeight = FontWeight.Bold, fontFamily = fontUi)
                }
            }
        }

        if (groups.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "No groups yet — create one above and invite readers to claim a juz.",
                    fontSize = 14.sp,
                    color = pal.inkMid,
                    fontFamily = fontBody
                )
            }
        } else {
            items(groups, key = { "sauka-${it.id}" }) { group ->
                SaukaGroupCard(
                    pal = pal,
                    group = group,
                    onOpen = { onOpenGroup(group.id) },
                    onDelete = {
                        SaukaStore.deleteGroup(group.id)
                        refresh++
                    }
                )
            }
        }
    }
}

@Composable
private fun SaukaGroupCard(
    pal: NurPalette,
    group: SaukaGroup,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val total = SaukaStore.totalUnits(group.division)
    val claimed = (group.claims.keys + group.doneUnits).distinct().size
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = pal.cream,
        border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(if (group.division == "hizb") "Hizb division" else "Juz division")
                            if (group.deadline.isNotBlank()) append(" · ${group.deadline}")
                            if (group.intention.isNotBlank()) append(" · ${group.intention}")
                        },
                        fontSize = 12.sp,
                        color = pal.inkMuted,
                        fontFamily = fontBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete group",
                        tint = pal.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else claimed.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(100.dp)),
                color = pal.teal,
                trackColor = pal.bone
            )
            Text(
                text = "claimed $claimed/$total",
                fontSize = 12.sp,
                color = pal.inkMid,
                fontFamily = fontBody
            )
        }
    }
}

/**
 * LOCAL-only group-khatmah detail (desktop Sauka group).
 *
 * Tap cycles a unit: free -> claim as "You" -> done -> free.
 * Every unit also offers an "open" link to its start page/surah.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaukaGroupDesktop(
    pal: NurPalette,
    groupId: String,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> },
    onOpenPage: (Int) -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var refresh by remember { mutableStateOf(0) }
    var newMember by remember { mutableStateOf("") }
    val group = remember(groupId, refresh) { SaukaStore.getGroup(groupId) }

    Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = pal.ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group?.title ?: "Sauka group",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (group != null) {
                    val total = SaukaStore.totalUnits(group.division)
                    val done = group.doneUnits.size
                    val claimed = (group.claims.keys + group.doneUnits).distinct().size
                    Text(
                        text = "$claimed/$total claimed · $done done" +
                            (group.deadline.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        fontSize = 13.sp,
                        color = pal.inkMid,
                        fontFamily = fontBody
                    )
                }
            }
        }

        if (group == null) {
            Text(
                text = "Group not found — it may have been deleted.",
                fontSize = 14.sp,
                color = pal.inkMid,
                fontFamily = fontBody,
                modifier = Modifier.padding(20.dp)
            )
            return@Column
        }

        val total = SaukaStore.totalUnits(group.division)
        val starts: List<DivisionStart> =
            if (group.division == "hizb") HIZB_STARTS else JUZ_STARTS
        val startById = remember(starts) { starts.associateBy { it.id.toString() } }

        LinearProgressIndicator(
            progress = {
                val claimed = (group.claims.keys + group.doneUnits).distinct().size
                if (total == 0) 0f else claimed.toFloat() / total
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(8.dp)
                .clip(RoundedCornerShape(100.dp)),
            color = pal.teal,
            trackColor = pal.bone
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "grid") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SaukaStore.unitsFor(group.division).forEach { unitId ->
                        val done = group.doneUnits.contains(unitId)
                        val claim = group.claims[unitId]
                        SaukaUnitCell(
                            pal = pal,
                            unitId = unitId,
                            done = done,
                            claim = claim,
                            onCycle = {
                                when {
                                    done -> SaukaStore.unclaimUnit(group.id, unitId)
                                    claim != null && claim == "You" -> SaukaStore.toggleDone(group.id, unitId)
                                    claim == null -> SaukaStore.claimUnit(group.id, unitId, "You")
                                    else -> SaukaStore.claimUnit(group.id, unitId, "You")
                                }
                                refresh++
                            },
                            onOpen = {
                                val start = startById[unitId] ?: return@SaukaUnitCell
                                onOpenPage(start.pageNumber)
                            },
                            onOpenSurah = {
                                val start = startById[unitId] ?: return@SaukaUnitCell
                                val surahId = start.verseKey.substringBefore(":").toIntOrNull()
                                    ?: return@SaukaUnitCell
                                onOpenSurah(surahId, start.verseKey)
                            }
                        )
                    }
                }
            }

            item(key = "members") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Members · ${group.members.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newMember,
                            onValueChange = { newMember = it },
                            label = { Text("Add member") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                SaukaStore.addMember(group.id, newMember)
                                newMember = ""
                                refresh++
                            },
                            enabled = newMember.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pal.teal,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Add", fontFamily = fontUi)
                        }
                    }
                    if (group.members.isEmpty()) {
                        Text(
                            text = "No members yet — tapping a free unit claims it as \"You\".",
                            fontSize = 13.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            group.members.forEach { member ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(pal.tealSoft)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = member,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.teal,
                                        fontFamily = fontBody
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaukaUnitCell(
    pal: NurPalette,
    unitId: String,
    done: Boolean,
    claim: String?,
    onCycle: () -> Unit,
    onOpen: () -> Unit,
    onOpenSurah: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val (bg, fg) = when {
        done -> pal.gold to Color.White
        claim != null -> pal.teal to Color.White
        else -> pal.bone to pal.ink
    }
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (done) pal.gold else pal.boneDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onCycle)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = when {
                done -> "✓ $unitId"
                claim != null -> "${claim.firstOrNull()?.uppercase() ?: "?"} · $unitId"
                else -> unitId
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            fontFamily = fontUi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (claim != null && !done) {
            Text(
                text = claim,
                fontSize = 10.sp,
                color = fg.copy(alpha = 0.85f),
                fontFamily = fontUi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "open",
                fontSize = 10.sp,
                color = if (done || claim != null) fg.copy(alpha = 0.9f) else pal.teal,
                fontFamily = fontUi,
                modifier = Modifier.clickable(onClick = onOpen).padding(horizontal = 2.dp)
            )
            Text(
                text = "·",
                fontSize = 10.sp,
                color = if (done || claim != null) fg.copy(alpha = 0.7f) else pal.inkMuted
            )
            Text(
                text = "surah",
                fontSize = 10.sp,
                color = if (done || claim != null) fg.copy(alpha = 0.9f) else pal.teal,
                fontFamily = fontUi,
                modifier = Modifier.clickable(onClick = onOpenSurah).padding(horizontal = 2.dp)
            )
        }
    }
}
