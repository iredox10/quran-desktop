package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

/**
 * Tajweed-OFF verse renderer — fully portable, no AndroidView.
 *
 * Mirrors the VerseItem OFF branch in the Android app
 * (SurahScreen.kt: buildAnnotatedString + end-word gold SpanStyle + ClickableText
 * with fontSize 26*scale, lineHeight 52*scale, TextAlign.Right, RTL
 * CompositionLocalProvider).
 *
 * Contract for [wordRanges]: each entry is `range to wordIndex`, where `range`
 * follows the [buildVerseDisplayText] convention from the Android app —
 * `start..sb.length`, i.e. [IntRange.first] is the inclusive start and
 * [IntRange.last] is the EXCLUSIVE end (passed straight to
 * `addStyle`/`addStringAnnotation`, exactly like the Android OFF branch does).
 * Bounds are clamped defensively.
 *
 * End-of-ayah detection: the Android branch checks
 * `words[wordIndex].charTypeName == "end"`, but this portable signature carries
 * no word metadata, so a range is treated as the end marker when its substring
 * contains U+06DD (ARABIC END OF AYAH "۝"). The plain-text pipeline
 * canonicalizes every verse to a single U+06DD frame, so this is reliable.
 *
 * Click handling uses [pointerInput] + [detectTapGestures] +
 * [TextLayoutResult.getOffsetForPosition], which is available on Compose
 * Multiplatform desktop (no ClickableText, no AndroidView needed).
 */
@Composable
fun PlainVerseText(
    plainText: String,
    wordRanges: List<Pair<IntRange, Int>>,
    fontScale: Float = 1f,
    lineHeightMultiplier: Float = 1f,
    fontFamily: FontFamily = FontFamily.Default,
    onWordClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val annotated = remember(plainText, wordRanges) {
        buildAnnotatedString {
            append(plainText)
            for ((range, wordIndex) in wordRanges) {
                val start = range.first.coerceIn(0, plainText.length)
                // NOTE: range.last is the EXCLUSIVE end per buildVerseDisplayText
                // (start..sb.length), matching the Android OFF branch usage.
                val end = range.last.coerceIn(start, plainText.length)
                if (end <= start) continue
                val slice = plainText.substring(start, end)
                if (slice.contains(END_OF_AYAH_CHAR)) {
                    addStyle(SpanStyle(color = EndMarkerGold), start, end)
                }
                addStringAnnotation(WORD_INDEX_TAG, wordIndex.toString(), start, end)
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BasicText(
            text = annotated,
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(annotated, onWordClick) {
                    detectTapGestures { tapOffset ->
                        val layout = layoutResult ?: return@detectTapGestures
                        val textOffset = layout.getOffsetForPosition(tapOffset)
                        annotated
                            .getStringAnnotations(WORD_INDEX_TAG, textOffset, textOffset)
                            .firstOrNull()
                            ?.item
                            ?.toIntOrNull()
                            ?.let(onWordClick)
                    }
                },
            style = TextStyle(
                fontSize = (26 * fontScale).sp,
                lineHeight = (52 * fontScale * lineHeightMultiplier).sp,
                textAlign = TextAlign.Right,
                fontFamily = fontFamily,
            ),
            onTextLayout = { layoutResult = it },
        )
    }
}

private const val WORD_INDEX_TAG = "WORD_INDEX"

/** U+06DD ARABIC END OF AYAH ("۝"). */
private const val END_OF_AYAH_CHAR = '۝'

/** Light-theme hGold (#B8924A); dark theme uses #C6A87C (caller-themed later). */
private val EndMarkerGold = Color(0xFFB8924A)
