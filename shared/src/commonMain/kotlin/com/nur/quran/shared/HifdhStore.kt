package com.nur.quran.shared

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

/**
 * Desktop persistence for hifdh (memorization) state, mirroring the Android
 * `HifdhStore` (`hifdhHistory`, `transitionLinks`, `hifdhGoals`).
 *
 * History and goals are kept as JSON strings; transition links as a JSON
 * string array. Backed by `java.util.prefs.Preferences` under the
 * "hifdh_settings" node. No Android dependencies.
 *
 * [HifdhHistoryEntry] and [HifdhGoal] are reused from [FsrsScheduler.kt]
 * (same package) — they are `@Serializable` there.
 */
object HifdhStore {

    private val prefs: Preferences =
        Preferences.userNodeForPackage(HifdhStore::class.java).node("hifdh_settings")

    private val json = Json { ignoreUnknownKeys = true }

    private val historySerializer = MapSerializer(String.serializer(), HifdhHistoryEntry.serializer())
    private val linksSerializer = SetSerializer(String.serializer())
    private val goalsSerializer = ListSerializer(HifdhGoal.serializer())

    fun loadHifdhHistory(): Map<String, HifdhHistoryEntry> {
        val raw = prefs.get(KEY_HISTORY, null) ?: return emptyMap()
        return runCatching { json.decodeFromString(historySerializer, raw) }.getOrNull() ?: emptyMap()
    }

    fun saveHifdhHistory(history: Map<String, HifdhHistoryEntry>) {
        prefs.put(KEY_HISTORY, json.encodeToString(historySerializer, history))
        runCatching { prefs.flush() }
    }

    fun loadTransitionLinks(): Set<String> {
        val raw = prefs.get(KEY_TRANSITION_LINKS, null) ?: return emptySet()
        return runCatching { json.decodeFromString(linksSerializer, raw) }.getOrNull() ?: emptySet()
    }

    fun saveTransitionLinks(links: Set<String>) {
        prefs.put(KEY_TRANSITION_LINKS, json.encodeToString(linksSerializer, links))
        runCatching { prefs.flush() }
    }

    fun loadHifdhGoals(): List<HifdhGoal> {
        val raw = prefs.get(KEY_GOALS, null) ?: return emptyList()
        return runCatching { json.decodeFromString(goalsSerializer, raw) }.getOrNull() ?: emptyList()
    }

    fun saveHifdhGoals(goals: List<HifdhGoal>) {
        prefs.put(KEY_GOALS, json.encodeToString(goalsSerializer, goals))
        runCatching { prefs.flush() }
    }

    private const val KEY_HISTORY = "hifdh_history"
    private const val KEY_TRANSITION_LINKS = "transition_links"
    private const val KEY_GOALS = "hifdh_goals"
}
