package com.nur.quran.shared

/**
 * Translation fallback mechanisms, ported from quran-kotlin
 * .../data/TranslationFallback.kt to pure common Kotlin.
 *
 * Stripped for commonMain (all JVM/Android-only APIs removed):
 * - Gson / `loadFromJson(InputStream)` — platform code should parse JSON and
 *   call [loadTranslations] / [loadOfflineList] instead.
 * - `java.io.File` auto-load (`tryAutoLoad`) — desktop lookup of
 *   `app/src/main/assets/...` paths makes no sense outside the Android repo.
 * - `java.util.concurrent.ConcurrentHashMap` / `@Volatile` — replaced with a
 *   plain LinkedHashMap (single-threaded access; platform can synchronize).
 * - `ApiVerse` / `VerseEntity` overloads (`resolveTranslation(ApiVerse)`,
 *   `backfillBlankTranslations`) — depend on Android-module entity types;
 *   keep the primitive [resolveTranslation] overload which carries the logic.
 *
 * Kept: [FALLBACK_TRANSLATION_ID], [resolveTranslationId], offline seed cache,
 * [loadTranslations], [getOfflineTranslation], [resolveTranslation].
 */

/** One offline fallback verse. Platform JSON loaders map into this. */
data class OfflineVerse(val verseKey: String, val translation: String?)

object TranslationFallback {

    /** Default fallback translation ID: 20 (Saheeh International). */
    const val FALLBACK_TRANSLATION_ID = 20

    /**
     * Maps translation ID to a supported edition.
     * Translation 131 (Khattab) is absent from the live v4 API and returns no verses,
     * so it maps to the default fallback translation ID (20).
     */
    fun resolveTranslationId(translationId: Int): Int {
        return if (translationId == 131) FALLBACK_TRANSLATION_ID else translationId
    }

    /**
     * In-memory cache of offline translations mapped by verse key (e.g., "1:1").
     */
    private val offlineTranslations = LinkedHashMap<String, String>()

    private var isLoaded = false

    init {
        // Seed first surah (Al-Fatihah) as guaranteed immediate fallback
        offlineTranslations["1:1"] = "In the name of God, the Lord of Mercy, the Giver of Mercy!"
        offlineTranslations["1:2"] = "Praise belongs to God, Lord of the Worlds,"
        offlineTranslations["1:3"] = "the Lord of Mercy, the Giver of Mercy,"
        offlineTranslations["1:4"] = "Master of the Day of Judgement."
        offlineTranslations["1:5"] = "It is You we worship; it is You we ask for help."
        offlineTranslations["1:6"] = "Guide us to the straight path:"
        offlineTranslations["1:7"] = "the path of those You have blessed, those who incur no anger and who have not gone astray."
    }

    /**
     * Seeds or overrides offline translations in the cache.
     */
    fun loadTranslations(translations: Map<String, String>) {
        offlineTranslations.putAll(translations)
        isLoaded = true
    }

    /**
     * Seeds or overrides offline translations from a parsed list.
     * Replaces the Gson-based `loadFromJson(InputStream)` from the Android
     * source: platform code parses JSON with its own decoder and calls this.
     */
    fun loadOfflineList(items: List<OfflineVerse>) {
        items.forEach { item ->
            if (!item.translation.isNullOrBlank()) {
                offlineTranslations[item.verseKey] = item.translation
            }
        }
        isLoaded = true
    }

    fun isOfflineLoaded(): Boolean = isLoaded

    /**
     * Retrieves the offline fallback translation text for a given verse key (e.g. "1:1").
     */
    fun getOfflineTranslation(verseKey: String): String? =
        offlineTranslations[verseKey]

    /**
     * Resolves the translation for a verse key and nullable API translation text.
     * If the API translation is present and non-blank, returns it; otherwise,
     * falls back to the offline translation for the verse key.
     */
    fun resolveTranslation(verseKey: String, apiTranslationText: String?): String? {
        if (!apiTranslationText.isNullOrBlank()) {
            return apiTranslationText
        }
        return getOfflineTranslation(verseKey)
    }
}
