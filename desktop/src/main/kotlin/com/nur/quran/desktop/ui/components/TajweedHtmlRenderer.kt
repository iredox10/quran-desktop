package com.nur.quran.desktop.ui.components

import com.nur.quran.shared.TajweedSegment

/**
 * Shape-safe tajweed renderer for desktop: converts plain verse text +
 * [TajweedSegment]s (produced by shared `TajweedProcessor.getWordTajweedSegments(
 * plainText, tajweedHtml, defaultColor)`) into an HTML fragment.
 *
 * The fragment is injected into `verse_template.html` (`{{VERSE_HTML}}`) and
 * shown in a browser engine (see [VerseWebView]). The browser shapes the
 * whole Arabic run first and only then applies per-span paint, so color
 * boundaries never break cursive joins — the same guarantee the Android
 * `TajweedAndroidText` gets from `TextView` + `ForegroundColorSpan`, and the
 * opposite of Compose `AnnotatedString` spans, which can split shaping runs.
 *
 * Rules mirrored from `buildTajweedSpannable` in `TajweedAndroidText.kt`:
 * - segments are sorted by start, clamped to the text, empty ones dropped;
 * - overlapping segments are trimmed (first segment wins, cursor advances);
 * - `ruleClass == "end"` (ayah-end marker) is forced to gold `#B8924A`.
 */
private val COLOR_HEX = Regex("^#[0-9a-fA-F]{3}([0-9a-fA-F]{3}([0-9a-fA-F]{2})?)?$")

/** Gold used for the ayah-end marker — mirrors Android's `isEndRule` branch. */
const val TAJWEED_END_MARKER_COLOR = "#B8924A"

/**
 * Converts [plainText] + [segments] to an HTML fragment wrapped in a
 * `<div dir="rtl" lang="ar">`.
 *
 * - Text outside segments is emitted verbatim (HTML-escaped).
 * - Each segment becomes `<span style="color:#hex">…</span>`; when the
 *   segment carries a `ruleClass`, a `data-rule` attribute is added so the
 *   host page / JS bridge can show rule explanations later.
 * - Segments with an invalid [TajweedSegment.colorHex] are emitted without a
 *   color style (fall back to surrounding text color) instead of producing
 *   broken CSS.
 */
fun segmentsToHtml(plainText: String, segments: List<TajweedSegment>): String {
    if (plainText.isEmpty()) return """<div dir="rtl" lang="ar"></div>"""
    if (segments.isEmpty()) return """<div dir="rtl" lang="ar">${escapeHtml(plainText)}</div>"""

    val sb = StringBuilder(plainText.length + segments.size * 48)
    sb.append("""<div dir="rtl" lang="ar">""")

    var cursor = 0
    for (seg in segments.sortedBy { it.start }) {
        val start = seg.start.coerceIn(0, plainText.length)
        val end = seg.end.coerceIn(start, plainText.length)
        if (end <= start || end <= cursor) continue
        val from = maxOf(start, cursor)
        if (from > cursor) sb.append(escapeHtml(plainText.substring(cursor, from)))

        val color = if (seg.ruleClass == "end") TAJWEED_END_MARKER_COLOR else seg.colorHex
        sb.append("<span")
        if (COLOR_HEX.matches(color)) {
            sb.append(""" style="color:""").append(color).append(";\"")
        }
        if (!seg.ruleClass.isNullOrBlank()) {
            sb.append(""" data-rule="""").append('"').append(escapeHtml(seg.ruleClass ?: "")).append('"')
        }
        sb.append('>')
        sb.append(escapeHtml(plainText.substring(from, end)))
        sb.append("</span>")
        cursor = end
    }
    if (cursor < plainText.length) sb.append(escapeHtml(plainText.substring(cursor)))

    sb.append("</div>")
    return sb.toString()
}

/** Minimal HTML escaper for verse text / attribute values. */
fun escapeHtml(raw: String): String {
    if (raw.isEmpty()) return raw
    val sb = StringBuilder(raw.length)
    for (c in raw) {
        when (c) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&#39;")
            else -> sb.append(c)
        }
    }
    return sb.toString()
}
