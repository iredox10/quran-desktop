package com.nur.quran.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.data.VerseWord
import com.nur.quran.desktop.data.WordStore
import com.nur.quran.desktop.ui.components.TajweedRules
import com.nur.quran.desktop.ui.components.WordTooltipCard
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.desktop.ui.theme.rememberBodyFontFamily
import androidx.compose.runtime.remember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Word study: tajweed rule catalog, dataset/display alignment, tooltip card. */
class WordTooltipTest {

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    @Test
    fun tajweedRuleCatalogResolvesAliases() {
        assertEquals("Idghaam without Ghunnah", TajweedRules.find("idgham_wo_ghunnah")?.name)
        assertEquals("Ikhfa", TajweedRules.find("ikhafa")?.name)
        assertEquals("Silent", TajweedRules.find("slnt")?.name)
        assertEquals("Qalqalah", TajweedRules.find("qalaqah")?.name)
        assertEquals("Madda (Normal)", TajweedRules.find("madda_normal")?.name)
        assertEquals(null, TajweedRules.find("not_a_rule"))
    }

    @Test
    fun rulesInExtractsInDocumentOrderWithoutDuplicates() {
        val html = "<rule class=ham_wasl>ٱ</rule>للَّهِ <rule class=madda_normal>ـٰ</rule>نِ" +
            "<rule class=ham_wasl>ٱ</rule>"
        val rules = TajweedRules.rulesIn(html)
        assertEquals(listOf("Hamzat al-Wasl", "Madda (Normal)"), rules.map { it.name })
        assertTrue(TajweedRules.rulesIn(null).isEmpty())
    }

    @Test
    fun bundledWordsAlignWithReaderDisplayWords() {
        // The reader maps a tapped display word index onto the bundled word
        // list, so both must have the same number of whitespace tokens
        // (including the end-of-ayah marker).
        val samples = listOf("1:1", "1:7", "2:255", "18:10", "36:1", "112:4", "114:6")
        for (key in samples) {
            val verse = QuranStore.versesOfChapter(key.substringBefore(":").toInt())
                .first { it.verseKey == key }
            val display = verseDisplayArabic(verse, "KFGQPC Hafs", "uthmani")
                .trim()
                .split(Regex("\\s+"))
            val words = WordStore.wordsForVerse(key)
            assertEquals("$key display=$display", words.size, display.size)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun translationTooltipRendersWordTranslation() {
        val word = VerseWord(
            verseKey = "1:1",
            position = 1,
            arabic = "بِسْمِ",
            translation = "In (the) name",
            transliteration = "bis'mi",
            tajweedHtml = "",
            isEndMarker = false
        )
        rule.setContent {
            NurTheme {
                val fontBody = rememberBodyFontFamily()
                WordTooltipCard(
                    pal = pal,
                    word = word,
                    showTajweedRules = false,
                    fontUi = remember { fontBody },
                    onDismiss = {}
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("In (the) name").assertExists()
        rule.onNodeWithText("bis'mi").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tajweedTooltipRendersRuleNameAndDescription() {
        val word = VerseWord(
            verseKey = "1:2",
            position = 1,
            arabic = "ٱلْحَمْدُ",
            translation = "All praise",
            transliteration = "al-hamdu",
            tajweedHtml = "<rule class=ham_wasl>ٱ</rule>لْحَمْدُ",
            isEndMarker = false
        )
        rule.setContent {
            NurTheme {
                WordTooltipCard(
                    pal = pal,
                    word = word,
                    showTajweedRules = true,
                    fontUi = rememberBodyFontFamily(),
                    onDismiss = {}
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Hamzat al-Wasl").assertExists()
        assertNotNull(TajweedRules.find("ham_wasl"))
    }
}
