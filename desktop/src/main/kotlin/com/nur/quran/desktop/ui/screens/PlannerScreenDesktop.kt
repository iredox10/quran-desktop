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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.nur.quran.desktop.ui.input.dragToScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.nur.quran.desktop.ui.components.copyToClipboard
import com.nur.quran.desktop.ui.input.touchTarget
import kotlinx.coroutines.delay
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
import com.nur.quran.desktop.ui.planner.DeletePlanDialog
import com.nur.quran.desktop.ui.planner.IntentionView
import com.nur.quran.desktop.ui.planner.JournalTab
import com.nur.quran.desktop.ui.planner.MetricGlassCard
import com.nur.quran.desktop.ui.planner.PaceRing
import com.nur.quran.desktop.ui.planner.PlannerSettingsDialog
import com.nur.quran.desktop.ui.planner.RebalanceDialog
import com.nur.quran.desktop.ui.planner.ReplacePlanDialog
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
    var showDeletePlan by remember { mutableStateOf(false) }
    var showAutoRebalance by remember { mutableStateOf(false) }
    var pendingReplace by remember {
        mutableStateOf<Pair<ReadingPlan, ReadingPlan>?>(null)
    }

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

    // Web parity (Planner.jsx 1523-1528): the page opens on the Intention view
    // unless the active plan already has progress. Creating/switching a plan
    // explicitly enters the active view.
    var showActive by remember(activePlan != null) { mutableStateOf(activePlan != null) }
    LaunchedEffect(activePlan?.id) {
        if (activePlan == null) showActive = false
    }

    // Web parity (ActiveView useEffect 687-695): nudge a rebalance once when
    // the first incomplete assignment is already in the past.
    LaunchedEffect(activePlan?.id, todayStr) {
        val plan = activePlan ?: return@LaunchedEffect
        val firstIncomplete = PlannerEngine.getPlannerOverview(plan, todayStr)?.firstIncomplete
        if (firstIncomplete != null && firstIncomplete.date < todayStr) {
            showAutoRebalance = true
        }
    }

    fun adoptPlan(plan: ReadingPlan) {
        val updated = PlannerStore.getAllPlans().filterNot { it.id == plan.id } + plan
        PlannerStore.saveAllPlans(updated)
        PlannerStore.saveActivePlan(plan)
        customError = null
        showActive = true
        refresh++
    }

    // Web parity (Planner.jsx handleBegin 1537-1547): a plan with the same
    // title asks to replace the existing one before being added.
    fun beginPlan(plan: ReadingPlan) {
        val existing = PlannerStore.getAllPlans().firstOrNull {
            it.title.isNotBlank() && plan.title.isNotBlank() && it.title == plan.title
        }
        if (existing != null) {
            pendingReplace = existing to plan
            return
        }
        adoptPlan(plan)
        showActive = true
    }

    fun activatePlan(plan: ReadingPlan) {
        PlannerStore.saveActivePlan(plan)
        showActive = true
        refresh++
    }

    // Web parity (useAppStore deletePlanner): deleting the active plan falls
    // back to the first remaining plan; deleting the last plan clears active.
    fun deletePlan(plan: ReadingPlan) {
        val remaining = PlannerStore.getAllPlans().filterNot { it.id == plan.id }
        PlannerStore.saveAllPlans(remaining)
        if (plan.id == activePlan?.id) {
            val next = remaining.firstOrNull()
            PlannerStore.saveActivePlan(next)
            if (next == null) showActive = false
        }
        if (remaining.isEmpty()) showActive = false
        refresh++
    }

    /** Prayer-slot mark/undo: mirrors web setPlannerAssignmentProgress. */
    fun setSlotProgress(plan: ReadingPlan, dayDate: String, count: Int) {
        val assignment = plan.assignments.firstOrNull { it.date == dayDate } ?: return
        val totalItems = assignment.items.size
        val clamped = count.coerceIn(0, totalItems)

        val progress = plan.assignmentProgress.toMutableMap()
        progress[assignment.dayNumber] = clamped

        // Web parity: completion is the first N items, and completedDays is
        // derived from the progress map (not from item-level explicit marks).
        val completedItems = plan.assignmentCompletedItems.toMutableMap()
        completedItems[assignment.dayNumber] =
            assignment.items.take(clamped).map { it.rangeValue }

        val completedAt = plan.assignmentCompletedAt.toMutableMap()
        if (clamped >= totalItems) {
            completedAt.putIfAbsent(assignment.dayNumber, PlannerEngine.formatPlannerDate())
        } else {
            completedAt.remove(assignment.dayNumber)
        }

        val completedDays = plan.assignments
            .filter { (progress[it.dayNumber] ?: 0) >= it.items.size }
            .map { it.dayNumber }
            .sorted()

        PlannerStore.saveActivePlan(
            plan.copy(
                assignmentProgress = progress,
                assignmentCompletedItems = completedItems,
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
        beginPlan(plan)
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
            beginPlan(built)
        } catch (e: Exception) {
            customError = e.message ?: "Could not create plan"
        }
    }

    // ── Intention view (no progress yet, or back from the active view) ──
    if (!showActive || activePlan == null) {
        val intentionScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pal.white)
                .verticalScroll(intentionScroll)
                .dragToScroll(intentionScroll)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            IntentionView(
                pal = pal,
                hasActivePlan = activePlan != null,
                plans = allPlans,
                activePlanId = activePlan?.id,
                chapters = planChapters(),
                onBegin = { beginPlan(it) },
                onViewActive = { showActive = true },
                onSwitchPlan = { id ->
                    allPlans.firstOrNull { it.id == id }?.let {
                        activatePlan(it)
                        showActive = true
                    }
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        val dashboardListState = rememberLazyListState()
        LazyColumn(
            state = dashboardListState,
            modifier = Modifier.fillMaxSize().background(pal.white).dragToScroll(dashboardListState),
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
            val overallPct = Math.round(overview.completionRatio * 100)
            val completionDate = try {
                PlannerEngine.formatPlannerShortDate(
                    PlannerEngine.addDays(plan.startDate, plan.durationDays - 1)
                )
            } catch (_: Exception) {
                plan.startDate
            }
            val streak = metrics?.consistencyStreak ?: 0
            val unitsLabel = PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
            val todayDone = todayProgress?.readPagesCount ?: 0
            val todayTotal = (todayProgress?.totalPagesCount ?: 1).coerceAtLeast(1)
            // Web parity (Planner.jsx line 673): started = pages read OR last read page.
            val hasStartedReading = todayDone > 0 || plan.lastReadPage != null
            // Web parity (Planner.jsx lines 705-714): next-prayer-aware subtitle
            // with the "All done for today!" fallback (slots need no timings for status).
            val nextPrayerName = remember(plan, todayAssignment) {
                runCatching {
                    val prefs = PlannerStore.getPrayerPrefs()
                    buildPrayerSlots(
                        plan = plan,
                        todayAssignment = todayAssignment,
                        prayerTimings = null,
                        readPreference = prefs.readPreference,
                        activePrayers = prefs.activePrayers
                    ).firstOrNull { it.status == "current" || it.status == "upcoming" }?.name
                }.getOrNull()
            }
            // Web parity (Planner.jsx 811-819): exact subtitle cascade.
            val daySubtitle = when {
                planDone -> "Plan complete! \uD83C\uDF89"
                todayAssignment == null -> "Starting soon\u2026"
                todayComplete && nextAssignment != null ->
                    "Day $currentDay complete! \uD83C\uDF89 \u00B7 Day ${nextAssignment.dayNumber} ready"
                todayComplete -> "Day $currentDay complete! \uD83C\uDF89"
                todayDone > 0 && todayDone < todayTotal && nextPrayerName != null ->
                    "$todayDone of $todayTotal $unitsLabel read \u00B7 $nextPrayerName next"
                nextPrayerName != null ->
                    "$todayTotal $unitsLabel today \u00B7 start with $nextPrayerName"
                else -> "All done for today! \u2713"
            }
            val ctaLabel = when {
                planDone -> "Plan Complete \u2713"
                todayComplete && nextAssignment != null -> "Continue to Day ${nextAssignment.dayNumber}"
                todayComplete -> "All Caught Up"
                hasStartedReading -> "Resume Reading"
                else -> "Open Al-Quran"
            }
            // Web parity (Planner.jsx 824): firstIncomplete always exists, so the
            // CTA route is never null for a non-empty plan.
            val ctaDay = (nextAssignment ?: overview.firstIncomplete).date
            var switcherOpen by remember { mutableStateOf(false) }
            var copiedShare by remember { mutableStateOf(false) }
            LaunchedEffect(copiedShare) {
                if (copiedShare) {
                    delay(2000)
                    copiedShare = false
                }
            }

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
                            .touchTarget()
                            .clickable { showActive = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = pal.teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (allPlans.size > 1) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .touchTarget()
                                        .clickable { switcherOpen = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = plan.title.ifBlank { "Quran Plan" },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.ink,
                                        fontFamily = fontUi
                                    )
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = "Switch plan",
                                        tint = pal.inkMid,
                                        modifier = Modifier.size(24.dp)
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
                                            },
                                            modifier = Modifier.touchTarget()
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = plan.title.ifBlank { "Quran Plan" },
                                fontSize = 24.sp,
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
                    Spacer(modifier = Modifier.width(14.dp))

                    // Right header actions: Action buttons (Share, Settings, Delete) + Primary CTA
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share Progress button
                            val shareText = "I've completed ${overview.completedCount} days of my ${plan.durationDays}-day Quran reading plan on Quran Nur! I'm currently on Day $currentDay."
                            IconButton(
                                onClick = { copiedShare = copyToClipboard(shareText) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(pal.white)
                                    .border(1.dp, pal.boneDark, CircleShape)
                                    .touchTarget()
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = if (copiedShare) "Progress copied!" else "Share progress",
                                    tint = if (copiedShare) pal.green else pal.inkMid,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Settings gear button
                            IconButton(
                                onClick = { showPlannerSettings = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(pal.white)
                                    .border(1.dp, pal.boneDark, CircleShape)
                                    .touchTarget()
                            ) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Planner settings",
                                    tint = pal.inkMid,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Delete trashcan button
                            IconButton(
                                onClick = { showDeletePlan = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(pal.white)
                                    .border(1.dp, pal.boneDark, CircleShape)
                                    .touchTarget()
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete plan",
                                    tint = pal.inkMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Primary CTA Button
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
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                            modifier = Modifier.touchTarget()
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
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3 Top metric glass cards:
                // 1. Overall Progress (overallPct%, X of Y days finished)
                // 2. Finish by (completionDate formatted like "Nov 28")
                // 3. Streak (X Days)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricGlassCard(
                        label = "OVERALL",
                        value = "$overallPct%",
                        subtitle = "${overview.completedCount} of ${plan.durationDays} days finished",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = pal.inkMuted.copy(alpha = 0.5f),
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f)
                    )

                    MetricGlassCard(
                        label = "FINISH BY",
                        value = completionDate,
                        subtitle = "${plan.durationDays} days total",
                        icon = Icons.Filled.CalendarMonth,
                        iconTint = pal.inkMuted.copy(alpha = 0.5f),
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f)
                    )

                    MetricGlassCard(
                        label = "STREAK",
                        value = "${streak} Day${if (streak != 1) "s" else ""}",
                        subtitle = if (streak > 0) "Consistency streak" else "Start your streak",
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = Color(0xFFF59E0B).copy(alpha = 0.85f),
                        pal = pal,
                        fontUi = fontUi,
                        fontBody = fontBody,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Tab row pills: [Today | Progress | Journal] ──
        item(key = "tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(pal.cream)
                    .border(1.5.dp, pal.boneDark, RoundedCornerShape(18.dp))
                    .padding(5.dp),
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
                            .touchTarget()
                            .clickable { tab = index }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
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
                                onAdoptPlan = { adoptPlan(it) },
                                onOpenDay = { date -> onOpenDay(date) }
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
                            onOpenSurah = onOpenSurah
                        )
                    }
                }
            }
        }

        // ── Footer: delete plan (web Planner.jsx 1306-1315) ──
        item(key = "delete-plan-footer") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { showDeletePlan = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = pal.inkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete plan",
                        fontFamily = fontBody,
                        fontSize = 13.sp,
                        color = pal.inkMuted
                    )
                }
            }
        }

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
            onRestore = { planId ->
                PlannerStore.restoreArchived(planId)
                refresh++
            },
            onDelete = { planId ->
                PlannerStore.deleteArchived(planId)
                refresh++
            }
        )
    }
    pendingReplace?.let { (existing, replacement) ->
        ReplacePlanDialog(
            pal = pal,
            planTitle = replacement.title,
            onDismiss = { pendingReplace = null },
            onConfirm = {
                deletePlan(existing)
                adoptPlan(replacement)
                showActive = true
                pendingReplace = null
            }
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
    if (showDeletePlan && activePlan != null) {
        DeletePlanDialog(
            pal = pal,
            onDismiss = { showDeletePlan = false },
            onConfirm = {
                showDeletePlan = false
                deletePlan(activePlan)
            }
        )
    }
    if (showAutoRebalance && activePlan != null) {
        RebalanceDialog(
            pal = pal,
            onDismiss = { showAutoRebalance = false },
            onPick = { strategy ->
                adoptPlan(PlannerEngine.rebalancePlanner(activePlan, strategy))
                showAutoRebalance = false
            }
        )
    }
}

@Composable
private fun MetricGlassCard(
    label: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    pal: NurPalette,
    fontUi: androidx.compose.ui.text.font.FontFamily,
    fontBody: androidx.compose.ui.text.font.FontFamily,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pal.cream.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, pal.boneDark),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
            )
            Column {
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
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = fontBody,
                    fontSize = 11.sp,
                    color = pal.inkMuted
                )
            }
        }
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

