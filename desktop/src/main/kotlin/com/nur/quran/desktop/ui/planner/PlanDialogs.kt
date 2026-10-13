package com.nur.quran.desktop.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    var newDuration by remember(plan.id) { mutableStateOf(plan.durationDays) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = pal.cream, border = BorderStroke(1.dp, pal.boneDark), modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Adjust Pace", fontFamily = fontUi, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Change how many days you want to complete your remaining plan in. This will re-calculate your daily assignments.", fontFamily = fontBody, fontSize = 13.sp, lineHeight = 20.sp, color = pal.inkMid)
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "NEW TOTAL DAYS", fontFamily = fontBody, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = pal.gold)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = pal.white, border = BorderStroke(2.dp, pal.boneDark), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { newDuration = maxOf(minDuration, newDuration - 1) }, shape = RoundedCornerShape(10.dp), enabled = newDuration > minDuration) { Text(text = "\u2212", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.ink) }
                        Text(text = "$newDuration", fontFamily = fontUi, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                        OutlinedButton(onClick = { newDuration = minOf(1000, newDuration + 1) }, shape = RoundedCornerShape(10.dp)) { Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.ink) }
                    }
                }
                Text(text = "DAYS  (min $minDuration)", fontFamily = fontBody, fontSize = 10.sp, color = pal.inkMuted)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, pal.boneDark), modifier = Modifier.weight(1f).height(46.dp)) { Text(text = "Cancel", fontFamily = fontUi, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = pal.inkMid) }
                    Button(onClick = { onApply(newDuration) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = pal.gold, contentColor = androidx.compose.ui.graphics.Color.White), modifier = Modifier.weight(1f).height(46.dp)) { Text(text = "Apply", fontFamily = fontUi, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
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
        Surface(shape = RoundedCornerShape(20.dp), color = pal.cream, border = BorderStroke(1.dp, pal.boneDark), modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Rebalance Plan", fontFamily = fontUi, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "You missed some pages from past assignments. How would you like to catch up?", fontFamily = fontBody, fontSize = 13.sp, lineHeight = 20.sp, color = pal.inkMid)
                Spacer(modifier = Modifier.height(16.dp))
                RebalanceOption(pal, title = "Extend Plan", body = "Keep your daily reading amount exactly the same, but add extra days to the end of your plan.", onClick = { onPick("extend") })
                Spacer(modifier = Modifier.height(12.dp))
                RebalanceOption(pal, title = "Spread Pages", body = "Keep your original deadline, but evenly distribute the missed pages across your remaining days.", onClick = { onPick("spread") })
            }
        }
    }
}

@Composable
fun DeletePlanDialog(
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
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = "Delete this plan?",
                    fontFamily = fontUi,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "All progress for \"$planTitle\" will be permanently lost. This cannot be undone.",
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
                        Text(
                            text = "Cancel",
                            fontFamily = fontBody,
                            fontSize = 14.sp,
                            color = pal.inkMid
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFC0392B)
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Delete",
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
    Surface(shape = RoundedCornerShape(16.dp), color = pal.white, border = BorderStroke(2.dp, pal.bone), modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontFamily = fontUi, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                Text(text = "\u276F", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.teal)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, fontFamily = fontBody, fontSize = 12.sp, lineHeight = 18.sp, color = pal.inkMid)
        }
    }
}
