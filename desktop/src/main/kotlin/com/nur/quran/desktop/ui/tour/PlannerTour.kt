package com.nur.quran.desktop.ui.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/** One page of the planner tour. */
data class TourStep(
    val title: String,
    val description: String
)

/**
 * Core planner tour (mirrors the web tour config intent).
 */
val PLANNER_TOUR_STEPS = listOf(
    TourStep(
        title = "Your Planner",
        description = "Create a reading plan from templates like Daily Juz or Ramadan Last 10, or build a custom range across pages, juz, hizb or surahs."
    ),
    TourStep(
        title = "Daily Assignments",
        description = "Each day shows a focused assignment with its pages and starting point. Open it to read, track progress, and mark the day complete."
    ),
    TourStep(
        title = "Adjust Pace",
        description = "Life happens — exclude rest days, extend the schedule, or set a custom pace so the plan fits your rhythm without losing your place."
    )
)

/** Advanced planner tour (mirrors the web tour config intent). */
val PLANNER_ADVANCED_STEPS = listOf(
    TourStep(
        title = "Smart Rebalancing",
        description = "Falling behind? Spread the remaining pages across the days left, or extend the plan — completed days stay untouched while unread pages are redistributed."
    ),
    TourStep(
        title = "Progress Visualization",
        description = "Weekly summaries, on-time rate, consistency streaks and catch-up counts show how steady your reading is and where to focus next."
    )
)

/**
 * Paged tour dialog: step counter (n/N), dots indicator, Back / Next / Done.
 */
@Composable
fun TourPagesDialog(
    pal: NurPalette,
    title: String,
    steps: List<TourStep>,
    onDone: () -> Unit
) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    var index by remember(steps) { mutableStateOf(0) }
    val safeSteps = steps.ifEmpty { listOf(TourStep("Tour", "Nothing to show yet.")) }
    val current = safeSteps[index.coerceIn(safeSteps.indices)]
    val isFirst = index <= 0
    val isLast = index >= safeSteps.lastIndex

    Dialog(onCloseRequest = onDone) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = pal.cream,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = title.uppercase(),
                    fontFamily = fontBody,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = pal.teal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = current.title,
                    fontFamily = fontUi,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = current.description,
                    fontFamily = fontBody,
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                    color = pal.inkMid
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    safeSteps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == index) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (i == index) pal.gold else pal.boneDark)
                        )
                        if (i < safeSteps.lastIndex) Spacer(modifier = Modifier.size(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Step ${index + 1} of ${safeSteps.size}",
                    fontFamily = fontBody,
                    fontSize = 11.sp,
                    color = pal.inkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (!isFirst) index-- },
                        enabled = !isFirst,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Back",
                            fontFamily = fontUi,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = if (isFirst) pal.inkMuted else pal.teal
                        )
                    }
                    Button(
                        onClick = { if (isLast) onDone() else index++ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.teal)
                    ) {
                        Text(
                            text = if (isLast) "Done" else "Next",
                            fontFamily = fontUi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = pal.white
                        )
                    }
                }
            }
        }
    }
}
