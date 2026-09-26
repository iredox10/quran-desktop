package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.planner.JournalEditor
import com.nur.quran.desktop.ui.planner.PaceRing
import com.nur.quran.desktop.ui.planner.PlanTodayCard
import com.nur.quran.desktop.ui.planner.SectionTitle
import com.nur.quran.desktop.ui.planner.TemplatesGrid
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLAN_TEMPLATES
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlanTemplate
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan

private val OverdueRed = Color(0xFFDC2626)

/** Desktop `ChapterJson` -> shared [PlanChapter] mapping. */
private fun planChapters(): List<PlanChapter> =
    QuranStore.chapters.map { c ->
        PlanChapter(
            id = c.id,
            versesCount = c.versesCount,
            startPage = c.pages.firstOrNull() ?: 0,
            endPage = c.pages.lastOrNull() ?: 0
        )
    }

/**
 * Desktop planner mirroring Android `PlannerScreen`: dashboard header, tab row
 * (Today / Progress / Journal), templates library with one-tap start, and a
 * custom surah-range plan form.
 */
@Composable
fun PlannerScreenDesktop(
    pal: NurPalette,
    onBack: () -> Unit = {},
    onOpenSurah: (Int, String?) -> Unit = { _, _ -> },
    onOpenPage: (Int) -> Unit = {},
    onOpenDay: (String) -> Unit = {}
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()

    var tab by remember { mutableStateOf(0) }
    var refresh by remember { mutableStateOf(0) }
    var journalDate by remember { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var customError by remember { mutableStateOf<String?>(null) }

    val todayStr = remember { PlannerEngine.formatPlannerDate() }
    val activePlan = remember(refresh) { PlannerStore.getActivePlan() }

    fun adoptPlan(plan: ReadingPlan) {
        val updated = PlannerStore.getAllPlans().filterNot { it.id == plan.id } + plan
        PlannerStore.saveAllPlans(updated)
        PlannerStore.saveActivePlan(plan)
        customError = null
        refresh++
    }

    fun startFromTemplate(template: PlanTemplate) {
        try {
            val built = PlannerEngine.buildReadingPlanner(
                unitType = template.unitType,
                durationDays = 30,
                startDate = todayStr,
                startUnit = template.startUnit,
                endUnit = template.endUnit,
                customTitle = template.title,
                excludeDays = emptyList(),
                chapters = planChapters()
            )
            adoptPlan(built)
        } catch (e: Exception) {
            customError = e.message ?: "Could not start plan"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(pal.white),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Dashboard header ──
        item(key = "header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = pal.ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quran Nur",
                            fontSize = 13.sp,
                            color = pal.inkMuted,
                            fontFamily = fontBody
                        )
                        Text(
                            text = activePlan?.title ?: "Planner",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            fontFamily = fontUi
                        )
                    }
                }
                if (activePlan != null) {
                    val overview = remember(activePlan) {
                        PlannerEngine.getPlannerOverview(activePlan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (overview != null) {
                            "Day ${overview.currentDayNumber} of ${activePlan.durationDays} · " +
                                "${overview.completedCount} days complete"
                        } else {
                            "Starts ${activePlan.startDate}"
                        },
                        fontSize = 13.sp,
                        color = pal.inkMid,
                        fontFamily = fontBody
                    )
                }
            }
        }

        // ── Simple tab row: Today / Progress / Journal ──
        item(key = "tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(pal.cream)
                    .border(1.dp, pal.boneDark, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Today", "Progress", "Journal").forEachIndexed { index, label ->
                    val selected = tab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) pal.teal else Color.Transparent)
                            .clickable { tab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else pal.inkMid,
                            fontFamily = fontUi
                        )
                    }
                }
            }
        }

        // ── Tab contents ──
        when (tab) {
            0 -> {
                item(key = "today") {
                    if (activePlan == null) {
                        Text(
                            text = "No active plan yet — pick a template below or build a custom plan.",
                            fontSize = 14.sp,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                    } else {
                        val plan = activePlan
                        val overview = remember(plan, refresh) {
                            PlannerEngine.getPlannerOverview(plan)
                        }
                        val todayAssignment = remember(plan, todayStr) {
                            plan.assignments.find { it.date == todayStr }
                                ?: plan.assignments.getOrNull(
                                    (overview?.currentDayNumber ?: 1) - 1
                                )
                                ?: plan.assignments.firstOrNull()
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PaceRing(
                                pal = pal,
                                progress = overview?.completionRatio ?: 0f,
                                label = "${overview?.completedCount ?: 0} of ${plan.durationDays} days"
                            )
                            PlanTodayCard(
                                pal = pal,
                                title = todayAssignment?.title ?: plan.title,
                                detail = listOfNotNull(
                                    todayAssignment?.subtitle,
                                    todayAssignment?.date
                                ).joinToString(" · "),
                                ctaLabel = "Open day",
                                onOpen = {
                                    onOpenDay(todayAssignment?.date ?: plan.startDate)
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (activePlan == null) {
                    item(key = "progress-empty") {
                        Text(
                            text = "Start a plan to track per-day progress here.",
                            fontSize = 14.sp,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                    }
                } else {
                    val plan = activePlan
                    if (plan != null) {
                        item(key = "weekly") {
                            val summaries = remember(plan, refresh) {
                                PlannerEngine.getWeeklySummary(plan)
                            }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(pal = pal, text = "Weekly summary")
                            summaries.forEach { week ->
                                val pct = if (week.totalUnits > 0) {
                                    (week.completedUnits * 100) / week.totalUnits
                                } else 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = week.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink,
                                        fontFamily = fontBody
                                    )
                                    Text(
                                        text = "${week.completedUnits}/${week.totalUnits} pages · $pct%",
                                        fontSize = 13.sp,
                                        color = pal.inkMuted,
                                        fontFamily = fontBody
                                    )
                                }
                            }
                        }
                    }
                    items(plan.assignments, key = { "assign-${it.dayNumber}" }) { assignment ->
                        val status = PlannerEngine.getAssignmentStatus(plan, assignment)
                        Surface(
                            onClick = {
                                if (assignment.unitType == "surah") {
                                    val surahId = assignment.items.firstOrNull()
                                        ?.rangeValue?.toIntOrNull()
                                    if (surahId != null) onOpenSurah(surahId, null)
                                    else onOpenPage(assignment.pageStart)
                                } else {
                                    onOpenPage(assignment.pageStart)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = pal.cream,
                            border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Day ${assignment.dayNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.teal,
                                    fontFamily = fontBody,
                                    modifier = Modifier.width(56.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = assignment.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink,
                                        fontFamily = fontBody
                                    )
                                    Text(
                                        text = assignment.date,
                                        fontSize = 12.sp,
                                        color = pal.inkMuted,
                                        fontFamily = fontBody
                                    )
                                }
                                StatusPill(pal = pal, status = status)
                            }
                        }
                    }
                    }
                }
            }
            2 -> {
                item(key = "journal") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = journalDate,
                            onValueChange = { journalDate = it },
                            label = { Text("Date (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        key(journalDate) {
                            JournalEditor(
                                pal = pal,
                                dateStr = journalDate,
                                initial = PlannerStore.getJournal(journalDate),
                                onSave = { text ->
                                    PlannerStore.saveJournal(journalDate, text)
                                    refresh++
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Templates section ──
        item(key = "templates-title") {
            SectionTitle(pal = pal, text = "Plan templates")
        }
        item(key = "templates-grid") {
            TemplatesGrid(
                pal = pal,
                templates = PLAN_TEMPLATES,
                onPick = ::startFromTemplate
            )
        }

        // ── Custom plan form ──
        item(key = "custom") {
            CustomPlanForm(
                pal = pal,
                fontUi = rememberUiFontFamily(),
                error = customError,
                onCreate = { selected, startDate, endDate, title ->
                    try {
                        val duration = (
                            PlannerEngine.diffDays(startDate, endDate) + 1
                            ).coerceAtLeast(1)
                        val built = PlannerEngine.buildReadingPlanner(
                            unitType = "surah",
                            durationDays = duration,
                            startDate = startDate,
                            startUnit = selected.minOrNull() ?: 1,
                            endUnit = selected.maxOrNull() ?: 114,
                            customTitle = title.ifBlank { null },
                            excludeDays = emptyList(),
                            chapters = planChapters()
                        )
                        adoptPlan(built)
                    } catch (e: Exception) {
                        customError = e.message ?: "Could not create plan"
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusPill(pal: NurPalette, status: String) {
    val (bg, fg) = when (status) {
        "completed" -> pal.tealSoft to pal.teal
        "today" -> pal.goldSoft to pal.gold
        "partial" -> pal.tealSoft to pal.teal
        "overdue" -> Color(0xFFFDECEC) to OverdueRed
        else -> pal.bone to pal.inkMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun CustomPlanForm(
    pal: NurPalette,
    fontUi: androidx.compose.ui.text.font.FontFamily,
    error: String?,
    onCreate: (selected: List<Int>, startDate: String, endDate: String, title: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var startDate by remember { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var endDate by remember {
        mutableStateOf(PlannerEngine.addDays(PlannerEngine.formatPlannerDate(), 29))
    }
    var dailyMinutes by remember { mutableStateOf("20") }
    var title by remember { mutableStateOf("") }

    val chapters = remember { QuranStore.chapters }
    val filtered = remember(query, chapters) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) chapters
        else chapters.filter {
            it.nameSimple.lowercase().contains(q) || it.id.toString() == q
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pal.cream)
            .border(1.dp, pal.boneDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(pal = pal, text = "Custom plan")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Plan title (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it.trim() },
                label = { Text("Start yyyy-MM-dd") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it.trim() },
                label = { Text("End yyyy-MM-dd") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = dailyMinutes,
            onValueChange = { dailyMinutes = it.filter(Char::isDigit).take(3) },
            label = { Text("Daily minutes target") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search surahs…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${selected.size} surahs selected" +
                (dailyMinutes.toIntOrNull()?.let { " · $it min/day" } ?: ""),
            fontSize = 12.sp,
            color = pal.inkMuted,
            fontFamily = fontUi
        )
        // Surah multi-pick (checkbox list, capped height via take window).
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            filtered.take(114).forEach { chapter ->
                val checked = selected.contains(chapter.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selected = if (checked) selected - chapter.id
                            else selected + chapter.id
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            selected = if (it) selected + chapter.id
                            else selected - chapter.id
                        }
                    )
                    Text(
                        text = "${chapter.id}. ${chapter.nameSimple}",
                        fontSize = 14.sp,
                        color = pal.ink,
                        fontFamily = fontUi,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${chapter.versesCount} ayahs",
                        fontSize = 12.sp,
                        color = pal.inkMuted
                    )
                }
            }
        }
        if (error != null) {
            Text(text = error, fontSize = 13.sp, color = OverdueRed)
        }
        Button(
            onClick = {
                onCreate(selected.sorted(), startDate, endDate, title.trim())
            },
            enabled = selected.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = pal.teal,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Create custom plan", fontWeight = FontWeight.Bold, fontFamily = fontUi)
        }
    }
}
