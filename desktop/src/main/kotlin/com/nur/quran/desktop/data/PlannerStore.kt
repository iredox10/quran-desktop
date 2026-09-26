package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.shared.ReadingPlan
import java.util.prefs.Preferences

/**
 * Desktop planner persistence, mirroring the Android `PlannerViewModel`
 * SharedPreferences keys. Backed by [Preferences] node `"planner_prefs"`.
 */
object PlannerStore {
    private const val KEY_ACTIVE_PLAN = "active_plan"
    private const val KEY_ALL_PLANS = "all_plans"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_JOURNALS = "journals"

    private val prefs: Preferences = Preferences.userRoot().node("planner_prefs")
    private val gson = Gson()

    private val planType = object : TypeToken<ReadingPlan>() {}.type
    private val planListType = object : TypeToken<List<ReadingPlan>>() {}.type
    private val sessionsType = object : TypeToken<Map<String, Int>>() {}.type
    private val journalsType = object : TypeToken<Map<String, String>>() {}.type

    @Synchronized
    fun getActivePlan(): ReadingPlan? {
        val raw = prefs.get(KEY_ACTIVE_PLAN, null) ?: return null
        if (raw.isBlank()) return null
        return try {
            gson.fromJson<ReadingPlan>(raw, planType)
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun saveActivePlan(plan: ReadingPlan?) {
        if (plan == null) {
            prefs.remove(KEY_ACTIVE_PLAN)
        } else {
            prefs.put(KEY_ACTIVE_PLAN, gson.toJson(plan))
        }
    }

    @Synchronized
    fun getAllPlans(): List<ReadingPlan> {
        val raw = prefs.get(KEY_ALL_PLANS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<ReadingPlan>>(raw, planListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveAllPlans(plans: List<ReadingPlan>) {
        prefs.put(KEY_ALL_PLANS, gson.toJson(plans))
    }

    @Synchronized
    fun logStudySession(dateStr: String, minutes: Int) {
        val current = getStudySessions().toMutableMap()
        current[dateStr] = (current[dateStr] ?: 0) + minutes
        prefs.put(KEY_SESSIONS, gson.toJson(current))
    }

    @Synchronized
    fun getStudySessions(): Map<String, Int> {
        val raw = prefs.get(KEY_SESSIONS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, Int>>(raw, sessionsType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    fun getJournal(dateStr: String): String {
        return getAllJournals()[dateStr].orEmpty()
    }

    @Synchronized
    fun saveJournal(dateStr: String, text: String) {
        val current = getAllJournals().toMutableMap()
        if (text.isBlank()) {
            current.remove(dateStr)
        } else {
            current[dateStr] = text
        }
        prefs.put(KEY_JOURNALS, gson.toJson(current))
    }

    private fun getAllJournals(): Map<String, String> {
        val raw = prefs.get(KEY_JOURNALS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, String>>(raw, journalsType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
