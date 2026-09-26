package com.nur.quran.shared

/**
 * Pure-Kotlin replacement for `HtmlCompat.fromHtml(...).toString()` used for
 * translation / tafsir / footnote bodies.
 *
 * No Android (or JVM) dependencies — safe for commonMain / desktop.
 */
object HtmlStripper {

    private val tagRegex = Regex("<[^>]*>")
    private val blockBreakRegex = Regex("(?i)<\\s*(br|/p|/div|/li|/tr)\\b[^>]*>")
    private val whitespaceRegex = Regex("\\s+")
    private val decimalEntityRegex = Regex("&#(\\d+);")
    private val hexEntityRegex = Regex("&#x([0-9a-fA-F]+);")

    /** Matches Android's `<sup foot_note="N">N</sup>` footnote markers. */
    private val footnoteSupRegex =
        Regex("<sup[^>]*foot_note=[\"']?(\\d+)[\"']?[^>]*>\\s*(\\d+)\\s*</sup>")

    /**
     * Strips HTML tags, decodes common entities and collapses runs of
     * whitespace to a single space (then trims).
     */
    fun strip(html: String): String {
        if (html.isEmpty()) return ""
        if ('<' !in html && '&' !in html) return html.trim()
        // Keep word boundaries where block tags / line breaks were.
        var text = blockBreakRegex.replace(html, " ")
        text = tagRegex.replace(text, "")
        text = decodeEntities(text)
        return whitespaceRegex.replace(text, " ").trim()
    }

    /**
     * Returns the footnote ids (`foot_note` attribute values) found in
     * `<sup foot_note="...">` markers, in document order.
     */
    fun extractFootnoteMarkers(html: String): List<String> {
        if (!html.contains("<sup")) return emptyList()
        return footnoteSupRegex.findAll(html).map { it.groupValues[1] }.toList()
    }

    private fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        var out = text
        out = out.replace("&nbsp;", " ")
        out = out.replace("&lt;", "<")
        out = out.replace("&gt;", ">")
        out = out.replace("&quot;", "\"")
        out = out.replace("&#39;", "'")
        out = out.replace("&#x27;", "'")
        out = out.replace("&apos;", "'")
        out = decimalEntityRegex.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            codeToString(code) ?: match.value
        }
        out = hexEntityRegex.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
            codeToString(code) ?: match.value
        }
        // Decode &amp; last so "&amp;lt;" single-decodes to "&lt;" (like HtmlCompat).
        out = out.replace("&amp;", "&")
        return out
    }

    private fun codeToString(code: Int): String? {
        if (code < 0 || code > 0x10FFFF) return null
        return if (code <= 0xFFFF) {
            code.toChar().toString()
        } else {
            val high = ((code - 0x10000) shr 10) + 0xD800
            val low = ((code - 0x10000) and 0x3FF) + 0xDC00
            charArrayOf(high.toChar(), low.toChar()).concatToString()
        }
    }
}
