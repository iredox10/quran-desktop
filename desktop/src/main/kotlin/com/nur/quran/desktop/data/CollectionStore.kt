package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.prefs.Preferences

/**
 * Persisted verse collections, backed by [java.util.prefs.Preferences].
 * Mirrors the mobile Room `CollectionEntity` / `CollectionItemEntity`
 * (fields id / name / verseKey).
 */
object CollectionStore {
    private val prefs: Preferences = Preferences.userRoot().node("collection_prefs")
    private const val KEY_COLLECTIONS = "collections_json"
    private const val KEY_ITEMS = "items_json"
    private val gson = Gson()

    data class Collection(val id: String, val name: String, val createdAt: Long)

    private val collectionListType = object : TypeToken<List<Collection>>() {}.type
    private val itemsMapType = object : TypeToken<Map<String, List<String>>>() {}.type

    @Synchronized
    fun listCollections(): List<Collection> = loadCollections()

    @Synchronized
    fun createCollection(name: String): Collection {
        val collection = Collection(
            id = System.currentTimeMillis().toString(),
            name = name,
            createdAt = System.currentTimeMillis()
        )
        val updated = loadCollections() + collection
        saveCollections(updated)
        return collection
    }

    @Synchronized
    fun renameCollection(id: String, name: String) {
        saveCollections(loadCollections().map {
            if (it.id == id) it.copy(name = name) else it
        })
    }

    @Synchronized
    fun deleteCollection(id: String) {
        saveCollections(loadCollections().filterNot { it.id == id })
        val items = loadItems().toMutableMap()
        items.remove(id)
        saveItems(items)
    }

    @Synchronized
    fun items(collectionId: String): List<String> =
        loadItems()[collectionId].orEmpty()

    @Synchronized
    fun addItem(collectionId: String, verseKey: String) {
        val items = loadItems().toMutableMap()
        val current = items[collectionId].orEmpty()
        if (!current.contains(verseKey)) {
            items[collectionId] = current + verseKey
            saveItems(items)
        }
    }

    @Synchronized
    fun removeItem(collectionId: String, verseKey: String) {
        val items = loadItems().toMutableMap()
        items[collectionId] = items[collectionId].orEmpty().filterNot { it == verseKey }
        saveItems(items)
    }

    private fun loadCollections(): List<Collection> {
        val raw = prefs.get(KEY_COLLECTIONS, "")
        if (raw.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<Collection>>(raw, collectionListType).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCollections(collections: List<Collection>) {
        prefs.put(KEY_COLLECTIONS, gson.toJson(collections))
    }

    private fun loadItems(): Map<String, List<String>> {
        val raw = prefs.get(KEY_ITEMS, "")
        if (raw.isBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, List<String>>>(raw, itemsMapType).orEmpty()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveItems(items: Map<String, List<String>>) {
        prefs.put(KEY_ITEMS, gson.toJson(items))
    }
}
