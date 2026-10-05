package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.prefs.Preferences

/**
 * LOCAL-only group-khatmah (Sauka) persistence for the desktop app.
 *
 * The web app backs Sauka groups with the cloud; the desktop edition keeps
 * groups on-device only. Backed by [Preferences] node `"sauka_prefs"`.
 *
 * Units: juz 1..30, hizb 1..60 (stored as plain number strings).
 */
data class SaukaGroup(
    val id: String = "",
    val title: String = "",
    val intention: String = "",
    val division: String = "juz",
    val deadline: String = "",
    val members: List<String> = emptyList(),
    val claims: Map<String, String> = emptyMap(),
    val doneUnits: List<String> = emptyList(),
    val createdAt: Long = 0L
)

object SaukaStore {
    private const val KEY_GROUPS = "groups"

    private val prefs: Preferences = Preferences.userRoot().node("sauka_prefs")
    private val gson = Gson()

    private val listType = object : TypeToken<List<SaukaGroup>>() {}.type

    /** Total units for a division ("juz" -> 30, anything else -> 60). */
    fun totalUnits(division: String): Int = if (division == "hizb") 60 else 30

    /** Unit ids for a division: "1".."30" (juz) or "1".."60" (hizb). */
    fun unitsFor(division: String): List<String> =
        (1..totalUnits(division)).map { it.toString() }

    @Synchronized
    fun listGroups(): List<SaukaGroup> {
        val raw = prefs.get(KEY_GROUPS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<SaukaGroup>>(raw, listType).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getGroup(groupId: String): SaukaGroup? =
        listGroups().firstOrNull { it.id == groupId }

    @Synchronized
    fun createGroup(
        title: String,
        intention: String,
        division: String,
        deadline: String
    ): SaukaGroup {
        val group = SaukaGroup(
            id = "sauka-" + System.currentTimeMillis(),
            title = title.trim(),
            intention = intention.trim(),
            division = if (division == "hizb") "hizb" else "juz",
            deadline = deadline.trim(),
            createdAt = System.currentTimeMillis()
        )
        saveAll(listGroups() + group)
        return group
    }

    @Synchronized
    fun deleteGroup(groupId: String) {
        saveAll(listGroups().filterNot { it.id == groupId })
    }

    @Synchronized
    fun claimUnit(groupId: String, unitId: String, member: String) {
        update(groupId) { g ->
            val name = member.trim().ifBlank { return@update g }
            g.copy(
                members = if (g.members.contains(name)) g.members else g.members + name,
                claims = g.claims + (unitId to name)
            )
        }
    }

    @Synchronized
    fun unclaimUnit(groupId: String, unitId: String) {
        update(groupId) { g ->
            g.copy(
                claims = g.claims - unitId,
                doneUnits = g.doneUnits - unitId
            )
        }
    }

    @Synchronized
    fun toggleDone(groupId: String, unitId: String) {
        update(groupId) { g ->
            g.copy(
                doneUnits = if (g.doneUnits.contains(unitId)) g.doneUnits - unitId
                else g.doneUnits + unitId
            )
        }
    }

    @Synchronized
    fun addMember(groupId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        update(groupId) { g ->
            if (g.members.contains(trimmed)) g
            else g.copy(members = g.members + trimmed)
        }
    }

    private inline fun update(groupId: String, transform: (SaukaGroup) -> SaukaGroup) {
        saveAll(listGroups().map { if (it.id == groupId) transform(it) else it })
    }

    private fun saveAll(groups: List<SaukaGroup>) {
        prefs.put(KEY_GROUPS, gson.toJson(groups))
    }
}
