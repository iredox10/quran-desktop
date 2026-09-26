package com.nur.quran.shared

/**
 * Catalog of Quran.com (api.quran.com v4) translation editions.
 * Ported from quran-kotlin .../data/translation/Translations.kt.
 *
 * IDs are `resource_id`s used in `translations=<id>` query params.
 * Verified against `GET /api/v4/resources/translations` (126 entries)
 * plus the versioned content-API docs at api-docs.quran.com.
 *
 * Pure Kotlin — no android.* imports (source had none).
 */
data class TranslationEdition(val id: Int, val name: String, val language: String)

val TRANSLATION_EDITIONS: List<TranslationEdition> = listOf(
    // English first (default 20 first), then Hausa, Urdu,
    // then the rest alphabetically by language.
    TranslationEdition(20, "Saheeh International", "English"),
    TranslationEdition(85, "M.A.S. Abdel Haleem", "English"),
    TranslationEdition(149, "Fadel Soliman, Bridges", "English"),
    TranslationEdition(131, "Dr. Mustafa Khattab", "English"),
    TranslationEdition(22, "A. Yusuf Ali", "English"),
    TranslationEdition(84, "Mufti Taqi Usmani", "English"),
    TranslationEdition(32, "Abubakar Mahmoud Gumi", "Hausa"),
    TranslationEdition(234, "Fatah Muhammad Jalandhari", "Urdu"),
    TranslationEdition(163, "Sheikh Mujibur Rahman", "Bengali"),
    TranslationEdition(56, "Ma Jian", "Chinese"),
    TranslationEdition(31, "Muhammad Hamidullah", "French"),
    TranslationEdition(27, "Bubenheim & Elyas", "German"),
    TranslationEdition(33, "Ministry of Religious Affairs", "Indonesian"),
    TranslationEdition(43, "Samir El-Hayek", "Portuguese"),
    TranslationEdition(45, "Elmir Kuliev", "Russian"),
    TranslationEdition(83, "Isa Garcia", "Spanish"),
    TranslationEdition(229, "Omar Sharif", "Tamil"),
    TranslationEdition(77, "Diyanet", "Turkish"),
)

fun translationNameOf(id: Int): String =
    TRANSLATION_EDITIONS.find { it.id == id }?.name ?: "Translation $id"

fun translationsByLanguage(): Map<String, List<TranslationEdition>> =
    TRANSLATION_EDITIONS.groupBy { it.language }

fun resolveTranslationId(id: Int): Int = TranslationFallback.resolveTranslationId(id)
