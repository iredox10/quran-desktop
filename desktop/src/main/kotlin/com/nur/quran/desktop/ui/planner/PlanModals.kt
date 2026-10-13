package com.nur.quran.desktop.ui.planner

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLANNER_UNITS
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import kotlin.math.ceil

/**
 * Local archive entry. The store agent persists archives separately;
 * this stays decoupled: a snapshot of the plan plus an ISO timestamp.
 */
data class ArchivedPlan(
    val plan: ReadingPlan,
    val archivedAt: String
)

data class CustomPlanParams(
    val title: String,
    val unitType: String,
    val startUnit: Int,
    val endUnit: Int,
    val perDay: Int,
    val startDate: String,
    val excludeDays: List<Int>
)

private val DayShortNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
private fun ModalShell(pal: NurPalette, onDismiss: () -> Unit, title: String, content: @Composable () -> Unit) {
    val fontUi = rememberUiFontFamily()
    Dialog(onCloseRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = pal.cream,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = fontUi,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.ink,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(pal.bone),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMid,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(pal.boneDark))
                content()
            }
        }
    }
}

@Composable
private fun FieldLabel(pal: NurPalette, text: String) {
    val fontBody = rememberBodyFontFamily()
    Text(
        text = text.uppercase(),
        fontFamily = fontBody,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = pal.inkMuted
    )
}

@Composable
private fun UnitPill(
    pal: NurPalette,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fontBody = rememberBodyFontFamily()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) pal.teal else pal.cream,
        border = BorderStroke(1.5.dp, if (selected) pal.teal else pal.boneDark),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontFamily = fontBody,
            fontSize = 13.sp,
            color = if (selected) pal.white else pal.inkMid,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/**
 * Custom plan builder mirroring web CustomPlanModal:
 * optional name, unit pills, From/To range, per-day number + slider,
 * yyyy-MM-dd start date, Sun–Sat days-off toggles, footer summary.
 */
@Composable
fun CustomPlanModal(
    pal: NurPalette,
    chapters: List<PlanChapter>,
    onDismiss: () -> Unit,
    onCreate: (CustomPlanParams) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var title by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("page") }
    var fromText by remember { mutableStateOf("1") }
    var toText by remember { mutableStateOf((PLANNER_UNITS["page"]?.max ?: 604).toString()) }
    // Web parity (Planner.jsx 158): custom composer opens with 10 units/day.
    var perDayText by remember { mutableStateOf("10") }
    var startDate by remember { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var excludeDays by remember { mutableStateOf(emptyList<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }

    @Suppress("UNUSED_PARAMETER")
    val unusedChapters = chapters

    val unitMeta = PLANNER_UNITS[unitType] ?: PLANNER_UNITS.getValue("page")
    val maxUnit = unitMeta.max
    val fromNum = fromText.toIntOrNull() ?: 1
    val toNum = toText.toIntOrNull() ?: maxUnit
    val safeFrom = fromNum.coerceIn(1, maxUnit)
    val safeTo = toNum.coerceIn(safeFrom, maxUnit)
    val total = (safeTo - safeFrom + 1).coerceAtLeast(0)
    val perDay = perDayText.toIntOrNull() ?: 1
    val computedDuration = if (total > 0 && perDay >= 1) ceil(total.toDouble() / perDay).toInt() else 0
    val sliderMax = minOf(total.coerceAtLeast(1), 50)
    val endLabel = remember(startDate, computedDuration, excludeDays) {
        if (computedDuration <= 0) startDate
        else PlannerEngine.formatPlannerDateLabel(
            PlannerEngine.getReadingDate(startDate, computedDuration - 1, excludeDays)
        )
    }
    val rangeValid = fromNum in 1..maxUnit && toNum in fromNum..maxUnit
    val canCreate = perDay >= 1 && rangeValid && total > 0

    ModalShell(pal = pal, onDismiss = onDismiss, title = "Create Custom Plan") {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(pal, "Plan name (optional)")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    placeholder = { Text("e.g. Morning Routine, Juz Amma…", fontFamily = fontBody, color = pal.inkMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pal.gold, unfocusedBorderColor = pal.boneDark,
                        cursorColor = pal.teal, focusedTextColor = pal.ink, unfocusedTextColor = pal.ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(pal, "Reading unit")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PLANNER_UNITS.forEach { (key, meta) ->
                        UnitPill(pal = pal, label = meta.label, selected = unitType == key) {
                            unitType = key
                            fromText = "1"
                            toText = meta.max.toString()
                            // Web parity (Planner.jsx 158/430): 10/day default,
                            // kept on unit switch like the web's range reset.
                            perDayText = "10"
                            error = null
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(pal, "${unitMeta.label} range")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("From", fontFamily = fontBody, fontSize = 9.sp, letterSpacing = 0.8.sp, color = pal.inkMuted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fromText,
                            onValueChange = { v -> fromText = v.filter { it.isDigit() }.take(3); error = null },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = pal.gold, unfocusedBorderColor = pal.boneDark,
                                cursorColor = pal.teal, focusedTextColor = pal.ink, unfocusedTextColor = pal.ink
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("→", fontSize = 17.sp, color = pal.inkMuted, modifier = Modifier.padding(horizontal = 12.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("To", fontFamily = fontBody, fontSize = 9.sp, letterSpacing = 0.8.sp, color = pal.inkMuted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = toText,
                            onValueChange = { v -> toText = v.filter { it.isDigit() }.take(3); error = null },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = pal.gold, unfocusedBorderColor = pal.boneDark,
                                cursorColor = pal.teal, focusedTextColor = pal.ink, unfocusedTextColor = pal.ink
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // Clamp on commit: mirror web onBlur behaviour lazily at create time,
                // but keep live hint accurate.
                Text(
                    text = "$total ${unitMeta.plural} selected",
                    fontFamily = fontBody,
                    fontSize = 10.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(pal, "${unitMeta.plural} per day")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = perDayText,
                        onValueChange = { v -> perDayText = v.filter { it.isDigit() }.take(2); error = null },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.gold, unfocusedBorderColor = pal.boneDark,
                            cursorColor = pal.teal, focusedTextColor = pal.ink, unfocusedTextColor = pal.ink
                        ),
                        modifier = Modifier.width(76.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = perDay.coerceIn(1, sliderMax).toFloat(),
                        onValueChange = { perDayText = it.toInt().coerceAtLeast(1).toString(); error = null },
                        valueRange = 1f..sliderMax.toFloat(),
                        steps = (sliderMax - 1).coerceAtLeast(0),
                        colors = SliderDefaults.colors(thumbColor = pal.teal, activeTrackColor = pal.teal, inactiveTrackColor = pal.boneDark),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(pal, "Start date")
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it; error = null },
                    placeholder = { Text("yyyy-MM-dd", fontFamily = fontBody, color = pal.inkMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pal.gold, unfocusedBorderColor = pal.boneDark,
                        cursorColor = pal.teal, focusedTextColor = pal.ink, unfocusedTextColor = pal.ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(pal, "Days Off (Optional)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayShortNames.forEachIndexed { idx, day ->
                        val selected = excludeDays.contains(idx)
                        UnitPill(pal = pal, label = day, selected = selected) {
                            excludeDays = if (selected) excludeDays - idx else excludeDays + idx
                        }
                    }
                }
                Text(
                    text = "Reading assignments will skip these days.",
                    fontFamily = fontBody,
                    fontSize = 12.sp,
                    color = pal.inkMuted
                )
            }

            if (error != null) {
                Text(text = error!!, fontFamily = fontBody, fontSize = 12.sp, color = pal.teal)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(pal.boneDark))
            Text(
                text = "$perDay ${unitMeta.plural}/day · $computedDuration days · Done by $endLabel",
                fontFamily = fontBody,
                fontSize = 11.sp,
                color = pal.inkMid,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Button(
                onClick = {
                    if (!canCreate) {
                        error = when {
                            perDay < 1 -> "Set at least 1 ${unitMeta.plural.lowercase()} per day."
                            !rangeValid -> "Range must be within 1..$maxUnit with From ≤ To."
                            else -> "Nothing selected in this range."
                        }
                        return@Button
                    }
                    onCreate(
                        CustomPlanParams(
                            title = title.trim(),
                            unitType = unitType,
                            startUnit = safeFrom,
                            endUnit = safeTo,
                            perDay = perDay,
                            startDate = startDate.trim(),
                            excludeDays = excludeDays.sorted()
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "CREATE PLAN",
                    fontFamily = fontBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = pal.white
                )
            }
            // Keep fontUi referenced for header styling consistency.
            @Suppress("UNUSED_EXPRESSION")
            fontUi
        }
    }
}

/** Past-plans archive mirroring the web Archives modal. */
@Composable
fun ArchivesModal(
    pal: NurPalette,
    archived: List<ArchivedPlan>,
    onDismiss: () -> Unit,
    onRestore: (ArchivedPlan) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    ModalShell(pal = pal, onDismiss = onDismiss, title = "Past Plans Archive") {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (archived.isEmpty()) {
                // Web parity (Planner.jsx 529-530): centered italic empty state.
                Text(
                    text = "No archived plans yet.",
                    fontFamily = fontBody,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 13.sp,
                    color = pal.inkMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else {
                archived.forEach { entry ->
                    val plan = entry.plan
                    val finished = plan.completedDays.size
                    val isCompleted = finished == plan.durationDays
                    // Web parity (Planner.jsx 547): split ISO on "T", then short label.
                    val archivedLabel = remember(entry.archivedAt) {
                        PlannerEngine.formatPlannerDateLabel(entry.archivedAt.substringBefore("T"))
                    }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.bone),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.title.ifBlank { "Plan" },
                                    fontFamily = fontBody,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.ink,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isCompleted) pal.goldSoft else pal.cream
                                ) {
                                    // Web parity (Planner.jsx 539): mono uppercase
                                    // COMPLETED / ENDED badge.
                                    Text(
                                        text = if (isCompleted) "COMPLETED" else "ENDED",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.6.sp,
                                        color = if (isCompleted) pal.gold else pal.inkMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            // Web parity (Planner.jsx 543-548): mono daily-progress
                            // line + italic archived date line (short label).
                            Text(
                                text = "${plan.durationDays} days \u00B7 $finished days finished",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 0.4.sp,
                                color = pal.inkMuted
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Archived on $archivedLabel",
                                fontFamily = fontBody,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 12.sp,
                                color = pal.inkMid
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onRestore(entry) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, pal.teal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Restore",
                                    fontFamily = fontUi,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = pal.teal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Template start-confirmation mirroring the web TemplatePreview modal
 * (quran-app Planner.jsx 561-612): title header, description, 2×2 stat grid
 * (Duration / Pace / Starts / Done by) derived from the already-built plan,
 * Cancel + Create Plan footer. [plan] is the built ReadingPlan produced by
 * handleTemplateSelect-equivalent at tap time; [onConfirm] adopts it directly
 * (web: onBegin(previewPlan)) instead of rebuilding.
 */
@Composable
fun TemplatePreviewModal(
    pal: NurPalette,
    plan: ReadingPlan,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    // Web parity: pace from built assignments, not the template range —
    // sum(items) / durationDays, so surah/juz chunking is reflected.
    val planUnits = remember(plan) { plan.assignments.sumOf { it.items.size } }
    val perDay = remember(planUnits, plan) {
        maxOf(ceil(planUnits.toDouble() / plan.durationDays.coerceAtLeast(1)).toInt(), 1)
    }
    val unitMeta = PLANNER_UNITS[plan.unitType]
    val endLabel = remember(plan) {
        PlannerEngine.getPaceStats(
            plan.durationDays,
            plan.excludeDays,
            plan.startDate,
            planUnits
        ).endLabel
    }
    val stats = listOf(
        "Duration" to "${plan.durationDays} days",
        "Pace" to "$perDay ${unitMeta?.plural ?: "units"}/day",
        "Starts" to PlannerEngine.formatPlannerDateLabel(plan.startDate),
        "Done by" to endLabel
    )

    ModalShell(pal = pal, onDismiss = onDismiss, title = "Start \"${plan.title}\"?") {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = plan.title,
                fontFamily = fontBody,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = pal.inkMid
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, value) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = pal.bone,
                                border = BorderStroke(1.5.dp, pal.boneDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = label.uppercase(),
                                        fontFamily = fontBody,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = pal.inkMuted
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = value,
                                        fontFamily = fontUi,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink
                                    )
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = fontBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = pal.inkMid
                    )
                }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text(
                        text = "Create Plan",
                        fontFamily = fontBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = pal.white
                    )
                }
            }
        }
    }
}
