package com.nur.quran.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.nur.quran.desktop.ui.components.DesktopFonts

/**
 * Quran Nur palette — exact hex values from the Android app
 * (`SurahScreen.kt`, themselves matching the web `index.css` variables).
 */
object NurColors {
    // Light
    const val WHITE = 0xFFFAFAF5
    const val CREAM = 0xFFFAF7F0
    const val BONE = 0xFFEDE8DA
    const val BONE_DARK = 0xFFDDD7C7
    const val INK = 0xFF2B3F3C
    const val INK_MID = 0xFF4D5F5C
    const val INK_MUTED = 0xFF8E9B97
    const val GOLD = 0xFFB8924A
    const val GOLD_SOFT = 0x2EB8924A
    const val GOLD_LIGHT = 0x26C6A87C
    const val TEAL = 0xFF2E4F4A
    const val TEAL_MID = 0xFF3D6560
    const val TEAL_SOFT = 0x142E4F4A
    const val GREEN = 0xFF10B981
    // Dark
    const val D_WHITE = 0xFF1A1A18
    const val D_CREAM = 0xFF2D2D2A
    const val D_BONE = 0xFF3A3A36
    const val D_BONE_DARK = 0xFF4A4A45
    const val D_INK = 0xFFEFECE4
    const val D_INK_MID = 0xFFB0ABA5
    const val D_INK_MUTED = 0xFF5C5855
    const val D_GOLD = 0xFFC6A87C
    const val D_GOLD_SOFT = 0x1AC6A87C
    const val D_GOLD_LIGHT = 0x1AC6A87C
    const val D_TEAL = 0xFF4A7A72
    const val D_TEAL_SOFT = 0x262E4F4A
}

private val LightScheme = lightColorScheme(
    primary = Color(NurColors.TEAL),
    onPrimary = Color.White,
    primaryContainer = Color(NurColors.TEAL_SOFT),
    secondary = Color(NurColors.GOLD),
    background = Color(NurColors.WHITE),
    surface = Color(NurColors.CREAM),
    onBackground = Color(NurColors.INK),
    onSurface = Color(NurColors.INK),
    surfaceVariant = Color(NurColors.BONE),
    outline = Color(NurColors.BONE_DARK)
)

private val DarkScheme = darkColorScheme(
    primary = Color(NurColors.D_TEAL),
    onPrimary = Color.White,
    primaryContainer = Color(NurColors.D_TEAL_SOFT),
    secondary = Color(NurColors.D_GOLD),
    background = Color(NurColors.D_WHITE),
    surface = Color(NurColors.D_CREAM),
    onBackground = Color(NurColors.D_INK),
    onSurface = Color(NurColors.D_INK),
    surfaceVariant = Color(NurColors.D_BONE),
    outline = Color(NurColors.D_BONE_DARK)
)

/** App theme wrapping Material3 with the Nur palette. */
@Composable
fun NurTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content
    )
}

/** Direct palette access mirroring the Android `hTeal`, `hGold`… getters. */
class NurPalette(val dark: Boolean) {
    val white: Color get() = Color(if (dark) NurColors.D_WHITE else NurColors.WHITE)
    val cream: Color get() = Color(if (dark) NurColors.D_CREAM else NurColors.CREAM)
    val bone: Color get() = Color(if (dark) NurColors.D_BONE else NurColors.BONE)
    val boneDark: Color get() = Color(if (dark) NurColors.D_BONE_DARK else NurColors.BONE_DARK)
    val ink: Color get() = Color(if (dark) NurColors.D_INK else NurColors.INK)
    val inkMid: Color get() = Color(if (dark) NurColors.D_INK_MID else NurColors.INK_MID)
    val inkMuted: Color get() = Color(if (dark) NurColors.D_INK_MUTED else NurColors.INK_MUTED)
    val gold: Color get() = Color(if (dark) NurColors.D_GOLD else NurColors.GOLD)
    val goldSoft: Color get() = Color(if (dark) NurColors.D_GOLD_SOFT else NurColors.GOLD_SOFT)
    val goldLight: Color get() = Color(if (dark) NurColors.D_GOLD_LIGHT else NurColors.GOLD_LIGHT)
    val teal: Color get() = Color(if (dark) NurColors.D_TEAL else NurColors.TEAL)
    val tealMid: Color get() = Color(NurColors.TEAL_MID)
    val tealSoft: Color get() = Color(if (dark) NurColors.D_TEAL_SOFT else NurColors.TEAL_SOFT)
    val green: Color get() = Color(NurColors.GREEN)
    val surface: Color get() = Color(if (dark) 0xFF2D2D2A else 0xFFEFECE4)
    val border: Color get() = boneDark
}

/** UI display font (Cormorant Garamond, like Android `fontFamilyUi`). */
@Composable
fun rememberUiFontFamily(): FontFamily = rememberBundledFontFamily(
    "fonts/cormorant_garamond_regular.ttf",
    "fonts/cormorant_garamond_bold.ttf"
)

/** Body font (Piazzolla, like Android `fontFamilyBody`). */
@Composable
fun rememberBodyFontFamily(): FontFamily = rememberBundledFontFamily(
    "fonts/piazzolla_regular.ttf",
    "fonts/piazzolla_bold.ttf"
)

/** Arabic font resolved through [DesktopFonts] (defaults to KFGQPC Hafs). */
@Composable
fun rememberArabicFontFamily(name: String = DesktopFonts.KFGQPC_HAFS): FontFamily =
    DesktopFonts.rememberFontFamily(name)

@Composable
private fun rememberBundledFontFamily(regularPath: String, boldPath: String): FontFamily {
    return androidx.compose.runtime.remember(regularPath, boldPath) {
        try {
            val regular = DesktopFonts::class.java.getResourceAsStream("/$regularPath")?.readBytes()
            val bold = DesktopFonts::class.java.getResourceAsStream("/$boldPath")?.readBytes()
            if (regular == null) {
                FontFamily.Default
            } else {
                val fonts = mutableListOf(
                    Font(identity = regularPath, data = regular, weight = FontWeight.Normal)
                )
                if (bold != null) {
                    fonts += Font(identity = boldPath, data = bold, weight = FontWeight.Bold)
                }
                FontFamily(fonts)
            }
        } catch (_: Exception) {
            FontFamily.Default
        }
    }
}

/** Time-of-day greeting, mirroring Android `getGreeting()`. */
fun getGreeting(): Pair<String, String> {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h in 5..11 -> Pair("Assalamu Alaikum", "May your morning be blessed")
        h in 12..16 -> Pair("Assalamu Alaikum", "Wishing you a productive afternoon")
        h in 17..20 -> Pair("Assalamu Alaikum", "May your evening be peaceful")
        else -> Pair("Assalamu Alaikum", "May your night be filled with barakah")
    }
}

/** Mirrors the web/Android `timeAgo()`. */
fun timeAgo(timestamp: Long): String {
    val mins = (System.currentTimeMillis() - timestamp) / 60000
    if (mins < 60) return "${mins}m ago"
    val hrs = mins / 60
    if (hrs < 24) return "${hrs}h ago"
    return "${hrs / 24}d ago"
}
