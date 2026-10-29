package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.nur.quran.shared.ReadingPlan

/**
 * PROGRESS-tab dialogs, mirroring web Planner.jsx ActiveView modals:
 * Adjust Pace (newDuration stepper, min = completedCount + 1, Apply calls
 * adjustPlannerPace) and Rebalance (Extend vs Spread cards).
 */

@Composable
fun AdjustPaceDialog(pal: NurPalette, plan: ReadingPlan, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val minDuration = maxOf(1, minOf(plan.durationDays, plan.completedDays.size + 1))
    var durationText by remember(plan.id) { mutableStateOf(plan.durationDays.toString()) }
    val parsed = durationText.toIntOrNull() ?: minDuration
    val newDuration = parsed.coerceIn(minDuration, 1000)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = pal.cream,
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text(
                    text = "Adjust Pace",
                    fontFamily = fontUi,
                    fontSize = 22.4.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Web parity (Planner.jsx 1336-1338).
                Text(
                    text = "Change how many days you want to complete your remaining plan in. " +
                        "This will re-calculate your daily assignments.",
                    fontFamily = fontBody,
                    fontSize = 15.2.sp,
                    lineHeight = 24.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "NEW TOTAL DAYS",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    color = pal.gold
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Web parity (1341-1345): single number input with a "DAYS"
                // suffix; the desktop stepper is intentionally gone.
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { value ->
                        durationText = value.filter { it.isDigit() }.take(4)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = fontUi,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink
                    ),
                    trailingIcon = {
                        Text(
                            text = "DAYS",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.8.sp,
                            color = pal.inkMuted
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pal.teal,
                        unfocusedBorderColor = pal.boneDark,
                        focusedContainerColor = pal.white,
                        unfocusedContainerColor = pal.white,
                        cursorColor = pal.teal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, pal.boneDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = pal.white,
                            contentColor = pal.inkMid
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = fontUi,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onApply(newDuration) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.gold),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontFamily = fontUi,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = pal.white,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
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
                            .clip(androidx.compose.foundation.shape.CircleShape)
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
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "You missed some pages from past assignments. " +
                        "How would you like to catch up?",
                    fontFamily = fontBody,
                    fontSize = 15.2.sp,
                    lineHeight = 24.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(24.dp))
                RebalanceOption(
                    pal = pal,
                    title = "Extend Plan",
                    body = "Keep your daily reading amount exactly the same, but add extra " +
                        "days to the end of your plan.",
                    onClick = { onPick("extend") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                RebalanceOption(
                    pal = pal,
                    title = "Spread Pages",
                    body = "Keep your original deadline, but evenly distribute the missed " +
                        "pages across your remaining days.",
                    onClick = { onPick("spread") }
                )
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
                            containerColor = androidx.compose.ui.graphics.Color(0xFFC0392B)
                        ),
                        contentPadding = PaddingValues(vertical = 13.6.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Delete",
                            fontFamily = fontBody,
                            fontSize = 14.4.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color.White
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
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RebalanceOption(pal: NurPalette, title: String, body: String, onClick: () -> Unit) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    // Web parity (Planner.jsx 1470-1488): rounded 20 card, 2dp bone border,
    // title + circular chevron, full explanatory body.
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = pal.white,
        border = BorderStroke(2.dp, pal.bone),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                    fontSize = 16.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(pal.boneDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = pal.teal,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = body,
                fontFamily = fontBody,
                fontSize = 13.6.sp,
                lineHeight = 18.sp,
                color = pal.inkMid
            )
        }
    }
}
