package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.input.dragToScroll
import com.nur.quran.desktop.ui.input.touchTarget
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlanTemplate
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import kotlin.math.ceil

data class PaceChoice(val id: String, val title: String, val durationDays: Int, val durationLabel: String)

private data class PaceUi(
    val choice: PaceChoice,
    val badge: String? = null
)

private val PACES_UI = listOf(
    PaceUi(PaceChoice("ramadan", "Ramadan Pace", 30, "30 DAYS")),
    PaceUi(PaceChoice("steady", "Steady Journey", 60, "60 DAYS"), badge = "MOST BALANCED"),
    PaceUi(PaceChoice("devotional", "Devotional Path", 365, "1 YEAR"))
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
private fun IntentionLogoMark() {
    // Web parity (Planner.jsx 232-234): real QURAN_NUR logo mark at 20% opacity.
    Image(
        painter = painterResource("drawable/ic_logo.png"),
        contentDescription = null,
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(18.dp)).alpha(0.2f)
    )
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
        // (1) Continue active plan button — web parity (Planner.jsx 221-229): centered banner, max-w-[480px]
        if (hasActivePlan) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onViewActive,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .touchTarget()
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // (2) Hero Header — web parity (Planner.jsx 231-240)
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IntentionLogoMark()
            Spacer(modifier = Modifier.height(20.dp)) // Web parity: mb-5 (20px)
            Text(
                text = "THE FIRST STEP",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                color = pal.teal
            )
            Spacer(modifier = Modifier.height(8.dp)) // Web parity: mb-2 (8px)
            Text(
                text = "Set Your Intention",
                fontFamily = fontUi,
                fontSize = 32.sp, // Web parity: clamp(1.7rem, 6vw, 2.8rem) ~ 32sp desktop
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                lineHeight = 35.sp,
                color = pal.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp)) // Web parity: mb-[0.6rem] (10px)
            Text(
                text = "Choose a pace that resonates with your soul. Whether intensive or slow, the journey of the Quran is a dialogue of devotion.",
                fontFamily = fontBody,
                fontSize = 14.5.sp,
                lineHeight = 24.sp,
                color = pal.inkMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp).padding(horizontal = 8.dp) // Web parity: md:max-w-[560px]
            )
        }

        // (3) Pace cards — web parity (Planner.jsx 242-299): 3-card grid on desktop (max-w-[860px]),
        // horizontally scrollable on mobile/tablet viewports with touch drag-to-scroll.
        var selectedPace by remember { mutableStateOf("steady") }
        var beginError by remember { mutableStateOf<String?>(null) }

        val handleBegin: (PaceChoice) -> Unit = { pace ->
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
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 768.dp
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    PACES_UI.forEach { paceUi ->
                        PaceCardItem(
                            pal = pal,
                            fontUi = fontUi,
                            fontBody = fontBody,
                            paceUi = paceUi,
                            isSelected = selectedPace == paceUi.choice.id,
                            onSelect = {
                                selectedPace = paceUi.choice.id
                                beginError = null
                            },
                            onBegin = { handleBegin(paceUi.choice) },
                            modifier = Modifier.weight(1f).widthIn(max = 260.dp)
                        )
                    }
                }
            } else {
                val paceScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(paceScroll)
                        .dragToScroll(paceScroll, Orientation.Horizontal)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PACES_UI.forEach { paceUi ->
                        PaceCardItem(
                            pal = pal,
                            fontUi = fontUi,
                            fontBody = fontBody,
                            paceUi = paceUi,
                            isSelected = selectedPace == paceUi.choice.id,
                            onSelect = {
                                selectedPace = paceUi.choice.id
                                beginError = null
                            },
                            onBegin = { handleBegin(paceUi.choice) },
                            modifier = Modifier.width(260.dp)
                        )
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

        // (4) My Plans list — web parity (Planner.jsx 302-355): centered max-w-[500px],
        // header with title, Archives link, and circular + action button.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Plans",
                        fontFamily = fontUi,
                        fontSize = 18.5.sp,
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
                                .touchTarget()
                                .clickable { onOpenArchives() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .touchTarget()
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
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        color = pal.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
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
                            var pendingDelete by remember(plan.id) { mutableStateOf(false) }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = pal.cream),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isActive) pal.gold else pal.boneDark
                                ),
                                // Web parity (Planner.jsx 329): row body tap switches
                                // plan AND navigates to the active view.
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .touchTarget()
                                    .clickable {
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
                                            text = "${plan.durationDays} days · ${plan.unitType} · $pct% complete",
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
                                        // Web parity (Planner.jsx 339): mono "Active" pill on gold wash.
                                        if (isActive) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = pal.goldSoft
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    letterSpacing = 1.sp,
                                                    color = pal.gold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        // Web parity (Planner.jsx 340-346 + 1601-1614):
                                        // trash icon opens the "Delete this plan?" modal.
                                        IconButton(
                                            onClick = { pendingDelete = true },
                                            modifier = Modifier.size(32.dp).touchTarget()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete plan",
                                                tint = pal.inkMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Web parity: delete confirm lives outside the row card.
                            if (pendingDelete) {
                                DeletePlanDialog(
                                    pal = pal,
                                    onDismiss = { pendingDelete = false },
                                    onConfirm = {
                                        pendingDelete = false
                                        onDeletePlan(plan.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // (5) Plan Templates — web parity (Planner.jsx 357-392): heading +
        // "Curated paths" mono label + responsive grid opening the preview modal.
        if (templates.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 860.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
        }

        // (6) Prophetic Wisdom footer (web parity: italic hadith + mono label)
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u201CThe best of deeds are those that are consistent, even if they are few.\u201D",
                fontFamily = fontBody,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 14.5.sp,
                lineHeight = 23.sp,
                color = pal.inkMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
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

/**
 * Single Pace Card — web parity (Planner.jsx lines 244-297):
 * Cream card with 14dp corners, bone-dark or gold border, ring with daily pages,
 * title, "X pages per day", icon with "Y pages per prayer", and Begin Journey / Select Path button.
 */
@Composable
private fun PaceCardItem(
    pal: NurPalette,
    fontUi: androidx.compose.ui.text.font.FontFamily,
    fontBody: androidx.compose.ui.text.font.FontFamily,
    paceUi: PaceUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pace = paceUi.choice
    val stats = remember(pace.durationDays) {
        PlannerEngine.getPaceStats(pace.durationDays, emptyList(), null, 604)
    }
    val dailyPages = stats.dailyPages
    val perPrayer = stats.perPrayer

    Box(modifier = modifier) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = pal.cream),
            border = BorderStroke(
                1.5.dp,
                if (isSelected) pal.gold else pal.boneDark
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 2.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .touchTarget()
                .clickable { onSelect() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaceCardRing(
                    pal = pal,
                    durationDays = pace.durationDays,
                    dailyPages = dailyPages,
                    selected = isSelected
                )
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
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = paceUi.icon,
                        contentDescription = null,
                        tint = pal.inkMid,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$perPrayer pages per prayer",
                        fontFamily = fontBody,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 13.sp,
                        color = pal.inkMid
                    )
                }

                if (isSelected) {
                    Button(
                        onClick = onBegin,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .touchTarget()
                    ) {
                        Text(
                            text = "Begin Journey",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.15.sp,
                            color = pal.white
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onSelect,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .touchTarget()
                    ) {
                        Text(
                            text = "Select Path",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.15.sp,
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
                    .offset(y = (-13).dp)
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
