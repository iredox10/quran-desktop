package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.PrayerPrefs
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PLANNER_UNITS
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import java.io.File
import javax.swing.JFileChooser

private val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

/**
 * PROGRESS-tab dialogs, mirroring web Planner.jsx ActiveView modals:
 * Adjust Pace (newDuration stepper, min = completedCount + 1, computed daily pace & completion date preview),
 * Rebalance (Extend Schedule vs Redistribute Reading with selection & Apply),
 * Delete Plan Dialog, Replace Plan Dialog, and Planner Settings Dialog.
 */

@Composable
fun AdjustPaceDialog(pal: NurPalette, plan: ReadingPlan, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val minDuration = maxOf(1, minOf(plan.durationDays, plan.completedDays.size + 1))
    var durationText by remember(plan.id) { mutableStateOf(plan.durationDays.toString()) }
    val parsed = durationText.toIntOrNull() ?: minDuration
    val newDuration = parsed.coerceIn(minDuration, 1000)

    val planUnits = remember(plan) {
        val count = plan.assignments.sumOf { it.items.size }
        if (count > 0) count else (PLANNER_UNITS[plan.unitType]?.max ?: 604)
    }
    val unitMeta = PLANNER_UNITS[plan.unitType] ?: PLANNER_UNITS.getValue("page")
    val stats = remember(newDuration, plan) {
        PlannerEngine.getPaceStats(newDuration, plan.excludeDays, plan.startDate, planUnits)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = pal.cream,
            border = BorderStroke(1.dp, pal.boneDark),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Adjust Pace",
                        fontFamily = fontUi,
                        fontSize = 22.4.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pal.bone),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMid,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Web parity (Planner.jsx 1336-1338).
                Text(
                    text = "Change how many days you want to complete your remaining plan in. " +
                        "This will re-calculate your daily assignments.",
                    fontFamily = fontBody,
                    fontSize = 14.4.sp,
                    lineHeight = 22.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NEW TOTAL DAYS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = pal.gold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Stepper + number input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = (newDuration - 1).coerceAtLeast(minDuration)
                            durationText = next.toString()
                        },
                        enabled = newDuration > minDuration,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.white,
                            contentColor = pal.ink
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease days", tint = pal.ink, modifier = Modifier.size(18.dp))
                    }

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { value ->
                            val clean = value.filter { it.isDigit() }.take(4)
                            durationText = clean
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = fontUi,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.ink,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        trailingIcon = {
                            Text(
                                text = "DAYS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.inkMuted,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.teal,
                            unfocusedBorderColor = pal.boneDark,
                            focusedContainerColor = pal.white,
                            unfocusedContainerColor = pal.white,
                            cursorColor = pal.teal
                        ),
                        modifier = Modifier.weight(1f).height(56.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val next = (newDuration + 1).coerceAtMost(1000)
                            durationText = next.toString()
                        },
                        enabled = newDuration < 1000,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.white,
                            contentColor = pal.ink
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase days", tint = pal.ink, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Displays computed pages/day and new completion date
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = pal.white,
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY PACE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = pal.inkMuted
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${stats.dailyPages} ${unitMeta.plural.lowercase()}/day",
                                fontFamily = fontUi,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "NEW END DATE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = pal.inkMuted
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stats.endLabel,
                                fontFamily = fontUi,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.teal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.white,
                            contentColor = pal.inkMid
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = fontUi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onApply(newDuration) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.gold),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontFamily = fontUi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RebalanceDialog(pal: NurPalette, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var selectedOption by remember { mutableStateOf("extend") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = pal.white,
            border = BorderStroke(1.dp, pal.boneDark),
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rebalance Plan",
                        fontFamily = fontUi,
                        fontSize = 22.4.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(pal.bone)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = pal.inkMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "You missed some pages from past assignments. " +
                        "How would you like to catch up?",
                    fontFamily = fontBody,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(20.dp))
                RebalanceOption(
                    pal = pal,
                    title = "Extend Schedule",
                    body = "Keep your daily reading amount exactly the same, but add extra days to the end of your plan.",
                    selected = selectedOption == "extend",
                    onClick = { selectedOption = "extend" }
                )
                Spacer(modifier = Modifier.height(12.dp))
                RebalanceOption(
                    pal = pal,
                    title = "Redistribute Reading",
                    body = "Keep your original deadline, but evenly distribute the missed pages across your remaining days.",
                    selected = selectedOption == "spread",
                    onClick = { selectedOption = "spread" }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.white,
                            contentColor = pal.inkMid
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = fontUi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onPick(selectedOption) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontFamily = fontUi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePlanDialog(
    pal: NurPalette,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pal.cream,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text(
                    text = "Delete this plan?",
                    fontFamily = fontUi,
                    fontSize = 22.4.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Web parity (Planner.jsx 1607).
                Text(
                    text = "All progress will be permanently lost. This cannot be undone.",
                    fontFamily = fontBody,
                    fontSize = 14.4.sp,
                    lineHeight = 23.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        contentPadding = PaddingValues(vertical = 13.6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = fontBody,
                            fontSize = 14.4.sp,
                            color = pal.inkMid
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC0392B)
                        ),
                        contentPadding = PaddingValues(vertical = 13.6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Delete",
                            fontFamily = fontBody,
                            fontSize = 14.4.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReplacePlanDialog(
    pal: NurPalette,
    planTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pal.cream,
            border = BorderStroke(1.dp, pal.boneDark),
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = "Replace plan?",
                    fontFamily = fontUi,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You already have a \"$planTitle\" plan. Replace it with a new one?",
                    fontFamily = fontBody,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel", fontFamily = fontBody, fontSize = 14.sp, color = pal.inkMid)
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            "Replace",
                            fontFamily = fontBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RebalanceOption(
    pal: NurPalette,
    title: String,
    body: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) pal.bone else pal.white,
        border = BorderStroke(2.dp, if (selected) pal.teal else pal.bone),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = fontUi,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (selected) pal.teal else pal.boneDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Filled.Check else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = if (selected) Color.White else pal.teal,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = body,
                fontFamily = fontBody,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = pal.inkMid
            )
        }
    }
}

/**
 * Planner Settings dialog (web `Planner.jsx` 1362-1454 modal parity):
 * title with close X, daily prayers pills, reading preference dropdown,
 * intention prompts switch, prayer times location display with Enable/Update,
 * export reading schedule to .ics calendar file, and Done button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerSettingsDialog(
    pal: NurPalette,
    city: String = "",
    country: String = "",
    loading: Boolean = false,
    onCityChange: (String) -> Unit = {},
    onCountryChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val savedPrefs = remember { PlannerStore.getPrayerPrefs() }
    var activePrayers by remember { mutableStateOf(savedPrefs.activePrayers) }
    var readPref by remember { mutableStateOf(savedPrefs.readPreference) }
    var intentionOn by remember { mutableStateOf(savedPrefs.intentionPromptEnabled) }
    var exportStatus by remember { mutableStateOf("") }
    var showLocationEditor by remember { mutableStateOf(false) }

    fun persist() {
        PlannerStore.savePrayerPrefs(PrayerPrefs(activePrayers, readPref, intentionOn))
    }

    @Composable
    fun SectionHeader(label: String) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            color = pal.gold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    fun esc(text: String): String = text
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")

    fun exportPlanIcs(): String {
        val plan = PlannerStore.getActivePlan() ?: return "No active plan to export."
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
            "Saved schedule to ${out.name}"
        } catch (_: Exception) {
            "Export failed"
        }
    }

    val locationDisplay = if (city.isNotBlank() && country.isNotBlank()) {
        "$city, $country"
    } else if (city.isNotBlank()) {
        city
    } else {
        "Not set — using defaults"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = pal.cream,
            border = BorderStroke(1.dp, pal.boneDark),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(26.dp).verticalScroll(rememberScrollState())) {
                // Header with title and close action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Planner Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pal.bone),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = pal.inkMid,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Daily Prayers
                SectionHeader("Daily Prayers")
                Text(
                    text = "Select which prayers you want to distribute reading across.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = pal.inkMid,
                    fontFamily = fontBody
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                                    if (active && activePrayers.size == 1) return@clickable // Minimum 1 required
                                    activePrayers = if (active) {
                                        activePrayers - prayer
                                    } else {
                                        (activePrayers + prayer).sortedBy { PRAYER_NAMES.indexOf(it) }
                                    }
                                    persist()
                                }
                                .padding(horizontal = 15.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = prayer,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else pal.inkMuted,
                                fontFamily = fontBody
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))

                // Reading Preference
                SectionHeader("Reading Preference")
                var readPrefOpen by remember { mutableStateOf(false) }
                val readPrefLabel = when (readPref) {
                    "before" -> "Read Before Prayer"
                    "split" -> "Split Before & After"
                    else -> "Read After Prayer"
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(pal.white)
                            .border(1.5.dp, pal.boneDark, RoundedCornerShape(16.dp))
                            .clickable { readPrefOpen = true }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = readPrefLabel,
                            fontFamily = fontUi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = pal.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = pal.inkMuted
                        )
                    }
                    DropdownMenu(
                        expanded = readPrefOpen,
                        onDismissRequest = { readPrefOpen = false }
                    ) {
                        listOf(
                            "after" to "Read After Prayer",
                            "before" to "Read Before Prayer",
                            "split" to "Split Before & After"
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(label, fontFamily = fontUi, color = pal.ink)
                                },
                                onClick = {
                                    readPref = value
                                    persist()
                                    readPrefOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(pal.boneDark))
                Spacer(modifier = Modifier.height(18.dp))

                // Intention Prompts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "INTENTION PROMPTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = pal.gold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Show a mindfulness prompt before reading.",
                            fontSize = 13.sp,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                    }
                    Switch(
                        checked = intentionOn,
                        onCheckedChange = {
                            intentionOn = it
                            persist()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = pal.green
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(pal.boneDark))
                Spacer(modifier = Modifier.height(18.dp))

                // Prayer Times Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRAYER TIMES LOCATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = pal.gold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = locationDisplay,
                            fontSize = 13.sp,
                            color = pal.inkMid,
                            fontFamily = fontBody
                        )
                    }
                    OutlinedButton(
                        onClick = { showLocationEditor = !showLocationEditor },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.white,
                            contentColor = pal.teal
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (city.isNotBlank()) "Update" else "Enable",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showLocationEditor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = city,
                        onValueChange = onCityChange,
                        label = { Text("City") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.teal,
                            unfocusedBorderColor = pal.boneDark,
                            focusedContainerColor = pal.white,
                            unfocusedContainerColor = pal.white
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = country,
                        onValueChange = onCountryChange,
                        label = { Text("Country") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.teal,
                            unfocusedBorderColor = pal.boneDark,
                            focusedContainerColor = pal.white,
                            unfocusedContainerColor = pal.white
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRefresh,
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.teal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (loading) "Loading…" else "Refresh prayer times", fontFamily = fontUi)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(pal.boneDark))
                Spacer(modifier = Modifier.height(18.dp))

                // Export to Calendar (.ics)
                OutlinedButton(
                    onClick = { exportStatus = exportPlanIcs() },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = pal.white,
                        contentColor = pal.ink
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = pal.ink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export to Calendar (.ics)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontUi
                    )
                }
                if (exportStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exportStatus,
                        fontSize = 12.sp,
                        color = if (exportStatus.startsWith("Saved")) pal.teal else Color(0xFFC0392B),
                        fontFamily = fontBody
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                // Done button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, pal.boneDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = pal.white,
                        contentColor = pal.ink
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontUi
                    )
                }
            }
        }
    }
}
