package com.nur.quran.shared

/**
 * Pure verse-text helpers ported from SurahScreen.kt (lines ~100-350) of the
 * Android app. No Compose, no Android imports — depends only on [Verse],
 * [Word] (Entities.kt) plus the shared [Mushaf], [wordTextForMushaf] and
 * [verseTextForMushaf] ports living in this same package.
 *
 * Compose-only pieces from the source range (PlatformTextStyle /
 * LineHeightStyle builders, FontFamily getters) are intentionally omitted:
 * they cannot exist in commonMain.
 */

/**
 * Shared vertical rhythm for Quran Arabic text, used by BOTH renderers so
 * toggling tajweed never changes the air between lines.
 */
const val ARABIC_LINE_HEIGHT_RATIO = 2.0f

fun formatArabicDigits(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

// The KFGQPC Hafs font draws the full end-of-ayah medallion from the digits alone
// (its GSUB substitutes each digit with the ornament composite), so emitting the
// U+06DD mark before the digits would render TWO medallions.
fun usesEmbeddedEndMarker(fontName: String): Boolean {
    val name = fontName.trim().lowercase()
    return name == "kfgqpc-hafs" || name == "kfgqpc hafs"
}

fun formatArabicVerseEndMarker(verseNumber: Int, fontName: String = "KFGQPC Hafs"): String {
    val digits = formatArabicDigits(verseNumber)
    val mark = if (usesEmbeddedEndMarker(fontName)) digits else "۝$digits"
    return "<tajweed class='end'>$mark</tajweed>"
}

fun formatCleanEndMarker(endWord: Word?, verseNumber: Int, fontName: String = "KFGQPC Hafs"): String {
    // verseNumber is authoritative: the end-word raw text carries Arabic-Indic
    // digits which never parse via toIntOrNull, so the old parse-then-fallback
    // was misleading dead code. Digits always come from verseNumber.
    val digits = formatArabicDigits(verseNumber)
    val mark = if (usesEmbeddedEndMarker(fontName)) digits else "۝$digits"
    return "<tajweed class='end'>$mark</tajweed>"
}

fun buildCleanVerseTajweedHtml(fullVerseHtml: String?, words: List<Word>, verseNumber: Int, fontName: String = "KFGQPC Hafs"): String {
    val endWord = words.firstOrNull { it.charTypeName == "end" }
    val cleanEndMarker = formatCleanEndMarker(endWord, verseNumber, fontName)

    val cleanInput = if (!fullVerseHtml.isNullOrBlank()) {
        fullVerseHtml
    } else {
        words.filter { it.charTypeName != "end" }.joinToString(" ") { word ->
            word.textUthmaniTajweed ?: (word.textUthmani ?: "")
        }
    }

    val baseHtml = cleanInput
        .replace("<(span|tajweed|rule)\\s+class=['\"]?end[^'\">]*['\"]?>.*?</(span|tajweed|rule)>".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("<(span|tajweed|rule)\\s+class=['\"]?end[^'\">]*['\"]?>".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("[۝۞۪۟۠ۢ-۬◌]".toRegex(), "")
        .replace("&#1757;|&#x0*6dd;|&#x0*6DD;".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("﴿.*?﴾".toRegex(), "")
        .replace("\\{.*?\\}".toRegex(), "")
        .replace("[﴿﴾{}]".toRegex(), "")

    return baseHtml.trimEnd() + cleanEndMarker
}

private val ORNAMENT_EMBEDDED_REGEX =
    "[\u06D6-\u06DC\u06DE\u06DD\u06DF-\u06E8\u06EA-\u06ED\u25CC\u06E9\uFD3E\uFD3F{}]".toRegex()

/**
 * Floating small-ornament marks. Verse-level display strips ALL of them —
 * including the waqf signs (U+06D6-06DC) and rub-el-hizb (U+06DE) — because a
 * standalone combining mark with non-zero advance renders as a floating blob
 * between words instead of attaching over the preceding glyph. The end-of-ayah
 * frame U+06DD is intentionally NOT in this set (non-KFGQPC fonts need it for
 * the medallion) — it is canonicalized separately below.
 *
 * NOTE: this set must ALSO cover the recitation marks U+06DF..U+06E8 and
 * U+06EA..U+06ED (sajdah U+06E9 included): a standalone combining mark with no
 * base renders on desktop Skia as a dotted-circle / floating blob, and PIL /
 * headless shaping confirmed KFGQPC has no dotted-circle fallback of its own —
 * the blobs come from the fallback font. U+06E9 is stripped too (the reader
 * shows a "Sajdah N" badge instead).
 */
private val ORNAMENT_PLAIN_REGEX =
    "[\u06D6-\u06DC\u06DE\u06DF-\u06E8\u06EA-\u06ED\u06E9\u25CC]".toRegex()

private fun foldExtendedArabicDigitsToStandard(text: String): String {
    val sb = StringBuilder(text.length)
    for (c in text) {
        val v = c.code
        sb.append(if (v in 0x06F0..0x06F9) (0x0660 + (v - 0x06F0)).toChar() else c)
    }
    return sb.toString()
}

private fun ensureSingleEndMarkerFrame(text: String): String {
    val frame = 0x06DD.toChar()
    val folded = foldExtendedArabicDigitsToStandard(text)
    val collapsed = StringBuilder(folded.length)
    var prevFrame = false
    for (c in folded) {
        if (c == frame) {
            if (!prevFrame) collapsed.append(c)
            prevFrame = true
        } else {
            collapsed.append(c)
            prevFrame = false
        }
    }
    val s = collapsed.toString()
    val out = StringBuilder(s.length + 1)
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c.code in 0x0660..0x0669 && (i == 0 || s[i - 1] != frame)) out.append(frame)
        out.append(c)
        i++
    }
    return out.toString()
}

/**
 * Display text per word, index-aligned with [words] (no reordering/removal,
 * so tap-target indices stay valid).
 *
 * - Picks the mushaf script (QPC Hafs vs Indopak).
 * - KFGQPC Hafs shapes bare digits into ONE ayah medallion via GSUB, so any
 *   U+06DD/ornament chars are stripped — otherwise TWO medallions render
 *   (empty frame + numbered medallion).
 * - Other fonts need the U+06DD frame to draw the medallion, so it is kept
 *   (and ensured) on the single end marker.
 * - Extra `end` words (e.g. stale offline rows surviving alongside network
 *   rows) collapse to "" so exactly one ayah marker renders.
 */
fun mushafPlainWordTexts(words: List<Word>, mushafId: String, fontName: String): List<String> {
    val mushaf = Mushaf.fromId(mushafId)
    val embedded = usesEmbeddedEndMarker(fontName)
    var endSeen = false
    return words.map { word ->
        var t = wordTextForMushaf(mushaf, word.textUthmani, word.textIndopak, word.textQpcHafs)
        t = if (embedded) t.replace(ORNAMENT_EMBEDDED_REGEX, "") else t.replace(ORNAMENT_PLAIN_REGEX, "")
        if (word.charTypeName == "end") {
            if (endSeen) return@map ""
            endSeen = true
            if (embedded) {
                t = t.trim()
            } else {
                // Canonicalize to a single U+06DD + bare standard digits so the OFF path is
                // glyph-identical to the ON-path rebuilt marker (U+06DD + verseNumber digits).
                val bare = foldExtendedArabicDigitsToStandard(t.replace(ORNAMENT_EMBEDDED_REGEX, "").trim())
                t = if (bare.isBlank()) "" else 0x06DD.toChar().toString() + bare
            }
        }
        t
    }
}

/** Verse-level fallback (words empty): same per-font ornament rules. */
fun mushafPlainVerseText(verse: Verse, mushafId: String, fontName: String): String {
    val mushaf = Mushaf.fromId(mushafId)
    var t = verseTextForMushaf(mushaf, verse.textUthmani, verse.textIndopak, verse.textQpcHafs)
    t = if (usesEmbeddedEndMarker(fontName)) {
        t.replace(ORNAMENT_EMBEDDED_REGEX, "")
    } else {
        // Strip ALL floating ornaments (waqf signs included — they are mushaf
        // punctuation, not recitation text), then canonicalize to a single
        // U+06DD + standard digits: glyph-identical to ON-path rebuilt marker.
        val stripped = t.replace(ORNAMENT_PLAIN_REGEX, "")
            .filter { c -> c.code != 0xFD3F && c.code != 0xFD3E && c != '{' && c != '}' }
        ensureSingleEndMarkerFrame(stripped)
    }
    // Stripping a standalone mark leaves its leading space behind ("كُمْ  إِنَّ").
    // Collapse runs of whitespace so no visible gap artifact remains.
    return t.replace(Regex("\\s+"), " ").trim()
}

/**
 * Single source of truth for VerseItem Arabic base text.
 * Built exactly like the tajweed path: mushafPlainWordTexts + space-joined
 * + per-word ranges. Both tajweed-ON and OFF branches must use this so the
 * base strings render byte-identical (only spans/colors may differ).
 */
fun buildVerseDisplayText(
    words: List<Word>,
    mushafId: String,
    fontName: String
): Pair<String, List<Pair<IntRange, Int>>> {
    val sb = StringBuilder()
    val ranges = mutableListOf<Pair<IntRange, Int>>()
    val displayWords = mushafPlainWordTexts(words, mushafId, fontName)
    words.forEachIndexed { wordIndex, _ ->
        if (wordIndex > 0) sb.append(" ")
        val rawText = displayWords.getOrElse(wordIndex) { "" }
        val start = sb.length
        sb.append(rawText)
        ranges.add(Pair(start..sb.length, wordIndex))
    }
    return sb.toString() to ranges
}

data class TajweedRule(
    val name: String,
    val nameAr: String,
    val colorHex: String,
    val description: String
)

object TajweedRules {
    val RULES = mapOf(
        "ham_wasl" to TajweedRule(
            "Hamzat al-Wasl",
            "همزة الوصل",
            "#AAAAAA",
            "A connecting hamza that is pronounced at the beginning of speech but silent when preceded by another word. It serves to connect words smoothly in recitation."
        ),
        "laam_shamsiyah" to TajweedRule(
            "Laam Shamsiyyah",
            "لام شمسية",
            "#AAAAAA",
            "The \"Lam\" of the definite article (Al-) is silent and assimilates into the following letter, which is one of the 14 \"sun letters\" (huruf shamsiyyah)."
        ),
        "madda_normal" to TajweedRule(
            "Madda (Normal)",
            "مد طبيعي",
            "#537FFF",
            "A natural elongation of 2 counts (harakaat). It occurs with the three letters of madd: Alif, Waw, and Ya when they follow their corresponding vowels."
        ),
        "madda_permissible" to TajweedRule(
            "Madda (Permissible)",
            "مد جائز",
            "#4050FF",
            "An elongation of 2, 4, or 5 counts that occurs when a hamza comes after a madd letter. The reader may choose the length, hence \"permissible\" (Jaa'iz)."
        ),
        "madda_obligatory" to TajweedRule(
            "Madda (Obligatory)",
            "مد لازم",
            "#000FB5",
            "A mandatory elongation of 6 counts that occurs when a madd letter is followed by a shaddah or sukoon within the same word. It must always be stretched to 6 counts."
        ),
        "madda_necessary" to TajweedRule(
            "Madda (Necessary)",
            "مد واجب",
            "#2142c7",
            "A necessary elongation of 4-5 counts that occurs when a madd letter is followed by a hamza in the same word."
        ),
        "qalpiala" to TajweedRule(
            "Qalqalah",
            "قلقلة",
            "#DD0008",
            "An echoing or bouncing sound produced when pronouncing one of the five Qalqalah letters (ق ط ب ج د) with a sukoon. The sound bounces off the articulation point."
        ),
        "qalaqah" to TajweedRule(
            "Qalqalah",
            "قلقلة",
            "#DD0008",
            "An echoing or bouncing sound produced when pronouncing one of the five Qalqalah letters (ق ط ب ج د) with a sukoon. The sound bounces off the articulation point."
        ),
        "ikhfa_shafawi" to TajweedRule(
            "Ikhfa Shafawi",
            "إخفاء شفوي",
            "#D500B7",
            "Oral hiding — when a Meem Saakinah (مْ) is followed by the letter Ba (ب). The meem is pronounced with a slight nasalization while hiding its sound."
        ),
        "ikhafa_shafawi" to TajweedRule(
            "Ikhfa Shafawi",
            "إخفاء شفوي",
            "#D500B7",
            "Oral hiding — when a Meem Saakinah (مْ) is followed by the letter Ba (ب). The meem is pronounced with a slight nasalization while hiding its sound."
        ),
        "ikhfa" to TajweedRule(
            "Ikhfa",
            "إخفاء",
            "#26BFFD",
            "Hiding — when a Noon Saakinah or Tanween is followed by one of 15 specific letters. The noon sound is hidden with a nasal tone for 2 counts."
        ),
        "ikhafa" to TajweedRule(
            "Ikhfa",
            "إخفاء",
            "#26BFFD",
            "Hiding — when a Noon Saakinah or Tanween is followed by one of 15 specific letters. The noon sound is hidden with a nasal tone for 2 counts."
        ),
        "idghaam_shafawi" to TajweedRule(
            "Idghaam Shafawi",
            "إدغام شفوي",
            "#169777",
            "Lip merging — when a Meem Saakinah (مْ) is followed by another Meem (م). The two meems merge into one with a ghunnah (nasalization) of 2 counts."
        ),
        "idgham_shafawi" to TajweedRule(
            "Idghaam Shafawi",
            "إدغام شفوي",
            "#169777",
            "Lip merging — when a Meem Saakinah (مْ) is followed by another Meem (م). The two meems merge into one with a ghunnah (nasalization) of 2 counts."
        ),
        "idghaam_ghunnah" to TajweedRule(
            "Idghaam with Ghunnah",
            "إدغام بغنة",
            "#169200",
            "Merging with nasalization — when a Noon Saakinah or Tanween is followed by one of 4 letters (ي ن م و). The noon merges into the following letter with a ghunnah of 2 counts."
        ),
        "idgham_ghunnah" to TajweedRule(
            "Idghaam with Ghunnah",
            "إدغام بغنة",
            "#169200",
            "Merging with nasalization — when a Noon Saakinah or Tanween is followed by one of 4 letters (ي ن م و). The noon merges into the following letter with a ghunnah of 2 counts."
        ),
        "idghaam_no_ghunnah" to TajweedRule(
            "Idghaam without Ghunnah",
            "إدغام بلا غنة",
            "#169200",
            "Merging without nasalization — when a Noon Saakinah or Tanween is followed by Lam (ل) or Ra (ر). The noon merges completely without any nasal sound."
        ),
        "idgham_wo_ghunnah" to TajweedRule(
            "Idghaam without Ghunnah",
            "إدغام بلا غنة",
            "#169200",
            "Merging without nasalization — when a Noon Saakinah or Tanween is followed by Lam (ل) or Ra (ر). The noon merges completely without any nasal sound."
        ),
        "idghaam_mutajanisayn" to TajweedRule(
            "Idghaam Mutajanisayn",
            "إدغام متجانسين",
            "#A1A1A1",
            "Merging of homorganic letters — when two letters share the same articulation point but differ in characteristics. The first letter merges into the second."
        ),
        "idgham_mutajanisayn" to TajweedRule(
            "Idghaam Mutajanisayn",
            "إدغام متجانسين",
            "#A1A1A1",
            "Merging of homorganic letters — when two letters share the same articulation point but differ in characteristics. The first letter merges into the second."
        ),
        "idghaam_mutaqaribayn" to TajweedRule(
            "Idghaam Mutaqaribayn",
            "إدغام متقاربين",
            "#A1A1A1",
            "Merging of close letters — when two letters have close articulation points and similar characteristics. The first letter assimilates into the second."
        ),
        "idgham_mutaqaribayn" to TajweedRule(
            "Idghaam Mutaqaribayn",
            "إدغام متقاربين",
            "#A1A1A1",
            "Merging of close letters — when two letters have close articulation points and similar characteristics. The first letter assimilates into the second."
        ),
        "iqlab" to TajweedRule(
            "Iqlab",
            "إقلاب",
            "#26BFFD",
            "Conversion — when a Noon Saakinah or Tanween is followed by the letter Ba (ب). The noon sound converts into a Meem (م) with a ghunnah of 2 counts."
        ),
        "ghunnah" to TajweedRule(
            "Ghunnah",
            "غنة",
            "#FF7E1E",
            "A nasalization sound of 2 counts that comes from the nasal passage. It naturally accompanies the letters Noon (ن) and Meem (م), especially with shaddah."
        ),
        "silent" to TajweedRule(
            "Silent",
            "حرف ساكن",
            "#AAAAAA",
            "A letter that is written but not pronounced during recitation. It appears in the script but is skipped when reading aloud."
        ),
        "slnt" to TajweedRule(
            "Silent",
            "حرف ساكن",
            "#AAAAAA",
            "A letter that is written but not pronounced during recitation. It appears in the script but is skipped when reading aloud."
        )
    )
}
