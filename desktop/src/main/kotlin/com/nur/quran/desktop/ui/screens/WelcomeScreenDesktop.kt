package com.nur.quran.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import com.nur.quran.desktop.ui.theme.rememberUiFontFamily
import java.util.prefs.Preferences

private const val WELCOME_PREFS_NODE = "welcome_prefs"
private const val KEY_SEEN = "seen"

private data class WelcomeStep(val key: String, val emoji: String, val label: String)

private val WELCOME_STEPS = listOf(
    WelcomeStep("step_read", "📖", "Open a Surah"),
    WelcomeStep("step_memorize", "🧠", "Review memorization"),
    WelcomeStep("step_planner", "📅", "Create a reading plan"),
    WelcomeStep("step_font", "🎨", "Pick an Arabic font")
)

/**
 * First-run welcome page mirroring Android `OnboardingProgressCard`
 * (HomeScreen.kt ~1344-1485) + `ui/components/SplashScreen.kt` branding:
 * logo, title, checklist with persisted manual check states, teal→gold
 * progress bar and a Get started button.
 *
 * The coordinator routes here only when unseen (prefs node
 * "welcome_prefs", key "seen").
 */
@Composable
fun WelcomeScreenDesktop(pal: NurPalette, onDone: () -> Unit = {}) {
    val fontUi = rememberUiFontFamily()
    val fontBody = rememberBodyFontFamily()
    val prefs = remember { Preferences.userRoot().node(WELCOME_PREFS_NODE) }

    var stepRead by remember { mutableStateOf(prefs.getBoolean("step_read", false)) }
    var stepMemorize by remember { mutableStateOf(prefs.getBoolean("step_memorize", false)) }
    var stepPlanner by remember { mutableStateOf(prefs.getBoolean("step_planner", false)) }
    var stepFont by remember { mutableStateOf(prefs.getBoolean("step_font", false)) }

    fun isChecked(key: String): Boolean = when (key) {
        "step_read" -> stepRead
        "step_memorize" -> stepMemorize
        "step_planner" -> stepPlanner
        "step_font" -> stepFont
        else -> false
    }

    fun toggle(key: String) {
        when (key) {
            "step_read" -> {
                stepRead = !stepRead
                runCatching { prefs.putBoolean(key, stepRead) }
            }
            "step_memorize" -> {
                stepMemorize = !stepMemorize
                runCatching { prefs.putBoolean(key, stepMemorize) }
            }
            "step_planner" -> {
                stepPlanner = !stepPlanner
                runCatching { prefs.putBoolean(key, stepPlanner) }
            }
            "step_font" -> {
                stepFont = !stepFont
                runCatching { prefs.putBoolean(key, stepFont) }
            }
        }
    }

    val completed = WELCOME_STEPS.count { isChecked(it.key) }
    val allDone = completed == WELCOME_STEPS.size

    Box(
        modifier = Modifier.fillMaxSize().background(pal.white),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource("drawable/ic_logo.png"),
                contentDescription = "Quran Nur logo",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
            )
            Text(
                text = "Welcome to Quran Nur",
                fontFamily = fontUi,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = pal.ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your companion for reading, memorizing, and studying the Quran.",
                fontFamily = fontBody,
                fontSize = 14.sp,
                color = pal.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = pal.cream),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, pal.boneDark)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$completed of ${WELCOME_STEPS.size} steps completed",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = pal.inkMuted
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(pal.surface)
                    ) {
                        if (completed > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = completed / WELCOME_STEPS.size.toFloat())
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(pal.teal, pal.gold)
                                        )
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    WELCOME_STEPS.forEach { step ->
                        WelcomeChecklistRow(
                            pal = pal,
                            fontBody = fontBody,
                            emoji = step.emoji,
                            label = step.label,
                            checked = isChecked(step.key),
                            onToggle = { toggle(step.key) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(pal.teal)
                    .clickable {
                        runCatching { prefs.putBoolean(KEY_SEEN, true) }
                        onDone()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (allDone) "Begin your journey ✦" else "Get started",
                    fontFamily = fontUi,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun WelcomeChecklistRow(
    pal: NurPalette,
    fontBody: FontFamily,
    emoji: String,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) pal.tealSoft else pal.surface)
                .border(1.dp, if (checked) pal.teal else pal.boneDark, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontFamily = fontBody,
            fontSize = 15.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.SemiBold,
            color = if (checked) pal.ink else pal.inkMid,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) pal.teal else Color.Transparent)
                .border(1.5.dp, if (checked) pal.teal else pal.boneDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
