package com.nur.quran.desktop.data

import java.util.prefs.Preferences

/**
 * Persisted verse bookmarks (set of verseKeys like "2:255"), backed by
 * [java.util.prefs.Preferences]. Mirrors the mobile bookmark toggle.
 */
object BookmarkStore {
    private val prefs: Preferences = Preferences.userNodeForPackage(BookmarkStore::class.java)
    private const val KEY = "bookmarked_verse_keys"

    @Synchronized
    fun isBookmarked(verseKey: String): Boolean = load().contains(verseKey)

    @Synchronized
    fun toggle(verseKey: String): Boolean {
        val set = load().toMutableSet()
        val now = if (set.contains(verseKey)) {
            set.remove(verseKey)
            false
        } else {
            set.add(verseKey)
            true
        }
        save(set)
        return now
    }

    @Synchronized
    fun all(): Set<String> = load()

    private fun load(): Set<String> =
        prefs.get(KEY, "").split(",").filter { it.isNotBlank() }.toSet()

    private fun save(set: Set<String>) {
        prefs.put(KEY, set.joinToString(","))
    }
}
