package com.nur.quran.desktop

import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.DesktopFonts
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.shared.HtmlStripper
import com.nur.quran.shared.formatArabicDigits
import com.nur.quran.shared.usesEmbeddedEndMarker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the verse-text pipeline: every verse in every bundled font must
 * render exactly like Android (floating ornaments stripped per-font, collapsed
 * whitespace, exactly one end marker), and translations must not contain
 * missing-space typos.
 *
 * Regression tests:
 * - U+06DF ۟ tofu blobs on Al-Baqarah 2:5/2:6.
 * - Floating waqf-sign blobs (U+06D6-ۖ … ۛ) in the word gaps of Surah Sad
 *   38:6/38:8 (verse-level text_uthmani carries them as space-separated
 *   tokens, so the standalone combining marks rendered as detached rings).
 */
class VerseTextTest {

    /**
     * Mirrors production predicates in shared/VerseText.kt EXACTLY
     * (isOrnamentalNoFrame for every font, isOrnamental extras for KFGQPC):
     * - stripped for EVERY font: waqf signs U+06D6-U+06DC, rub-el-hizb U+06DE,
     *   recitation marks U+06DF / U+06E0 / U+06E2-U+06E8 / U+06EA-U+06ED,
     *   sajdah U+06E9, U+25CC dotted circle.
     * - U+06E1 is CARVED OUT (load-bearing vowel in both scripts, 62k hits).
     * - KFGQPC additionally strips U+06DD + ornate brackets (bare-digit
     *   medallion needs no frame); other fonts get exactly one U+06DD marker.
     */
    private val bannedEverywhere = Regex("[\u06D6-\u06DC\u06DE\u06DF\u06E0\u06E2-\u06E8\u06EA-\u06ED\u06E9\u25CC]")
    private val indopakVowelKept = 0x6E1.toChar()
    private val bannedKfgqpcOnly = Regex("[\u06DD\uFD3E\uFD3F{}]")

    /** U+0615 must be gone from Indopak-script display text (waqf rings). */
    private fun hasIndopakWaqf(s: String): Boolean = s.any { it.code == 0x615 }

    /** ZWSP/ZWNBSP/PUA must be gone (unmapped -> dotted-circle tofu). */
    private fun hasIndopakInvisible(s: String): Boolean =
        s.any { it.code == 0x200B || it.code == 0xFEFF || it.code in 0xE000..0xF8FF }
    private val arabicDigits = Regex("[\u0660-\u0669]")

    /** `verseDisplayArabic` marker tail for [font] + [verseNumber]. */
    private fun expectedMarker(font: String, verseNumber: Int): String {
        val digits = formatArabicDigits(verseNumber)
        return if (usesEmbeddedEndMarker(font)) " $digits" else " \u06DD$digits"
    }

    @Test
    fun `no stripped ornaments survive display text in any font`() {
        val fonts = DesktopFonts.names
        assertTrue(fonts.isNotEmpty())
        var checked = 0
        for (font in fonts) {
            val embedded = DesktopFonts.isEmbeddedEndMarker(font)
            for (id in 1..114) {
                for (v in QuranStore.versesOfChapter(id)) {
                    val text = verseDisplayArabic(v, font)
                    // Indopak-script pass (mushaf id "indopak"): U+0615 waqf and
                    // ZWSP/ZWNBSP/PUA must be gone; U+06E1 vowel must survive
                    // (it is a letter in this script, 62k hits).
                    val indoText = verseDisplayArabic(v, font, "indopak")
                    assertFalse(
                        "indopak waqf U+0615 survived in ${v.verseKey} [$font]",
                        hasIndopakWaqf(indoText),
                    )
                    assertFalse(
                        "indopak invisible/PUA survived in ${v.verseKey} [$font]",
                        hasIndopakInvisible(indoText),
                    )
                    assertFalse(
                        "indopak double space in ${v.verseKey} [$font]",
                        indoText.contains("  "),
                    )
                    assertTrue(
                        "indopak vowel U+06E1 lost in ${v.verseKey} [$font] " +
                            "(raw has ${v.textIndopak.count { it.code == 0x6E1 }})",
                        v.textIndopak.none { it.code == 0x6E1 } ||
                            indoText.contains(indopakVowelKept),
                    )
                    assertFalse(
                        "floating ornament survived in ${v.verseKey} [$font]: ${text.takeLast(30)}",
                        bannedEverywhere.containsMatchIn(text)
                    )
                    assertFalse(
                        "double space in ${v.verseKey} [$font]: ${text.takeLast(40)}",
                        text.contains("  ")
                    )
                    if (embedded) {
                        // KFGQPC: bare-digit medallion, no U+06DD frame, and the
                        // embedded-set ornaments are stripped too.
                        assertFalse(
                            "U+06DD leaked into KFGQPC text ${v.verseKey}",
                            text.contains('۝')
                        )
                        assertFalse(
                            "embedded ornament survived in ${v.verseKey}: ${text.takeLast(30)}",
                            bannedKfgqpcOnly.containsMatchIn(text)
                        )
                    }
                    // Exactly one end marker, at the very end, with NO digits or
                    // frame anywhere in the body (raw text_uthmani has none).
                    val marker = expectedMarker(font, v.verseNumber)
                    assertTrue(
                        "bad end marker in ${v.verseKey} [$font]: …${text.takeLast(20)}",
                        text.endsWith(marker)
                    )
                    val body = text.dropLast(marker.length)
                    assertFalse(
                        "stray digit in body of ${v.verseKey} [$font]",
                        arabicDigits.containsMatchIn(body)
                    )
                    assertFalse(
                        "stray U+06DD in body of ${v.verseKey} [$font]",
                        body.contains('۝')
                    )
                    checked++
                }
            }
        }
        assertTrue("expected 6236 verses x fonts, got $checked", checked == 6236 * fonts.size)
    }

    /**
     * Regression: Surah Sad 38:6/38:8 — the waqf-sign tokens (ۖ ۚ) in
     * text_uthmani used to render as detached floating rings between words.
     */
    @Test
    fun `surah sad waqf blobs are gone and marker appears once`() {
        for (font in DesktopFonts.names) {
            for ((key, number) in listOf("38:6" to 6, "38:8" to 8)) {
                val v = QuranStore.versesOfChapter(38).first { it.verseKey == key }
                val text = verseDisplayArabic(v, font)
                assertFalse(
                    "waqf sign survived in $key [$font]: $text",
                    Regex("[\u06D6-\u06DC]").containsMatchIn(text)
                )
                assertFalse("double space in $key [$font]: $text", text.contains("  "))
                // Marker exactly once: endswith the canonical marker, which
                // itself contains the only U+06DD (non-embedded) / digits.
                val marker = expectedMarker(font, number)
                assertTrue("missing marker in $key [$font]: …${text.takeLast(15)}", text.endsWith(marker))
                val occurrences = text.length - text.replace(marker, "").length
                assertTrue(
                    "marker count ${occurrences / marker.length} in $key [$font]",
                    occurrences == marker.length
                )
            }
        }
    }

    @Test
    fun `translations have no missing spaces after punctuation`() {
        val badComma = Regex(",(?=[A-Za-z])")
        val badSemi = Regex(";(?=[A-Za-z])")
        val badPeriod = Regex("\\.(?=[A-Z])")
        var repaired = 0
        for (id in 1..114) {
            for (v in QuranStore.versesOfChapter(id)) {
                val hadTypo = badComma.containsMatchIn(v.translation) ||
                    badSemi.containsMatchIn(v.translation) ||
                    badPeriod.containsMatchIn(v.translation)
                val clean = HtmlStripper.strip(v.translation)
                assertFalse("comma typo in ${v.verseKey}", badComma.containsMatchIn(clean))
                assertFalse("semicolon typo in ${v.verseKey}", badSemi.containsMatchIn(clean))
                assertFalse("period typo in ${v.verseKey}", badPeriod.containsMatchIn(clean))
                if (hadTypo) repaired++
            }
        }
        assertTrue("expected the 15 known typos to be repaired, got $repaired", repaired == 15)
    }
}
