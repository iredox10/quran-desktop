package com.nur.quran.desktop

import java.util.prefs.Preferences

/**
 * Desktop preferences backed by [java.util.prefs.Preferences].
 *
 * Keys mirror the Android SharedPreferences / DataStore keys so behaviour
 * stays consistent across platforms. Defaults match the mobile app.
 */
object PrefsCache {
    private const val KEY_RECITER = "reciter"
    private const val KEY_TRANSLATION = "translation"
    private const val KEY_MUSHAF = "mushaf"
    private const val KEY_FONT = "font"
    private const val KEY_TAJWEED_ENABLED = "tajweed_enabled"
    private const val KEY_LAST_CHAPTER_ID = "last_chapter_id"
    private const val KEY_LAST_CHAPTER_NAME = "last_chapter_name"
    private const val KEY_LAST_VERSE_KEY = "last_verse_key"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    private const val KEY_DARK_THEME = "dark_theme"

    private val prefs: Preferences =
        Preferences.userNodeForPackage(PrefsCache::class.java)

    // --- reciter ---
    fun getReciter(): String = prefs.get(KEY_RECITER, "mishary")
    fun putReciter(value: String) = prefs.put(KEY_RECITER, value)

    // --- translation ---
    fun getTranslation(): String = prefs.get(KEY_TRANSLATION, "en_sahih")
    fun putTranslation(value: String) = prefs.put(KEY_TRANSLATION, value)

    // --- mushaf ---
    fun getMushaf(): String = prefs.get(KEY_MUSHAF, "uthmani")
    fun putMushaf(value: String) = prefs.put(KEY_MUSHAF, value)

    // --- font ---
    fun getFont(): String = prefs.get(KEY_FONT, "KFGQPC Hafs")
    fun putFont(value: String) = prefs.put(KEY_FONT, value)

    // --- tajweedEnabled ---
    fun getTajweedEnabled(): Boolean = prefs.getBoolean(KEY_TAJWEED_ENABLED, true)
    fun putTajweedEnabled(value: Boolean) = prefs.putBoolean(KEY_TAJWEED_ENABLED, value)

    /** Last-read position (drives the home Continue card). */
    data class LastRead(
        val chapterId: Int,
        val chapterName: String,
        val verseKey: String?,
        val timestamp: Long
    )

    fun getLastRead(): LastRead? {
        val id = prefs.getInt(KEY_LAST_CHAPTER_ID, -1)
        if (id < 0) return null
        return LastRead(
            chapterId = id,
            chapterName = prefs.get(KEY_LAST_CHAPTER_NAME, "Surah $id") ?: "Surah $id",
            verseKey = prefs.get(KEY_LAST_VERSE_KEY, null),
            timestamp = prefs.getLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
        )
    }

    fun putLastRead(chapterId: Int, chapterName: String, verseKey: String? = null) {
        prefs.putInt(KEY_LAST_CHAPTER_ID, chapterId)
        prefs.put(KEY_LAST_CHAPTER_NAME, chapterName)
        if (verseKey == null) prefs.remove(KEY_LAST_VERSE_KEY) else prefs.put(KEY_LAST_VERSE_KEY, verseKey)
        prefs.putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
    }

    // --- dark theme ---
    fun getDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, false)
    fun putDarkTheme(value: Boolean) = prefs.putBoolean(KEY_DARK_THEME, value)

    // --- reader scales (mirror Android SettingsDrawer sliders/switches) ---
    private const val KEY_ARABIC_SCALE = "arabic_scale"
    private const val KEY_TRANSLATION_SCALE = "translation_scale"
    private const val KEY_LINE_HEIGHT_MULT = "line_height_mult"
    private const val KEY_TRANSLATION_ENABLED = "translation_enabled"

    fun getArabicScale(): Float = prefs.getFloat(KEY_ARABIC_SCALE, 1f)
    fun putArabicScale(value: Float) = prefs.putFloat(KEY_ARABIC_SCALE, value)
    fun getTranslationScale(): Float = prefs.getFloat(KEY_TRANSLATION_SCALE, 1f)
    fun putTranslationScale(value: Float) = prefs.putFloat(KEY_TRANSLATION_SCALE, value)
    fun getLineHeightMult(): Float = prefs.getFloat(KEY_LINE_HEIGHT_MULT, 1f)
    fun putLineHeightMult(value: Float) = prefs.putFloat(KEY_LINE_HEIGHT_MULT, value)
    fun getReaderTranslationEnabled(): Boolean = prefs.getBoolean(KEY_TRANSLATION_ENABLED, true)
    fun putReaderTranslationEnabled(value: Boolean) = prefs.putBoolean(KEY_TRANSLATION_ENABLED, value)

    /** Clears all cached desktop prefs (used by tests / reset-to-defaults). */
    fun clear() = prefs.clear()
}
