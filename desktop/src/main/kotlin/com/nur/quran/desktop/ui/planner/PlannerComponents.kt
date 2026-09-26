package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import com.nur.quran.shared.PlanTemplate

/**
 * Desktop planner building blocks, styled after the Android planner components
 * (teal/gold palette, cream cards with 14-18dp corners and bone-dark borders).
 */

/** Circular progress ring: bone-dark track, gold sweep, centered [label]. */
@Composable
fun PaceRing(pal: NurPalette, progress: Float, label: String, modifier: Modifier = Modifier) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val p = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width, size.height)
            drawArc(
                color = pal.boneDark,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (p > 0f) {
                drawArc(
                    color = pal.gold,
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontFamily = fontUi,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${(p * 100).toInt()}% COMPLETE",
                fontFamily = fontBody,
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                color = pal.inkMuted
            )
        }
    }
}

/** Section header: gold accent bar + ui semibold title. */
@Composable
fun SectionTitle(pal: NurPalette, text: String) {
    val fontUi = rememberUiFontFamily()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(pal.gold)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontFamily = fontUi,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.ink
        )
    }
}

/** Today's assignment card: cream surface, title + detail, teal CTA. */
@Composable
fun PlanTodayCard(
    pal: NurPalette,
    title: String,
    detail: String,
    ctaLabel: String,
    onOpen: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TODAY'S READING",
                fontFamily = fontBody,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = pal.teal
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = fontUi,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
            if (detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    fontFamily = fontBody,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = pal.inkMid
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ctaLabel,
                    fontFamily = fontUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = pal.white,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/** Two-column grid of template cards (label + description + duration pill). */
@Composable
fun TemplatesGrid(
    pal: NurPalette,
    templates: List<PlanTemplate>,
    onPick: (PlanTemplate) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        templates.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tmpl ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = pal.cream),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPick(tmpl) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(pal.goldSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tmpl.title.firstOrNull()?.uppercase() ?: "•",
                                        fontFamily = fontUi,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal.gold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tmpl.title,
                                    fontFamily = fontUi,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.ink
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tmpl.description,
                                fontFamily = fontBody,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = pal.inkMid,
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pal.tealSoft
                            ) {
                                Text(
                                    text = "${tmpl.durationDays} days",
                                    fontFamily = fontBody,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = pal.teal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Reflection editor: date header, multiline field, Save button. */
@Composable
fun JournalEditor(
    pal: NurPalette,
    dateStr: String,
    initial: String,
    onSave: (String) -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var text by remember(dateStr) { mutableStateOf(initial) }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = pal.cream),
        border = BorderStroke(1.5.dp, pal.boneDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = dateStr,
                fontFamily = fontUi,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "DAILY REFLECTION",
                fontFamily = fontBody,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = pal.inkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write your reflection…", fontFamily = fontBody, color = pal.inkMuted) },
                minLines = 5,
                maxLines = 10,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = pal.teal,
                    unfocusedBorderColor = pal.boneDark,
                    cursorColor = pal.teal,
                    focusedTextColor = pal.ink,
                    unfocusedTextColor = pal.ink
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSave(text) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pal.teal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Save Reflection",
                    fontFamily = fontUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = pal.white
                )
            }
        }
    }
}

/* ---------- Extras ---------- */

/** Single assignment row with day pill and completion tint. */
@Composable
fun AssignmentRow(
    pal: NurPalette,
    dayLabel: String,
    title: String,
    done: Boolean,
    onClick: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = pal.white,
        border = BorderStroke(1.dp, pal.boneDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (done) pal.tealSoft else pal.goldSoft
            ) {
                Text(
                    text = dayLabel,
                    fontFamily = fontBody,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (done) pal.teal else pal.gold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontFamily = fontUi,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (done) pal.inkMuted else pal.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Centered intention header (eyebrow + title + subtitle). */
@Composable
fun IntentionHero(pal: NurPalette, title: String, subtitle: String) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "THE FIRST STEP",
            fontFamily = fontBody,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = pal.teal
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontFamily = fontUi,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.ink
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontFamily = fontBody,
            fontSize = 13.5.sp,
            lineHeight = 22.sp,
            color = pal.inkMid
        )
    }
}

/** Two-column grid of small label/value stat boxes. */
@Composable
fun StatsCards(pal: NurPalette, stats: List<Pair<String, String>>) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pal.white,
                        border = BorderStroke(1.dp, pal.boneDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = label,
                                fontFamily = fontBody,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = pal.inkMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = value,
                                fontFamily = fontUi,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.ink
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
