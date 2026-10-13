package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.desktop.ui.planner.ArchivedPlan
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import java.io.File
import java.util.prefs.Preferences

/**
 * Web `prayerSettings` parity: which prayers reading is distributed across,
 * when to read relative to prayer, and the pre-reading intention prompt flag.
 */
data class PrayerPrefs(
    val activePrayers: List<String>,
    /** "after" | "before" | "split" */
    val readPreference: String,
    val intentionPromptEnabled: Boolean
) {
    companion object {
        val DEFAULT = PrayerPrefs(
            activePrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            readPreference = "after",
            intentionPromptEnabled = true
        )
    }
}

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
    private const val KEY_PRAYER_PREFS = "prayer_prefs"

    private val prefs: Preferences = Preferences.userRoot().node("planner_prefs")
    private val gson = Gson()

    /**
     * Plan JSON routinely exceeds the 8192-char limit of `java.util.prefs`
     * values (a page plan serializes ~50 KB with every item embedded), which
     * made `Preferences.put` throw and silently killed plan persistence. All
     * JSON blobs therefore live in `~/.quran-nur/planner_<key>.json`; the
     * legacy prefs value is still read as a migration fallback.
     */
    private val storeDir: File = File(System.getProperty("user.home"), ".quran-nur").apply { mkdirs() }

    private fun putJson(key: String, value: String?) {
        val file = File(storeDir, "planner_$key.json")
        if (value.isNullOrBlank()) {
            file.delete()
            runCatching { prefs.remove(key) }
        } else {
            file.writeText(value, Charsets.UTF_8)
        }
    }

    private fun getJson(key: String): String? {
        val file = File(storeDir, "planner_$key.json")
        if (file.isFile) return file.readText(Charsets.UTF_8)
        return prefs.get(key, null)
    }

    private val planType = object : TypeToken<ReadingPlan>() {}.type
    private val planListType = object : TypeToken<List<ReadingPlan>>() {}.type
    private val sessionsType = object : TypeToken<Map<String, Int>>() {}.type
    private val journalsType = object : TypeToken<Map<String, String>>() {}.type
    private val archivedPlansType = object : TypeToken<List<ArchivedPlan>>() {}.type
    private val reflectionsType = object : TypeToken<Map<String, String>>() {}.type
    private val marksType = object : TypeToken<Map<String, List<PlanVerseMark>>>() {}.type
    private val prayerPrefsType = object : TypeToken<PrayerPrefs>() {}.type

    /** Web `prayerSettings` (activePrayers / readPreference / intentionPrompt). */
    @Synchronized
    fun getPrayerPrefs(): PrayerPrefs {
        val raw = getJson(KEY_PRAYER_PREFS) ?: return PrayerPrefs.DEFAULT
        return try {
            gson.fromJson<PrayerPrefs>(raw, prayerPrefsType) ?: PrayerPrefs.DEFAULT
        } catch (_: Exception) {
            PrayerPrefs.DEFAULT
        }
    }

    @Synchronized
    fun savePrayerPrefs(p: PrayerPrefs) {
        putJson(KEY_PRAYER_PREFS, gson.toJson(p, prayerPrefsType))
        runCatching { prefs.flush() }
    }

    @Synchronized
    fun getActivePlan(): ReadingPlan? {
        val raw = getJson(KEY_ACTIVE_PLAN) ?: return null
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
            putJson(KEY_ACTIVE_PLAN, null)
        } else {
            putJson(KEY_ACTIVE_PLAN, gson.toJson(plan))
        }
    }

    @Synchronized
    fun getAllPlans(): List<ReadingPlan> {
        val raw = getJson(KEY_ALL_PLANS) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<ReadingPlan>>(raw, planListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveAllPlans(plans: List<ReadingPlan>) {
        putJson(KEY_ALL_PLANS, gson.toJson(plans))
    }

    @Synchronized
    fun logStudySession(dateStr: String, minutes: Int) {
        val current = getStudySessions().toMutableMap()
        current[dateStr] = (current[dateStr] ?: 0) + minutes
        putJson(KEY_SESSIONS, gson.toJson(current))
    }

    @Synchronized
    fun getStudySessions(): Map<String, Int> {
        val raw = getJson(KEY_SESSIONS) ?: return emptyMap()
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
        putJson(KEY_JOURNALS, gson.toJson(current))
    }

    private fun getAllJournals(): Map<String, String> {
        val raw = getJson(KEY_JOURNALS) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, String>>(raw, journalsType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ── Archived plans ──────────────────────────────────────────────

    // Web parity (useAppStore archivePlanner): archived entries carry an ISO
    // timestamp; blank stamps fall back to entry order. Newest first.
    @Synchronized
    fun getArchived(): List<ArchivedPlan> {
        val raw = getJson(KEY_ARCHIVED_PLANS) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val parsed: List<ArchivedPlan> =
                gson.fromJson<List<ArchivedPlan>>(raw, archivedPlansType) ?: emptyList()
            parsed.sortedByDescending { it.archivedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Web parity (useAppStore archivePlanner): archive [planId], falling back
     * to the saved active plan like web's `planId || activePlannerId`. Keeps
     * the newest archived first and refreshes the saved active plan id.
     */
    @Synchronized
    fun archivePlan(planId: String) {
        val targetId = planId.ifBlank { getActivePlan()?.id.orEmpty() }
        val plans = getAllPlans().toMutableList()
        val idx = plans.indexOfFirst { it.id == targetId }
        val active = getActivePlan()
        val plan = when {
            idx != -1 -> plans.removeAt(idx)
            active != null && active.id == targetId -> active
            active != null && targetId.isBlank() -> {
                plans.removeAll { it.id == active.id }
                active
            }
            else -> return
        }
        saveAllPlans(plans)
        val archived = getArchived().toMutableList()
        archived.removeAll { it.plan.id == plan.id }
        archived.add(0, ArchivedPlan(plan = plan, archivedAt = PlannerEngine.formatPlannerDate()))
        putJson(KEY_ARCHIVED_PLANS, gson.toJson(archived))
        if (getActivePlan()?.id == plan.id) {
            saveActivePlan(plans.firstOrNull())
        }
    }

    @Synchronized
    fun restoreArchived(planId: String) {
        val archived = getArchived().toMutableList()
        val idx = archived.indexOfFirst { it.plan.id == planId }
        if (idx == -1) return
        val entry = archived.removeAt(idx)
        putJson(KEY_ARCHIVED_PLANS, gson.toJson(archived))
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
            putJson(KEY_ARCHIVED_PLANS, gson.toJson(archived))
        }
    }

    // ── Plan-keyed reflections ("planId|day" -> text) ────────────────

    private fun getAllReflections(): Map<String, String> {
        val raw = getJson(KEY_REFLECTIONS) ?: return emptyMap()
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
        putJson(KEY_REFLECTIONS, gson.toJson(current))
    }

    // ── Plan bookmarks (planId -> verse marks) ───────────────────────

    private fun getAllMarks(): Map<String, List<PlanVerseMark>> {
        val raw = getJson(KEY_MARKS) ?: return emptyMap()
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
        putJson(KEY_MARKS, gson.toJson(current))
        return added
    }
}

data class PlanVerseMark(
    val verseKey: String,
    val surahName: String,
    val createdAt: Long
)
