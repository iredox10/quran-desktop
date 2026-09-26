package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

private data class TafsirRow(
    @SerializedName("verse_key") val verseKey: String = "",
    val text: String = ""
)

/**
 * Offline Ibn Kathir tafsir, lazy-loaded from the bundled
 * `data/tafsir_ibn_kathir.json` (list of {verse_key, text(html), resource_id}).
 */
object TafsirStore {
    private var map: Map<String, String>? = null

    fun get(verseKey: String): String? {
        if (map == null) {
            map = try {
                val stream = TafsirStore::class.java.getResourceAsStream("/data/tafsir_ibn_kathir.json")
                    ?: return null
                val rows: List<TafsirRow> = stream.bufferedReader(Charsets.UTF_8).use {
                    Gson().fromJson(it, object : TypeToken<List<TafsirRow>>() {}.type)
                }
                rows.associate { it.verseKey to it.text }
            } catch (_: Exception) {
                emptyMap()
            }
        }
        return map?.get(verseKey)?.takeIf { it.isNotBlank() }
    }
}
