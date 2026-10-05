package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.prefs.Preferences

/**
 * Desktop pomodoro persistence, mirroring the WEB planner's pomodoro system
 * (FloatingPomodoro / PomodoroWidget / ConfigModal + useAppStore pomodoro state).
 *
 * Backed by [Preferences] node `"pomodoro_prefs"`. All complex values are
 * stored as Gson JSON strings.
 */
object PomodoroStore {
    private val prefs: Preferences = Preferences.userRoot().node("pomodoro_prefs")
    private val gson = Gson()

    private const val KEY_PROFILES = "profiles"
    private const val KEY_ACTIVE = "active_profile"
    private const val KEY_DAILY_GOAL = "daily_goal"
    private const val KEY_HISTORY = "history"

    private const val DEFAULT_DAILY_GOAL = 4
    private const val MAX_HISTORY = 500

    private val profilesType = object : TypeToken<List<PomodoroProfile>>() {}.type
    private val historyType = object : TypeToken<List<HistoryEntry>>() {}.type

    data class PomodoroProfile(
        val id: String,
        val name: String,
        val focusMinutes: Int,
        val breakMinutes: Int
    )

    data class HistoryEntry(
        val date: String,
        val durationSec: Long,
        val mode: String,
        val completedAt: Long
    )

    private fun defaultProfiles(): List<PomodoroProfile> = listOf(
        PomodoroProfile(id = "reading-focus", name = "Reading Focus", focusMinutes = 25, breakMinutes = 5),
        PomodoroProfile(id = "deep-study", name = "Deep Study", focusMinutes = 45, breakMinutes = 10),
        PomodoroProfile(id = "hifz-sprint", name = "Hifz Sprint", focusMinutes = 15, breakMinutes = 3)
    )

    @Synchronized
    fun profiles(): List<PomodoroProfile> {
        val raw = prefs.get(KEY_PROFILES, null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultProfiles()
            prefs.put(KEY_PROFILES, gson.toJson(defaults))
            return defaults
        }
        return try {
            val parsed: List<PomodoroProfile>? = gson.fromJson(raw, profilesType)
            if (parsed.isNullOrEmpty()) defaultProfiles() else parsed
        } catch (_: Exception) {
            defaultProfiles()
        }
    }

    /** Upsert: replaces the profile with the same id, or appends it. */
    @Synchronized
    fun saveProfile(profile: PomodoroProfile) {
        val updated = profiles().toMutableList()
        val idx = updated.indexOfFirst { it.id == profile.id }
        if (idx >= 0) updated[idx] = profile else updated.add(profile)
        prefs.put(KEY_PROFILES, gson.toJson(updated))
    }

    /**
     * Deletes a profile by id. At least one profile is always kept: deleting
     * the last remaining profile is a no-op, and if the active profile is
     * deleted the first remaining profile becomes active.
     */
    @Synchronized
    fun deleteProfile(id: String) {
        val current = profiles()
        if (current.size <= 1) return
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return
        prefs.put(KEY_PROFILES, gson.toJson(updated))
        if (getActiveId() == id) {
            prefs.put(KEY_ACTIVE, updated.first().id)
        }
    }

    @Synchronized
    fun setActive(id: String) {
        if (profiles().any { it.id == id }) {
            prefs.put(KEY_ACTIVE, id)
        }
    }

    @Synchronized
    fun getActiveId(): String {
        val saved = prefs.get(KEY_ACTIVE, null)
        val all = profiles()
        if (!saved.isNullOrBlank() && all.any { it.id == saved }) return saved
        return all.first().id
    }

    @Synchronized
    fun getActive(): PomodoroProfile {
        val id = getActiveId()
        return profiles().firstOrNull { it.id == id } ?: defaultProfiles().first()
    }

    @Synchronized
    fun getDailyGoal(): Int {
        val v = prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
        return if (v < 1) DEFAULT_DAILY_GOAL else v
    }

    @Synchronized
    fun putDailyGoal(value: Int) {
        prefs.putInt(KEY_DAILY_GOAL, value.coerceAtLeast(1))
    }

    @Synchronized
    fun logSession(date: String, durationSec: Long, mode: String) {
        val updated = history().toMutableList()
        updated.add(
            HistoryEntry(
                date = date,
                durationSec = durationSec,
                mode = mode,
                completedAt = System.currentTimeMillis()
            )
        )
        prefs.put(KEY_HISTORY, gson.toJson(updated.takeLast(MAX_HISTORY)))
    }

    @Synchronized
    fun history(): List<HistoryEntry> {
        val raw = prefs.get(KEY_HISTORY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val parsed: List<HistoryEntry>? = gson.fromJson(raw, historyType)
            (parsed ?: emptyList()).takeLast(MAX_HISTORY)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Number of completed focus sessions on [dateStr] (`yyyy-MM-dd`). */
    @Synchronized
    fun todayCount(dateStr: String): Int =
        history().count { it.date == dateStr && it.mode == "focus" }
}
