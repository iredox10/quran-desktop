package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.prefs.Preferences

/**
 * Persisted reading history, newest-first, capped at [MAX].
 * Backed by [java.util.prefs.Preferences] (JSON list) — mirrors the mobile
 * recently-read tracking. `verseKey == null` means a whole-page visit.
 */
object RecentlyReadStore {
    private const val MAX = 20

    data class Entry(
        val chapterId: Int,
        val chapterName: String,
        val verseKey: String? = null,
        val timestamp: Long = 0L
    )

    private val prefs: Preferences =
        Preferences.userRoot().node("recently_read")
    private val gson = Gson()
    private val type = object : TypeToken<List<Entry>>() {}.type
    private const val KEY = "items_json"

    @Synchronized
    fun record(chapterId: Int, chapterName: String, verseKey: String? = null) {
        val entries = load().toMutableList()
        entries.removeAll {
            it.chapterId == chapterId &&
                it.verseKey == verseKey &&
                isToday(it.timestamp)
        }
        entries.add(0, Entry(chapterId, chapterName, verseKey, System.currentTimeMillis()))
        save(entries.take(MAX))
    }

    @Synchronized
    fun all(): List<Entry> = load()

    @Synchronized
    fun clear() {
        prefs.remove(KEY)
        runCatching { prefs.flush() }
    }

    private fun isToday(ts: Long): Boolean {
        val cal = java.util.Calendar.getInstance()
        val day = cal.get(java.util.Calendar.DAY_OF_YEAR)
        val year = cal.get(java.util.Calendar.YEAR)
        cal.timeInMillis = ts
        return cal.get(java.util.Calendar.DAY_OF_YEAR) == day &&
            cal.get(java.util.Calendar.YEAR) == year
    }

    private fun load(): List<Entry> =
        runCatching { gson.fromJson<List<Entry>>(prefs.get(KEY, ""), type) }
            .getOrNull() ?: emptyList()

    private fun save(entries: List<Entry>) {
        prefs.put(KEY, gson.toJson(entries.filter { it.timestamp > 0L }))
        runCatching { prefs.flush() }
    }
}
