package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.desktop.ui.planner.ArchivedPlan
import com.nur.quran.shared.PlannerEngine
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
    private const val KEY_ARCHIVED_PLANS = "archived_plans"
    private const val KEY_REFLECTIONS = "reflections_json"
    private const val KEY_MARKS = "marks_json"

    private val prefs: Preferences = Preferences.userRoot().node("planner_prefs")
    private val gson = Gson()

    private val planType = object : TypeToken<ReadingPlan>() {}.type
    private val planListType = object : TypeToken<List<ReadingPlan>>() {}.type
    private val sessionsType = object : TypeToken<Map<String, Int>>() {}.type
    private val journalsType = object : TypeToken<Map<String, String>>() {}.type
    private val archivedPlansType = object : TypeToken<List<ArchivedPlan>>() {}.type
    private val reflectionsType = object : TypeToken<Map<String, String>>() {}.type
    private val marksType = object : TypeToken<Map<String, List<PlanVerseMark>>>() {}.type

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

    // ── Archived plans ──────────────────────────────────────────────

    @Synchronized
    fun getArchived(): List<ArchivedPlan> {
        val raw = prefs.get(KEY_ARCHIVED_PLANS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<ArchivedPlan>>(raw, archivedPlansType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun archivePlan(planId: String) {
        val plans = getAllPlans().toMutableList()
        val idx = plans.indexOfFirst { it.id == planId }
        if (idx == -1) return
        val plan = plans.removeAt(idx)
        saveAllPlans(plans)
        val archived = getArchived().toMutableList()
        archived.removeAll { it.plan.id == planId }
        archived.add(ArchivedPlan(plan = plan, archivedAt = PlannerEngine.formatPlannerDate()))
        prefs.put(KEY_ARCHIVED_PLANS, gson.toJson(archived))
    }

    @Synchronized
    fun restoreArchived(planId: String) {
        val archived = getArchived().toMutableList()
        val idx = archived.indexOfFirst { it.plan.id == planId }
        if (idx == -1) return
        val entry = archived.removeAt(idx)
        prefs.put(KEY_ARCHIVED_PLANS, gson.toJson(archived))
        val plans = getAllPlans().toMutableList()
        if (plans.none { it.id == planId }) {
            plans.add(entry.plan)
            saveAllPlans(plans)
        }
    }

    @Synchronized
    fun deleteArchived(planId: String) {
        val archived = getArchived().toMutableList()
        if (archived.removeAll { it.plan.id == planId }) {
            prefs.put(KEY_ARCHIVED_PLANS, gson.toJson(archived))
        }
    }

    // ── Plan-keyed reflections ("planId|day" -> text) ────────────────

    private fun getAllReflections(): Map<String, String> {
        val raw = prefs.get(KEY_REFLECTIONS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, String>>(raw, reflectionsType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    fun getReflection(planId: String, day: String): String {
        return getAllReflections()["$planId|$day"].orEmpty()
    }

    @Synchronized
    fun saveReflection(planId: String, day: String, text: String) {
        val current = getAllReflections().toMutableMap()
        val key = "$planId|$day"
        if (text.isBlank()) {
            current.remove(key)
        } else {
            current[key] = text
        }
        prefs.put(KEY_REFLECTIONS, gson.toJson(current))
    }

    // ── Plan bookmarks (planId -> verse marks) ───────────────────────

    private fun getAllMarks(): Map<String, List<PlanVerseMark>> {
        val raw = prefs.get(KEY_MARKS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, List<PlanVerseMark>>>(raw, marksType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    fun getMarks(planId: String): List<PlanVerseMark> {
        return getAllMarks()[planId] ?: emptyList()
    }

    /**
     * Toggles a verse mark for [planId]. Returns true when the mark was
     * added, false when an existing mark was removed.
     */
    @Synchronized
    fun toggleMark(planId: String, verseKey: String, surahName: String): Boolean {
        val current = getAllMarks().toMutableMap()
        val marks = current[planId].orEmpty().toMutableList()
        val idx = marks.indexOfFirst { it.verseKey == verseKey }
        val added: Boolean
        if (idx == -1) {
            marks.add(PlanVerseMark(verseKey = verseKey, surahName = surahName, createdAt = System.currentTimeMillis()))
            added = true
        } else {
            marks.removeAt(idx)
            added = false
        }
        if (marks.isEmpty()) {
            current.remove(planId)
        } else {
            current[planId] = marks
        }
        prefs.put(KEY_MARKS, gson.toJson(current))
        return added
    }
}

data class PlanVerseMark(
    val verseKey: String,
    val surahName: String,
    val createdAt: Long
)
