package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.prefs.Preferences

/**
 * Persisted reading/memorization sessions, backed by
 * [java.util.prefs.Preferences] as a single Gson JSON array.
 */
object SessionStore {
    private val prefs: Preferences = Preferences.userNodeForPackage(SessionStore::class.java)
    private const val KEY = "sessions_json"
    private const val MAX_ENTRIES = 2000
    private val gson = Gson()
    private val listType = object : TypeToken<List<Session>>() {}.type
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class Session(
        val date: String,
        val durationSec: Long,
        val type: String,
        val chapterId: Int?
    )

    @Synchronized
    fun log(type: String, chapterId: Int?, minutes: Int) {
        if (minutes < 1) return // sub-minute renders/back-presses carry no signal; skip them
        val today = LocalDate.now().format(dateFmt)
        val updated = all().toMutableList()
        updated.add(Session(date = today, durationSec = minutes * 60L, type = type, chapterId = chapterId))
        save(updated.takeLast(MAX_ENTRIES))
    }

    @Synchronized
    fun all(): List<Session> {
        val raw = prefs.get(KEY, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val parsed: List<Session>? = gson.fromJson(raw, listType)
            (parsed ?: emptyList()).takeLast(MAX_ENTRIES)
        }.getOrDefault(emptyList())
    }

    private fun save(sessions: List<Session>) {
        prefs.put(KEY, gson.toJson(sessions))
    }
}
