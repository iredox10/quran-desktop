package com.nur.quran.desktop.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.OnboardingStore
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily

/**
 * Desktop Onboarding Progress card mirroring web `OnboardingProgress.jsx`:
 * - "Welcome to Quran Nur" headline and step counter
 * - Multi-stop gradient progress bar (teal to gold)
 * - 5 onboarding tour items:
 *   1. Home Page 🏠
 *   2. Reading & Audio 📖
 *   3. Hifdh Mode 🧠
 *   4. Study Planner 📅
 *   5. Library 📚
 * - Persistent dismissal X button backed by [OnboardingStore]
 */
@Composable
fun HomeOnboardingProgressCard(
    pal: NurPalette,
    onOpenHome: () -> Unit = {},
    onOpenSurah: (chapterId: Int, verseKey: String?) -> Unit = { _, _ -> },
    onOpenMemorize: (chapterId: Int) -> Unit = {},
    onOpenPlanner: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDismissed by remember { mutableStateOf(OnboardingStore.isDismissed()) }
    var completedTours by remember { mutableStateOf(OnboardingStore.completedTours()) }

    val allTours = OnboardingStore.ALL_TOURS
    val completedCount = allTours.count { completedTours.contains(it.id) }
    val isFullyCompleted = completedCount == allTours.size

    if (isDismissed || (isFullyCompleted && completedTours.isNotEmpty())) {
        return
    }

    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val progressPct = (completedCount.toFloat() / allTours.size.toFloat()).coerceIn(0f, 1f)

    AnimatedVisibility(
        visible = !isDismissed,
        enter = fadeIn(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = pal.cream,
            border = BorderStroke(1.5.dp, pal.boneDark),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                // Background watermark logo
                runCatching {
                    Image(
                        painter = painterResource("drawable/ic_logo.png"),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 25.dp, y = (-25).dp)
                            .alpha(0.04f),
                        contentScale = ContentScale.Fit
                    )
                }

                // Dismiss X button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(pal.white)
                        .border(1.dp, pal.bone, CircleShape)
                        .clickable {
                            OnboardingStore.setDismissed(true)
                            isDismissed = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss onboarding",
                        tint = pal.inkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Text(
                        text = "Welcome to Quran Nur",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        fontFamily = fontUi
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$completedCount of ${allTours.size} steps completed",
                        fontSize = 13.sp,
                        color = pal.inkMid,
                        fontFamily = fontBody
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(pal.boneDark)
                    ) {
                        if (progressPct > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressPct)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(pal.teal, Color(0xFFB8924A))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5 Tour Rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allTours.forEach { tour ->
                            val isCompleted = completedTours.contains(tour.id)

                            val clickAction: () -> Unit = {
                                OnboardingStore.completeTour(tour.id)
                                completedTours = OnboardingStore.completedTours()
                                when (tour.id) {
                                    "home-tour" -> onOpenHome()
                                    "surah-tour" -> onOpenSurah(1, null)
                                    "memorization-tour" -> onOpenMemorize(1)
                                    "planner-tour" -> onOpenPlanner()
                                    "library-tour" -> onOpenLibrary()
                                    else -> {}
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = clickAction)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Emoji Icon Box
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pal.white)
                                        .border(1.dp, pal.bone, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tour.icon,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Label
                                Text(
                                    text = tour.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (isCompleted) pal.inkMuted.copy(alpha = 0.6f) else pal.ink,
                                    fontFamily = fontUi,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Status Indicator
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color(0xFFB8924A),
                                        modifier = Modifier.size(18.dp).alpha(0.85f)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open",
                                        tint = pal.inkMuted.copy(alpha = 0.4f),
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
