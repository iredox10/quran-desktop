package com.nur.quran.desktop.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.nur.quran.shared.HtmlStripper

/**
 * Desktop equivalent of Android's `TranslationText` (SurahScreen.kt).
 *
 * Instead of `HtmlCompat.fromHtml`, the HTML is stripped with the
 * pure-Kotlin [HtmlStripper] and rendered as plain Compose [Text].
 */
@Composable
fun TranslationTextDesktop(
    html: String,
    fontScale: Float,
    fontFamily: FontFamily = FontFamily.Default,
    modifier: Modifier = Modifier
) {
    val plain = remember(html) { HtmlStripper.strip(html) }
    Text(
        text = plain,
        modifier = modifier,
        fontSize = (15 * fontScale).sp,
        lineHeight = (24 * fontScale).sp,
        fontFamily = fontFamily,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
    )
}
