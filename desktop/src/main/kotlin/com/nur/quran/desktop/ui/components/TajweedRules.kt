package com.nur.quran.desktop.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Tajweed rule explanations shown by the reader's word tooltips, ported from
 * the web `TajweedTooltip.jsx` catalog. Colors match the web legend so the
 * reader's colored text and the popup agree.
 */
data class TajweedRule(
    val id: String,
    val name: String,
    val nameArabic: String,
    val color: Color,
    val description: String
)

object TajweedRules {

    private fun rule(
        id: String,
        name: String,
        nameArabic: String,
        color: Long,
        description: String
    ) = TajweedRule(id, name, nameArabic, Color(color), description)

    private val catalog: Map<String, TajweedRule> = listOf(
        rule(
            "ham_wasl", "Hamzat al-Wasl", "همزة الوصل", 0xFFAAAAAA,
            "A connecting hamza that is pronounced at the beginning of speech but silent when preceded by another word. It serves to connect words smoothly in recitation."
        ),
        rule(
            "laam_shamsiyah", "Laam Shamsiyyah", "لام شمسية", 0xFFAAAAAA,
            "The \"Lam\" of the definite article (Al-) is silent and assimilates into the following letter, which is one of the 14 \"sun letters\" (huruf shamsiyyah)."
        ),
        rule(
            "madda_normal", "Madda (Normal)", "مد طبيعي", 0xFF537FFF,
            "A natural elongation of 2 counts (harakaat). It occurs with the three letters of madd: Alif, Waw, and Ya when they follow their corresponding vowels."
        ),
        rule(
            "madda_permissible", "Madda (Permissible)", "مد جائز", 0xFF4050FF,
            "An elongation of 2, 4, or 5 counts that occurs when a hamza comes after a madd letter. The reader may choose the length, hence \"permissible\" (Jaa'iz)."
        ),
        rule(
            "madda_obligatory", "Madda (Obligatory)", "مد لازم", 0xFF000FB5,
            "A mandatory elongation of 6 counts that occurs when a madd letter is followed by a shaddah or sukoon within the same word. It must always be stretched to 6 counts."
        ),
        rule(
            "madda_necessary", "Madda (Necessary)", "مد واجب", 0xFF2142C7,
            "A necessary elongation of 4-5 counts that occurs when a madd letter is followed by a hamza in the same word."
        ),
        rule(
            "qalpiala", "Qalqalah", "قلقلة", 0xFFDD0008,
            "An echoing or bouncing sound produced when pronouncing one of the five Qalqalah letters (ق ط ب ج د) with a sukoon. The sound bounces off the articulation point."
        ),
        rule(
            "ikhfa_shafawi", "Ikhfa Shafawi", "إخفاء شفوي", 0xFFD500B7,
            "Oral hiding — when a Meem Saakinah (مْ) is followed by the letter Ba (ب). The meem is pronounced with a slight nasalization while hiding its sound."
        ),
        rule(
            "ikhfa", "Ikhfa", "إخفاء", 0xFF26BFFD,
            "Hiding — when a Noon Saakinah or Tanween is followed by one of 15 specific letters. The noon sound is hidden with a nasal tone for 2 counts."
        ),
        rule(
            "idghaam_shafawi", "Idghaam Shafawi", "إدغام شفوي", 0xFF169777,
            "Lip merging — when a Meem Saakinah (مْ) is followed by another Meem (م). The two meems merge into one with a ghunnah (nasalization) of 2 counts."
        ),
        rule(
            "idghaam_ghunnah", "Idghaam with Ghunnah", "إدغام بغنة", 0xFF169200,
            "Merging with nasalization — when a Noon Saakinah or Tanween is followed by one of 4 letters (ي ن م و). The noon merges into the following letter with a ghunnah of 2 counts."
        ),
        rule(
            "idghaam_no_ghunnah", "Idghaam without Ghunnah", "إدغام بلا غنة", 0xFF169200,
            "Merging without nasalization — when a Noon Saakinah or Tanween is followed by Lam (ل) or Ra (ر). The noon merges completely without any nasal sound."
        ),
        rule(
            "idghaam_mutajanisayn", "Idghaam Mutajanisayn", "إدغام متجانسين", 0xFFA1A1A1,
            "Merging of homorganic letters — when two letters share the same articulation point but differ in characteristics. The first letter merges into the second."
        ),
        rule(
            "idghaam_mutaqaribayn", "Idghaam Mutaqaribayn", "إدغام متقاربين", 0xFFA1A1A1,
            "Merging of close letters — when two letters have close articulation points and similar characteristics. The first letter assimilates into the second."
        ),
        rule(
            "iqlab", "Iqlab", "إقلاب", 0xFF26BFFD,
            "Conversion — when a Noon Saakinah or Tanween is followed by the letter Ba (ب). The noon sound converts into a Meem (م) with a ghunnah of 2 counts."
        ),
        rule(
            "ghunnah", "Ghunnah", "غنة", 0xFFFF7E1E,
            "A nasalization sound of 2 counts that comes from the nasal passage. It naturally accompanies the letters Noon (ن) and Meem (م), especially with shaddah."
        ),
        rule(
            "silent", "Silent", "حرف ساكن", 0xFFAAAAAA,
            "A letter that is written but not pronounced during recitation. It appears in the script but is skipped when reading aloud."
        )
    ).associateBy { it.id }

    /** Aliases used by the quran.com word markup (typos and short forms). */
    private val aliases: Map<String, String> = mapOf(
        "qalaqah" to "qalpiala",
        "ikhafa_shafawi" to "ikhfa_shafawi",
        "ikhafa" to "ikhfa",
        "idgham_shafawi" to "idghaam_shafawi",
        "idgham_ghunnah" to "idghaam_ghunnah",
        "idgham_wo_ghunnah" to "idghaam_no_ghunnah",
        "idgham_mutajanisayn" to "idghaam_mutajanisayn",
        "idgham_mutaqaribayn" to "idghaam_mutaqaribayn",
        "slnt" to "silent"
    )

    private val ruleClassRegex = Regex("class=([A-Za-z_]+)")

    fun find(ruleClass: String): TajweedRule? {
        val id = aliases[ruleClass] ?: ruleClass
        return catalog[id]
    }

    /** Rules that occur inside one word's tajweed markup, in document order. */
    fun rulesIn(tajweedHtml: String?): List<TajweedRule> {
        if (tajweedHtml.isNullOrBlank()) return emptyList()
        val seen = mutableSetOf<String>()
        return ruleClassRegex.findAll(tajweedHtml)
            .mapNotNull { find(it.groupValues[1]) }
            .filter { seen.add(it.id) }
            .toList()
    }

    /** Every known rule, ordered for a legend list. */
    fun all(): List<TajweedRule> = catalog.values.toList()
}
