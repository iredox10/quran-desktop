package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.desktop.PrefsCache
import com.nur.quran.shared.HifdhStore
import java.io.File
import java.util.prefs.Preferences

/**
 * Local backup / restore for the desktop edition.
 *
 * There is no Appwrite cloud sync on desktop, so this store exports every
 * [Preferences] node used by the app as `{ nodePath: { key: value } }` JSON
 * (all values as strings — [Preferences.get] already returns the string form
 * of booleans / ints / floats, and [Preferences.put] restores them so the
 * typed getters keep working). Import reverses the process.
 */
object BackupStore {
    private val gson = Gson()
    private val backupType = object : TypeToken<Map<String, Map<String, String>>>() {}.type

    /** Exact nodes used by the app, resolved from the owning classes. */
    private fun backupNodes(): List<Preferences> = listOf(
        Preferences.userNodeForPackage(PrefsCache::class.java),
        Preferences.userNodeForPackage(BookmarkStore::class.java),
        Preferences.userNodeForPackage(SessionStore::class.java),
        Preferences.userRoot().node("planner_prefs"),
        Preferences.userRoot().node("session_prefs"),
        Preferences.userRoot().node("collection_prefs"),
        Preferences.userRoot().node("profile_prefs"),
        Preferences.userNodeForPackage(HifdhStore::class.java).node("hifdh_settings")
    )

    fun exportToFile(file: File): Boolean {
        return try {
            val dump = LinkedHashMap<String, Map<String, String>>()
            // BookmarkStore and SessionStore share one package node — dedupe by path.
            backupNodes().map { it.absolutePath() }.distinct().forEach { path ->
                val node = Preferences.userRoot().node(path)
                val entries = LinkedHashMap<String, String>()
                node.keys().forEach { key ->
                    entries[key] = node.get(key, "")
                }
                dump[path] = entries
            }
            file.writeText(gson.toJson(dump))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun importFromFile(file: File): Boolean {
        return try {
            val dump: Map<String, Map<String, String>> =
                gson.fromJson(file.readText(), backupType) ?: return false
            dump.forEach { (path, entries) ->
                val node = Preferences.userRoot().node(path)
                entries.forEach { (key, value) -> node.put(key, value) }
                node.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
