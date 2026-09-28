package com.nur.quran.desktop

import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.DesktopFonts
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.shared.HtmlStripper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the verse-text pipeline: every verse in every bundled font must
 * render exactly like Android (ornaments stripped per-font, exactly one
 * end marker), and translations must not contain missing-space typos.
 *
 * Regression test for the U+06DF ۟ tofu blobs seen on Al-Baqarah 2:5/2:6.
 */
class VerseTextTest {

    /**
     * Mirrors Android's strip sets exactly:
     * - ORNAMENT_PLAIN ([۟۠ۢ + U+06EA–U+06EC + U+25CC]) is stripped for EVERY font.
     * - ORNAMENT_EMBEDDED extras ([۝۞ + ornate brackets]) are additionally
     *   stripped only for KFGQPC (embedded-marker font). Other fonts keep
     *   ۞/﴿﴾ (e.g. the rub-el-hizb ۞ in 2:26) and render them — Amiri does.
     */
    private val bannedEverywhere = Regex("[۟۠ۢ\u06EA-\u06EC◌]")
    private val bannedKfgqpcOnly = Regex("[۝۞\uFD3E\uFD3F{}]")

    @Test
    fun `no stripped ornaments survive display text in any font`() {
        val fonts = DesktopFonts.names
        assertTrue(fonts.isNotEmpty())
        var checked = 0
        for (font in fonts) {
            for (id in 1..114) {
                for (v in QuranStore.versesOfChapter(id)) {
                    val text = verseDisplayArabic(v, font)
                    assertFalse(
                        "ornament survived in ${v.verseKey} [$font]: ${text.takeLast(30)}",
                        bannedEverywhere.containsMatchIn(text)
                    )
                    if (DesktopFonts.isEmbeddedEndMarker(font)) {
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
                    // Exactly one end marker: text ends with Arabic-Indic digits.
                    val last = text.trimEnd().lastOrNull()
                    assertTrue(
                        "missing end marker in ${v.verseKey} [$font]: ${text.takeLast(20)}",
                        last != null && last.code in 0x0660..0x0669
                    )
                    checked++
                }
            }
        }
        assertTrue("expected 6236 verses x fonts, got $checked", checked == 6236 * fonts.size)
    }

    @Test
    fun `translations have no missing spaces after punctuation`() {
        val badComma = Regex(",(?=[A-Za-z])")
        val badSemi = Regex(";(?=[A-Za-z])")
        var repaired = 0
        for (id in 1..114) {
            for (v in QuranStore.versesOfChapter(id)) {
                val hadTypo = badComma.containsMatchIn(v.translation) ||
                    badSemi.containsMatchIn(v.translation)
                val clean = HtmlStripper.strip(v.translation)
                assertFalse("comma typo in ${v.verseKey}", badComma.containsMatchIn(clean))
                assertFalse("semicolon typo in ${v.verseKey}", badSemi.containsMatchIn(clean))
                if (hadTypo) repaired++
            }
        }
        assertTrue("expected the 10 known typos to be repaired, got $repaired", repaired == 10)
    }
}
