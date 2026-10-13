package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.Notifier
import com.nur.quran.desktop.data.PrayerPrefs
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.planner.ArchivesModal
import com.nur.quran.desktop.ui.planner.CustomPlanModal
import com.nur.quran.desktop.ui.planner.CustomPlanParams
import com.nur.quran.desktop.ui.planner.IntentionView
import com.nur.quran.desktop.ui.planner.JournalEditor
import com.nur.quran.desktop.ui.planner.JournalTab
import com.nur.quran.desktop.ui.planner.PaceRing
import com.nur.quran.desktop.ui.planner.PlanTodayCard
import com.nur.quran.desktop.ui.planner.SectionTitle
import com.nur.quran.desktop.ui.planner.TemplatePreviewModal
import com.nur.quran.desktop.ui.planner.TodayTab
import com.nur.quran.desktop.data.PrayerStore
import com.nur.quran.desktop.ui.planner.TemplatesGrid
import com.nur.quran.shared.PLANNER_UNITS
import com.nur.quran.shared.buildPrayerSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLAN_TEMPLATES
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlanTemplate
import com.nur.quran.shared.PRAYER_NAMES
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import java.io.File
import javax.swing.JFileChooser

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
    var showCustomModal by remember { mutableStateOf(false) }
    var showArchivesModal by remember { mutableStateOf(false) }
    var previewPlan by remember { mutableStateOf<ReadingPlan?>(null) }
    var showPlannerSettings by remember { mutableStateOf(false) }

    // ── Prayer slots state (Aladhan, cache-first via PrayerStore) ──
    var prayerCity by remember { mutableStateOf(PrayerStore.getCity()) }
    var prayerCountry by remember { mutableStateOf(PrayerStore.getCountry()) }
    var prayerTimings by remember { mutableStateOf<com.nur.quran.shared.PrayerTimings?>(null) }
    var prayerLoading by remember { mutableStateOf(false) }
    val prayerScope = rememberCoroutineScope()

    fun fetchPrayerTimings(city: String = prayerCity, country: String = prayerCountry) {
        prayerLoading = true
        prayerScope.launch {
            val timings = withContext(Dispatchers.IO) {
                PrayerStore.putCity(city)
                PrayerStore.putCountry(country)
                PrayerStore.getToday()
            }
            prayerTimings = timings
            prayerLoading = false
        }
    }

    LaunchedEffect(Unit) { fetchPrayerTimings() }

    val todayStr = remember { PlannerEngine.formatPlannerDate() }
    val activePlan = remember(refresh) { PlannerStore.getActivePlan() }
    val allPlans = remember(refresh) { PlannerStore.getAllPlans() }

    fun adoptPlan(plan: ReadingPlan) {
        val updated = PlannerStore.getAllPlans().filterNot { it.id == plan.id } + plan
        PlannerStore.saveAllPlans(updated)
        PlannerStore.saveActivePlan(plan)
        customError = null
        refresh++
    }

    fun activatePlan(plan: ReadingPlan) {
        PlannerStore.saveActivePlan(plan)
        refresh++
    }

    // Web parity (useAppStore deletePlanner): deleting the active plan falls
    // back to the first remaining plan; deleting the last plan clears active.
    fun deletePlan(plan: ReadingPlan) {
        val remaining = PlannerStore.getAllPlans().filterNot { it.id == plan.id }
        PlannerStore.saveAllPlans(remaining)
        if (plan.id == activePlan?.id) {
            PlannerStore.saveActivePlan(remaining.firstOrNull())
        }
        refresh++
    }

    /** Prayer-slot mark/undo: writes a progress count for the assignment date. */
    fun setSlotProgress(plan: ReadingPlan, dayDate: String, count: Int) {
        val assignment = plan.assignments.firstOrNull { it.date == dayDate } ?: return
        val total = PlannerEngine.getAssignmentProgress(plan, assignment).totalCount
            .coerceAtLeast(1)
        val clamped = count.coerceIn(0, total)
        val progress = plan.assignmentProgress.toMutableMap()
        progress[assignment.dayNumber] = clamped
        var completedDays = plan.completedDays.toMutableList()
        var completedAt = plan.assignmentCompletedAt.toMutableMap()
        if (clamped >= total) {
            if (!completedDays.contains(assignment.dayNumber)) {
                completedDays.add(assignment.dayNumber)
            }
            completedAt.putIfAbsent(
                assignment.dayNumber,
                PlannerEngine.formatPlannerDate()
            )
        } else {
            completedDays.remove(assignment.dayNumber)
            completedAt.remove(assignment.dayNumber)
        }
        PlannerStore.saveActivePlan(
            plan.copy(
                assignmentProgress = progress,
                completedDays = completedDays,
                assignmentCompletedAt = completedAt
            )
        )
        refresh++
    }

    fun startFromTemplate(template: PlanTemplate) {
        // Web parity (quran-app Planner.jsx handleTemplateSelect): build the
        // full plan at tap time so the preview modal shows real assignments,
        // and surface build failures immediately instead of at confirm.
        if (template.unitType == "surah" && planChapters().isEmpty()) {
            customError = "Surah data is still loading — please try again in a moment."
            return
        }
        try {
            val built = PlannerEngine.buildReadingPlanner(
                unitType = template.unitType,
                durationDays = template.durationDays,
                startDate = todayStr,
                startUnit = template.startUnit,
                endUnit = template.endUnit,
                customTitle = template.title,
                excludeDays = emptyList(),
                chapters = planChapters()
            )
            customError = null
            previewPlan = built
        } catch (e: Exception) {
            previewPlan = null
            customError = "Could not build template plan: ${e.message}"
        }
    }

    fun confirmTemplate(plan: ReadingPlan) {
        previewPlan = null
        adoptPlan(plan)
    }

    fun createCustomPlan(params: CustomPlanParams) {
        try {
            val totalUnits = (params.endUnit - params.startUnit + 1).coerceAtLeast(1)
            val duration = (totalUnits + params.perDay - 1) / params.perDay.coerceAtLeast(1)
            val built = PlannerEngine.buildReadingPlanner(
                unitType = params.unitType,
                durationDays = duration.coerceAtLeast(1),
                startDate = params.startDate,
                startUnit = params.startUnit,
                endUnit = params.endUnit,
                customTitle = params.title.ifBlank { null },
                excludeDays = params.excludeDays,
                chapters = planChapters()
            )
            showCustomModal = false
            adoptPlan(built)
        } catch (e: Exception) {
            customError = e.message ?: "Could not create plan"
        }
    }

    // ── No active plan → intention/onboarding view ──
    if (activePlan == null) {
        Column(modifier = Modifier.fillMaxSize().background(pal.white)) {
            IntentionView(
                pal = pal,
                hasActivePlan = false,
                plans = allPlans,
                activePlanId = null,
                chapters = planChapters(),
                onBegin = { adoptPlan(it) },
                onViewActive = {},
                onSwitchPlan = { id ->
                    allPlans.firstOrNull { it.id == id }?.let { activatePlan(it) }
                },
                onDeletePlan = { id ->
                    allPlans.firstOrNull { it.id == id }?.let { deletePlan(it) }
                },
                onNewCustom = { showCustomModal = true },
                onOpenArchives = { showArchivesModal = true },
                archivedCount = PlannerStore.getArchived().size,
                templates = PLAN_TEMPLATES,
                onPickTemplate = ::startFromTemplate,
                chaptersReady = planChapters().isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
        previewPlan?.let { built ->
            TemplatePreviewModal(
                pal = pal,
                plan = built,
                onDismiss = { previewPlan = null },
                onConfirm = { confirmTemplate(built) }
            )
        }
        if (showCustomModal) {
            CustomPlanModal(
                pal = pal,
                chapters = planChapters(),
                onDismiss = { showCustomModal = false },
                onCreate = { createCustomPlan(it) }
            )
        }
        if (showArchivesModal) {
            ArchivesModal(
                pal = pal,
                archived = PlannerStore.getArchived(),
                onDismiss = { showArchivesModal = false },
                onRestore = {
                    PlannerStore.restoreArchived(it.plan.id)
                    showArchivesModal = false
                    refresh++
                }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(pal.white),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Dashboard header ──
        item(key = "header") {
            val plan = activePlan ?: return@item
            val overview = remember(plan) { PlannerEngine.getPlannerOverview(plan) } ?: return@item
            val metrics = remember(plan) {
                runCatching { PlannerEngine.getPlannerSuccessMetrics(plan) }.getOrNull()
            }
            val todayAssignment = remember(plan) {
                plan.assignments.firstOrNull { it.date == todayStr }
                    ?: plan.assignments.getOrNull(overview.currentDayNumber - 1)
            }
            val todayProgress = todayAssignment?.let { PlannerEngine.getAssignmentProgress(plan, it) }
            val todayComplete = todayProgress?.isComplete ?: false
            val nextAssignment = if (todayComplete) {
                plan.assignments.firstOrNull { a ->
                    (todayAssignment == null || a.dayNumber > todayAssignment.dayNumber) &&
                        !PlannerEngine.getAssignmentProgress(plan, a).isComplete
                }
            } else null
            val planDone = overview.isFinishedWindow && nextAssignment == null
            val currentDay = overview.currentDayNumber
            val overallPct = (overview.completionRatio * 100).toInt()
            val completionDate = try {
                PlannerEngine.formatPlannerDateLabel(
                    PlannerEngine.addDays(plan.startDate, plan.durationDays - 1)
                )
            } catch (_: Exception) {
                plan.startDate
            }
            val streak = metrics?.consistencyStreak ?: 0
            val unitsLabel = PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
            val todayDone = todayProgress?.readPagesCount ?: 0
            val todayTotal = (todayProgress?.totalPagesCount ?: 1).coerceAtLeast(1)
            val hasStartedReading = todayDone > 0
            val daySubtitle = when {
                planDone -> "Plan complete! \uD83C\uDF89"
                todayAssignment == null -> "Starting soon\u2026"
                todayComplete && nextAssignment != null ->
                    "Day $currentDay complete! \uD83C\uDF89 \u00B7 Day ${nextAssignment.dayNumber} ready"
                todayComplete -> "Day $currentDay complete! \uD83C\uDF89"
                todayDone > 0 -> "$todayDone of $todayTotal $unitsLabel read"
                else -> "$todayTotal $unitsLabel today"
            }
            val ctaLabel = when {
                todayComplete && nextAssignment != null -> "Continue to Day ${nextAssignment.dayNumber}"
                todayComplete -> "All Caught Up"
                hasStartedReading -> "Resume Reading"
                else -> "Open Al-Quran"
            }
            val ctaDay = (nextAssignment ?: overview.firstIncomplete).date
            var switcherOpen by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(pal.white)
                            .border(1.dp, pal.boneDark, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = pal.teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (allPlans.size > 1) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { switcherOpen = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = plan.title.ifBlank { "Quran Plan" },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    )
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = "Switch plan",
                                        tint = pal.inkMid,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = switcherOpen,
                                    onDismissRequest = { switcherOpen = false }
                                ) {
                                    allPlans.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = p.title.ifBlank { "${p.durationDays} Day Plan" },
                                                    fontFamily = fontBody,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                switcherOpen = false
                                                activatePlan(p)
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = plan.title.ifBlank { "Quran Plan" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink,
                                fontFamily = fontUi
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Day $currentDay of ${plan.durationDays}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = pal.teal,
                            fontFamily = fontUi
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = daySubtitle,
                            fontSize = 13.sp,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (planDone) {
                                val rev = runCatching {
                                    PlannerEngine.buildRevisionPlanner(plan, planChapters())
                                }.getOrNull()
                                if (rev != null) adoptPlan(rev)
                            } else {
                                onOpenDay(ctaDay)
                            }
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (planDone) "Start Revision Plan" else ctaLabel,
                            fontFamily = fontUi,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = pal.white
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Stats header cards \u2014 web parity (Overall / Finish by / Streak)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "Overall" to "$overallPct%",
                        "Finish by" to completionDate,
                        "Streak" to "${streak} Day${if (streak != 1) "s" else ""}"
                    ).forEach { (label, value) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = pal.cream,
                            border = BorderStroke(1.dp, pal.boneDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = label.uppercase(),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp,
                                    color = pal.inkMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = value,
                                    fontFamily = fontUi,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pal.ink
                                )
                            }
                        }
                    }
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) pal.white else Color.Transparent)
                            .then(
                                if (selected) Modifier.border(1.dp, pal.boneDark, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { tab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) pal.teal else pal.inkMuted,
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
                    val plan = activePlan
                    if (plan == null) {
                        Text(
                            text = "No active plan yet — pick a template below or build a custom plan.",
                            fontSize = 14.sp,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                    } else {
                        TodayTab(
                            pal = pal,
                            plan = plan,
                            todayStr = todayStr,
                            onOpenDay = { date -> onOpenDay(date) },
                            onOpenSettings = { showPlannerSettings = true },
                            onSlotDone = { dayDate, upTo -> setSlotProgress(plan, dayDate, upTo) },
                            onSlotUndo = { dayDate, from -> setSlotProgress(plan, dayDate, from) }
                        )
                        // ── Reminders ──
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(pal = pal, text = "Reminders")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "System notifications: " +
                                        if (java.awt.SystemTray.isSupported()) "on" else "off",
                                    fontSize = 12.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                                TextButton(
                                    onClick = {
                                        Notifier.notify(
                                            "Quran Nur",
                                            "Reminders work — today's reading is ready."
                                        )
                                    }
                                ) {
                                    Text("Test notification", color = pal.teal, fontSize = 12.sp)
                                }
                            }
                        }
                        // ── Focus timer ──
                        com.nur.quran.desktop.ui.pomodoro.PomodoroWidget(pal = pal)
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
                        item(key = "progress-tab") {
                            com.nur.quran.desktop.ui.planner.ProgressTab(
                                pal = pal,
                                plan = plan,
                                todayStr = todayStr,
                                chapters = planChapters(),
                                onAdoptPlan = { adoptPlan(it) }
                            )
                        }
                    }
                }
            }
            2 -> {
                val jp = activePlan
                if (jp != null) {
                    item(key = "journal") {
                        JournalTab(
                            pal = pal,
                            plan = jp,
                            journalDate = journalDate,
                            onJournalDateChange = { journalDate = it; refresh++ },
                            onOpenSurah = onOpenSurah,
                            onSaved = { refresh++ }
                        )
                    }
                } else {
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
        }

        // ── My plans (management) ──
        if (allPlans.isNotEmpty()) {
            item(key = "my-plans-title") {
                SectionTitle(pal = pal, text = "My plans • ${allPlans.size}")
            }
            items(allPlans, key = { "plan-${it.id}" }) { plan ->
                val isActive = plan.id == activePlan?.id
                val planAnalytics = remember(plan.id) {
                    runCatching { PlannerEngine.getPlannerAnalytics(plan) }.getOrNull()
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) pal.tealSoft else pal.cream,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isActive) pal.teal else pal.boneDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plan.title + if (isActive) "  • active" else "",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isActive) pal.teal else pal.ink,
                                    fontFamily = fontUi
                                )
                                Text(
                                    text = "${plan.startDate} · ${plan.durationDays} days",
                                    fontSize = 11.sp,
                                    color = pal.inkMuted,
                                    fontFamily = fontBody
                                )
                                planAnalytics?.let { a ->
                                    Text(
                                        text = "${a.onTimeRate}% on time · ${a.totalReadPages} pages read",
                                        fontSize = 11.sp,
                                        color = pal.inkMuted,
                                        fontFamily = fontBody
                                    )
                                }
                            }
                            if (!isActive) {
                                TextButton(onClick = { activatePlan(plan) }) {
                                    Text("Activate", color = pal.teal, fontSize = 12.sp)
                                }
                                TextButton(onClick = { deletePlan(plan) }) {
                                    Text("Delete", color = pal.gold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Plan creation lives on the Intention (no-plan) screen; the active
        // view only manages the current plan via its tabs and settings ──
        if (customError != null) {
            item(key = "custom-error") {
                Text(
                    text = customError ?: "",
                    fontSize = 12.sp,
                    color = OverdueRed,
                    fontFamily = fontBody,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    if (showCustomModal) {
        CustomPlanModal(
            pal = pal,
            chapters = planChapters(),
            onDismiss = { showCustomModal = false },
            onCreate = { createCustomPlan(it) }
        )
    }
    previewPlan?.let { built ->
        TemplatePreviewModal(
            pal = pal,
            plan = built,
            onDismiss = { previewPlan = null },
            onConfirm = { confirmTemplate(built) }
        )
    }
    if (showPlannerSettings) {
        PlannerSettingsDialog(
            pal = pal,
            city = prayerCity,
            country = prayerCountry,
            loading = prayerLoading,
            onCityChange = { prayerCity = it },
            onCountryChange = { prayerCountry = it },
            onRefresh = { fetchPrayerTimings(prayerCity.trim(), prayerCountry.trim()) },
            onDismiss = { showPlannerSettings = false }
        )
    }
}

@Composable
private fun AnalyticsStat(pal: NurPalette, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = pal.teal,
            fontFamily = fontUiLabel()
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = pal.inkMuted,
            fontFamily = fontBodyLabel()
        )
    }
}

@Composable
private fun fontUiLabel(): androidx.compose.ui.text.font.FontFamily = rememberUiFontFamily()

@Composable
private fun fontBodyLabel(): androidx.compose.ui.text.font.FontFamily = rememberBodyFontFamily()

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

/** Planner Settings dialog (web `Planner.jsx` modal parity): prayers, reading preference, intention, location, export. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlannerSettingsDialog(
    pal: NurPalette,
    city: String,
    country: String,
    loading: Boolean,
    onCityChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val savedPrefs = remember { PlannerStore.getPrayerPrefs() }
    var activePrayers by remember { mutableStateOf(savedPrefs.activePrayers) }
    var readPref by remember { mutableStateOf(savedPrefs.readPreference) }
    var intentionOn by remember { mutableStateOf(savedPrefs.intentionPromptEnabled) }
    var exportStatus by remember { mutableStateOf("") }

    fun persist() {
        PlannerStore.savePrayerPrefs(PrayerPrefs(activePrayers, readPref, intentionOn))
    }

    @Composable
    fun SectionHeader(label: String) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = pal.teal,
            fontFamily = fontUi
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    fun esc(text: String): String = text
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")

    fun exportPlanIcs(): String {
        val plan = PlannerStore.getActivePlan() ?: return "No active plan"
        return try {
            val stampFormat = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", java.util.Locale.US)
            stampFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val dtStamp = stampFormat.format(java.util.Date())
            val sb = StringBuilder()
            sb.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Quran Nur//Planner//EN\r\n")
            plan.assignments.forEach { a ->
                sb.append("BEGIN:VEVENT\r\n")
                sb.append("UID:planner-${plan.id}-${a.dayNumber}@quran-nur\r\n")
                sb.append("DTSTAMP:$dtStamp\r\n")
                sb.append("DTSTART;VALUE=DATE:${a.date.replace("-", "")}\r\n")
                sb.append("SUMMARY:${esc(a.title)} (Day ${a.dayNumber})\r\n")
                sb.append("DESCRIPTION:${esc(a.subtitle)}\r\n")
                sb.append("END:VEVENT\r\n")
            }
            sb.append("END:VCALENDAR\r\n")
            val chooser = JFileChooser()
            chooser.selectedFile = File("quran-nur-planner.ics")
            if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return "Export cancelled"
            val out = chooser.selectedFile
            out.writeText(sb.toString(), Charsets.UTF_8)
            "Saved to ${out.absolutePath}"
        } catch (_: Exception) {
            "Export failed"
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pal.cream,
            border = androidx.compose.foundation.BorderStroke(1.dp, pal.boneDark),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "Planner Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    fontFamily = fontUi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used to split the day's reading across prayers.",
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    fontFamily = fontBody
                )
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("DAILY PRAYERS")
                Text(
                    text = "Select which prayers you want to distribute reading across.",
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    fontFamily = fontBody
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRAYER_NAMES.forEach { prayer ->
                        val active = prayer in activePrayers
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) pal.teal else pal.white)
                                .border(
                                    1.5.dp,
                                    if (active) pal.teal else pal.boneDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (active && activePrayers.size == 1) return@clickable
                                    activePrayers = if (active) {
                                        activePrayers - prayer
                                    } else {
                                        (activePrayers + prayer).sortedBy { PRAYER_NAMES.indexOf(it) }
                                    }
                                    persist()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = prayer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else pal.inkMuted,
                                fontFamily = fontBody
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("READING PREFERENCE")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "after" to "Read After Prayer",
                        "before" to "Read Before Prayer",
                        "split" to "Split Before & After"
                    ).forEach { (value, label) ->
                        val selected = readPref == value
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) pal.teal else pal.white)
                                .border(
                                    1.5.dp,
                                    if (selected) pal.teal else pal.boneDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    readPref = value
                                    persist()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else pal.inkMuted,
                                fontFamily = fontBody,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Text(
                                    text = "\u2713",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = fontUi
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("INTENTION PROMPTS")
                Text(
                    text = "Show a mindfulness prompt before reading.",
                    fontSize = 12.sp,
                    color = pal.inkMuted,
                    fontFamily = fontBody
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = intentionOn,
                        onCheckedChange = {
                            intentionOn = it
                            persist()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("PRAYER TIMES LOCATION")
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = onCountryChange,
                    label = { Text("Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRefresh,
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pal.teal,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (loading) "Loading…" else "Refresh times", fontFamily = fontUi)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { exportStatus = exportPlanIcs() },
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.teal),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = pal.teal),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        text = "Export to Calendar (.ics)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi
                    )
                }
                if (exportStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exportStatus,
                        fontSize = 12.sp,
                        color = if (exportStatus.startsWith("Saved")) pal.teal else OverdueRed,
                        fontFamily = fontBody
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Done", color = pal.inkMid)
                }
            }
        }
    }
}
