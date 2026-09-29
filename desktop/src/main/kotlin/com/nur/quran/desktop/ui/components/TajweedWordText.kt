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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.nur.quran.shared.TajweedProcessor
import com.nur.quran.shared.TajweedSegment

/**
 * Tajweed-ON verse renderer — pure Compose, no WebView, shape-safe by
 * construction.
 *
 * Compose `AnnotatedString` spans can split Arabic shaping runs (same failure
 * mode as Android, which is why Android paints tajweed with
 * TextView+ForegroundColorSpan: shape-then-paint). Coloring an ENTIRE word
 * with ONE [SpanStyle] never breaks joins (cursive joins don't cross spaces),
 * so per-word single-style coloring is always safe. Intra-word multi-color
 * spans are FORBIDDEN here — each word gets at most one [SpanStyle].
 *
 * Majority-color rule: a word's color is the [TajweedSegment.colorHex] with
 * the largest total character overlap onto the word's range; ties go to the
 * earliest segment in list order. Words with no colored overlap (or only the
 * default-ink sentinel) get NO override — they inherit the base style.
 * Segments with `ruleClass == "end"` are forced to the theme gold
 * [markerGold], mirroring the existing HTML renderer.
 *
 * Segment alignment: [TajweedProcessor.getWordTajweedSegments] aligns to the
 * verse text WITHOUT the end-of-ayah marker, so [plainText] (the display
 * string WITH marker, from [verseDisplayArabic]) is split at its last space:
 * the head is passed for segmentation, the tail word is painted gold.
 */
@Composable
fun TajweedWordText(
    plainText: String,
    tajweedHtml: String?,
    fontScale: Float = 1f,
    lineHeightMultiplier: Float = 1f,
    fontFamily: FontFamily = FontFamily.Default,
    onWordClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    markerGold: Color = TajweedWordGold,
) {
    val annotated = remember(plainText, tajweedHtml, markerGold) {
        buildTajweedWordAnnotatedString(plainText, tajweedHtml, markerGold)
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
                            .getStringAnnotations(WORD_INDEX_TAG_TAJWEED, textOffset, textOffset)
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

/**
 * Builds the word-level tajweed [AnnotatedString]: at most one [SpanStyle]
 * per whitespace-delimited word, plus [WORD_INDEX_TAG_TAJWEED] tap
 * annotations matching [PlainVerseText]'s convention.
 */
internal fun buildTajweedWordAnnotatedString(
    displayText: String,
    tajweedHtml: String?,
    markerGold: Color = TajweedWordGold,
): AnnotatedString {
    if (displayText.isEmpty()) return AnnotatedString("")
    // verseDisplayArabic appends " $digits" (or " \u06DD$digits") after the
    // cleaned body; segments align to the body only.
    val markerStart = displayText.lastIndexOf(' ')
    val bodyEnd = if (markerStart >= 0) markerStart else displayText.length
    val body = displayText.substring(0, bodyEnd)
    val segments: List<TajweedSegment> =
        if (!tajweedHtml.isNullOrEmpty() && body.isNotEmpty()) {
            TajweedProcessor.getWordTajweedSegments(body, tajweedHtml, WORD_DEFAULT_SENTINEL)
        } else {
            emptyList()
        }
    return buildAnnotatedString {
        append(displayText)
        var i = 0
        var wordIndex = 0
        while (i < displayText.length) {
            if (displayText[i] == ' ') {
                i++
                continue
            }
            var j = i
            while (j < displayText.length && displayText[j] != ' ') j++
            val ws = i
            val we = j
            if (ws >= bodyEnd) {
                // End-of-ayah marker word — always gold.
                addStyle(SpanStyle(color = markerGold), ws, we)
            } else {
                // Words never straddle bodyEnd (it sits on a space); clamp
                // defensively for the segment lookup only.
                val cws = ws.coerceIn(0, body.length)
                val cwe = we.coerceIn(cws, body.length)
                majorityColorForWord(cws, cwe, segments, markerGold)?.let { color ->
                    addStyle(SpanStyle(color = color), ws, we)
                }
            }
            addStringAnnotation(WORD_INDEX_TAG_TAJWEED, wordIndex.toString(), ws, we)
            wordIndex++
            i = j
        }
    }
}

/**
 * Majority color for the word range `[start, end)` (offsets into the
 * marker-free body that [segments] align to): sums per-[TajweedSegment]
 * character overlap grouped by color hex and returns the color with the
 * largest total overlap. Ties resolve to the earliest segment in list order.
 * Returns null when nothing colored overlaps (default-ink words stay on the
 * base style) or when the winning hex doesn't parse.
 */
private fun majorityColorForWord(
    start: Int,
    end: Int,
    segments: List<TajweedSegment>,
    markerGold: Color,
): Color? {
    if (start >= end || segments.isEmpty()) return null
    val coverage = mutableMapOf<String, Int>()
    val firstOrder = mutableMapOf<String, Int>()
    for ((order, seg) in segments.withIndex()) {
        val overlap = minOf(seg.end, end) - maxOf(seg.start, start)
        if (overlap <= 0) continue
        val hex = if (seg.ruleClass == "end") WORD_END_SEGMENT_KEY else seg.colorHex
        if (hex == WORD_DEFAULT_SENTINEL) continue
        coverage[hex] = (coverage[hex] ?: 0) + overlap
        if (!firstOrder.containsKey(hex)) firstOrder[hex] = order
    }
    if (coverage.isEmpty()) return null
    val best = coverage.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { firstOrder[it.key] ?: Int.MAX_VALUE })
        .first()
        .key
    return if (best == WORD_END_SEGMENT_KEY) markerGold else parseTajweedHexColor(best)
}

private fun parseTajweedHexColor(hex: String): Color? {
    return try {
        var h = hex.trim().removePrefix("#")
        if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
        when (h.length) {
            6 -> Color(("FF$h").toLong(16))
            8 -> Color(h.toLong(16))
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Sentinel passed as `defaultColor` to [TajweedProcessor.getWordTajweedSegments].
 * Uncolored chars (and unknown rule classes) come back tagged with this, so the
 * majority vote can skip them and leave default-ink words unstyled. Chosen to
 * never collide with a real `#…` tajweed hex.
 */
private const val WORD_INDEX_TAG_TAJWEED = "WORD_INDEX"

private const val WORD_DEFAULT_SENTINEL = "DEFAULT_INK"

/**
 * Coverage key for `ruleClass == "end"` segments — never a real hex, so the
 * winning entry can be swapped for the theme's [markerGold] at resolve time.
 */
private const val WORD_END_SEGMENT_KEY = "\u0001end"

private val TajweedWordGold = Color(0xFFB8924A)
