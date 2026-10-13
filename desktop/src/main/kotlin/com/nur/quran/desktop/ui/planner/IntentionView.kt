package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlanTemplate
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import kotlin.math.ceil

data class PaceChoice(val id: String, val title: String, val durationDays: Int)

private data class PaceUi(
    val choice: PaceChoice,
    val badge: String? = null
)

private val PACES_UI = listOf(
    PaceUi(PaceChoice("ramadan", "Ramadan Pace", 30)),
    PaceUi(PaceChoice("steady", "Steady Journey", 60), badge = "MOST BALANCED"),
    PaceUi(PaceChoice("devotional", "Devotional Path", 365))
)

/** Web parity (Planner.jsx PACES icons): moon for Ramadan, sun for Steady, gem for Devotional. */
private val PaceUi.icon
    get() = when (choice.id) {
        "ramadan" -> Icons.Filled.Nightlight
        "devotional" -> Icons.Filled.Diamond
        else -> Icons.Filled.WbSunny
    }

/** Web parity (Planner.jsx hero logo mark above THE FIRST STEP, shown at low opacity). */
@Composable
private fun IntentionLogoMark(pal: NurPalette) {
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            drawArc(
                color = pal.teal.copy(alpha = 0.22f),
                startAngle = 40f,
                sweepAngle = 280f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx())
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(pal.teal.copy(alpha = 0.22f))
        )
    }
}

@Composable
fun IntentionView(
    pal: NurPalette,
    hasActivePlan: Boolean,
    plans: List<ReadingPlan>,
    activePlanId: String?,
    chapters: List<PlanChapter>,
    onBegin: (ReadingPlan) -> Unit,
    onViewActive: () -> Unit,
    onSwitchPlan: (String) -> Unit,
    onDeletePlan: (String) -> Unit,
    onNewCustom: () -> Unit,
    onOpenArchives: () -> Unit,
    archivedCount: Int = 0,
    templates: List<PlanTemplate> = emptyList(),
    onPickTemplate: (PlanTemplate) -> Unit = {},
    chaptersReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val today = remember { PlannerEngine.formatPlannerDate() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // (1) Continue active plan button
        if (hasActivePlan) {
            Button(
                onClick = onViewActive,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue my active plan",
                    fontFamily = fontBody,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 15.sp,
                    color = pal.white,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = pal.white,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // (2) Hero
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IntentionLogoMark(pal)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "THE FIRST STEP",
                fontFamily = fontBody,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = pal.gold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set Your Intention",
                fontFamily = fontUi,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a pace that resonates with your soul. Whether intensive or slow, the journey of the Quran is a dialogue of devotion.",
                fontFamily = fontBody,
                fontSize = 13.5.sp,
                lineHeight = 21.sp,
                color = pal.inkMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\u201CThe best of deeds are those done consistently, even if small.\u201D",
                fontFamily = fontBody,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = pal.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // (3) Pace cards — web parity (Planner.jsx 242-299): horizontal 3-up row,
        // badge on the top edge, gold border when selected.
        var selectedPace by remember { mutableStateOf("steady") }
        var beginError by remember { mutableStateOf<String?>(null) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PACES_UI.forEach { paceUi ->
                val pace = paceUi.choice
                val isSelected = selectedPace == pace.id
                val dailyPages = ceil(604.0 / pace.durationDays).toInt()
                val perPrayer = ceil(dailyPages / 5.0).toInt()
                val endLabel = try {
                    PlannerEngine.formatPlannerDateLabel(
                        PlannerEngine.addDays(today, pace.durationDays - 1)
                    )
                } catch (_: Exception) {
                    ""
                }
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.cream),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) pal.gold else pal.boneDark
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 8.dp else 3.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable { selectedPace = pace.id }
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                                .padding(top = if (paceUi.badge != null) 8.dp else 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PaceCardRing(
                                pal = pal,
                                durationDays = pace.durationDays,
                                dailyPages = dailyPages,
                                selected = isSelected
                            )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pace.title,
                            fontFamily = fontUi,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.ink
                        )
                        Text(
                            text = "$dailyPages pages per day",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp,
                            color = pal.inkMid
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = paceUi.icon,
                                contentDescription = null,
                                tint = pal.inkMid,
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                text = "$perPrayer pages per prayer",
                                fontFamily = fontBody,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 13.sp,
                                color = pal.inkMid
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isSelected) {
                            // Web parity (Planner.jsx 281-296): selected card expands an
                            // inline "pages/day · Done by <date>" summary row above a
                            // full-width teal journey button.
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        5.dp,
                                        Alignment.CenterHorizontally
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$dailyPages pages/day",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.3.sp,
                                        color = pal.inkMid
                                    )
                                    Text(
                                        text = "·",
                                        fontFamily = fontBody,
                                        fontSize = 11.sp,
                                        color = pal.boneDark
                                    )
                                    Text(
                                        text = "Done by $endLabel",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.3.sp,
                                        color = pal.inkMid
                                    )
                                }
                            Button(
                                onClick = {
                                    try {
                                        val built = PlannerEngine.buildReadingPlanner(
                                            unitType = "page",
                                            durationDays = pace.durationDays,
                                            startDate = today,
                                            startUnit = 1,
                                            endUnit = 604,
                                            customTitle = pace.title,
                                            excludeDays = emptyList(),
                                            chapters = chapters
                                        )
                                        onBegin(built)
                                        beginError = null
                                    } catch (e: Exception) {
                                        beginError = e.message ?: "Could not build plan"
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "BEGIN MY JOURNEY",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 1.6.sp,
                                    color = pal.white
                                )
                            }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedPace = pace.id },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, pal.boneDark),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "SELECT PATH",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 1.2.sp,
                                    color = pal.inkMid
                                )
                            }
                        }
                    }
                    }
                    if (paceUi.badge != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = pal.teal,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-11).dp)
                        ) {
                            Text(
                                text = paceUi.badge,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                color = pal.white,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // (3b) Plan-creation errors — web parity (Planner.jsx handleBegin alert()).
        if (beginError != null) {
            Text(
                text = beginError!!,
                fontFamily = fontBody,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = Color(0xFFC0392B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        // (4) My Plans list — web parity (Planner.jsx 302-317): header row with
        // bottom divider, title left, Archives link + circular + action right.
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Plans",
                    fontFamily = fontUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink,
                    modifier = Modifier.weight(1f)
                )
                if (archivedCount > 0) {
                    Text(
                        text = "Archives ($archivedCount)",
                        fontFamily = fontBody,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                        color = pal.teal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenArchives() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier.size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, pal.boneDark, CircleShape)
                        .clickable { onNewCustom() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create custom plan",
                        tint = pal.teal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(pal.boneDark)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (plans.isEmpty()) {
                Text(
                    text = "No plans yet. Select a path above or tap + to create a custom plan.",
                    fontFamily = fontBody,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = pal.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    plans.forEach { plan ->
                        val overview = try {
                            PlannerEngine.getPlannerOverview(plan)
                        } catch (_: Exception) {
                            null
                        }
                        val pct = if (overview != null) {
                            (overview.completionRatio * 100).toInt().coerceIn(0, 100)
                        } else 0
                        val isActive = plan.id == activePlanId
                        var confirmDelete by remember(plan.id) { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = pal.cream),
                            border = BorderStroke(
                                1.5.dp,
                                if (isActive) pal.gold else pal.boneDark
                            ),
                            // Web parity (Planner.jsx 329): row body tap switches
                            // plan AND navigates to the active view.
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSwitchPlan(plan.id)
                                onViewActive()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.title.ifBlank { "Unnamed Plan" },
                                        fontFamily = fontBody,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.ink,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${plan.durationDays} days \u00B7 ${plan.unitType} \u00B7 $pct% complete",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.4.sp,
                                        color = pal.inkMuted
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(pal.boneDark)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(
                                                    if (pct == 0) 0.02f
                                                    else (pct / 100f).coerceIn(0.02f, 1f)
                                                )
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(pal.teal)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    // Web parity (Planner.jsx 339): mono uppercase
                                    // ACTIVE pill on gold wash.
                                    if (isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = pal.goldSoft
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 1.sp,
                                                color = pal.gold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (confirmDelete) {
                                        Text(
                                            text = "Tap again",
                                            fontFamily = fontBody,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = pal.teal,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    confirmDelete = false
                                                    onDeletePlan(plan.id)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { confirmDelete = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete plan",
                                                tint = pal.inkMuted,
                                                modifier = Modifier.size(16.dp)
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

        // (4) Plan Templates — web parity (Planner.jsx 357-392): heading +
        // "Curated paths" mono label + template grid opening the preview modal.
        if (templates.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Plan Templates",
                        fontFamily = fontUi,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.ink
                    )
                    Text(
                        text = "CURATED PATHS",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = pal.inkMuted
                    )
                }
                TemplatesGrid(
                    pal = pal,
                    templates = templates,
                    onPick = onPickTemplate,
                    chaptersReady = chaptersReady
                )
            }
        }

        // (5) Prophetic Wisdom footer (web parity: italic hadith + mono label)
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u201CThe best of deeds are those that are consistent, even if they are few.\u201D",
                fontFamily = fontBody,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = pal.inkMid,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PROPHETIC WISDOM",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = pal.inkMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
