package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Offline-first translation editions for desktop.
 *
 * Edition catalog mirrors Android `TRANSLATION_EDITIONS`
 * (`data/translation/Translations.kt`, default id 20 Saheeh International).
 *
 * Endpoint mirrors what Android's `TranslationPackManager` actually uses —
 * note Android has NO dedicated `quran/translations/{id}` endpoint.
 * Translation texts arrive inside the verses response:
 *
 *   GET https://api.quran.com/api/v4/verses/by_chapter/{chapterId}
 *       ?translations={resourceId}&words=false&per_page=300&page=N
 *
 * Response shape:
 * `{verses:[{verse_key, translations:[{resource_id, text}]}],
 *   pagination:{current_page, next_page, total_pages}}`.
 * Pages are followed via `pagination.next_page` (one page covers the longest
 * chapter; the loop is a safety net, same as Android, max 10 pages).
 *
 * Resource id 20 (Saheeh International) ships bundled with the app
 * (`data/quran_full.json`), so [getChapter] returns null for it ("use
 * bundled") and never hits the network. The desktop prefs key `translation`
 * stores the resource id as a string (e.g. "20"); see `PrefsCache`.
 *
 * Network I/O runs on the calling thread — call from a background dispatcher.
 * Only Gson + java.net are used (no new Gradle deps).
 */
object TranslationStore {
    /** Quran.com v4 base URL (mirrors Android `AppModule`). */
    const val BASE_URL = "https://api.quran.com/api/v4/"

    /** Bundled edition — [getChapter] returns null ("use bundled") for this id. */
    const val BUNDLED_ID = 20

    /**
     * Known editions, copied from Android `TRANSLATION_EDITIONS`
     * (id + display name; language dropped — the UI shows name + id).
     */
    val KNOWN_EDITIONS: List<Pair<Int, String>> = listOf(
        20 to "Saheeh International",
        85 to "M.A.S. Abdel Haleem",
        149 to "Fadel Soliman, Bridges",
        131 to "Dr. Mustafa Khattab",
        22 to "A. Yusuf Ali",
        84 to "Mufti Taqi Usmani",
        32 to "Abubakar Mahmoud Gumi",
        234 to "Fatah Muhammad Jalandhari",
        163 to "Sheikh Mujibur Rahman",
        56 to "Ma Jian",
        31 to "Muhammad Hamidullah",
        27 to "Bubenheim & Elyas",
        33 to "Ministry of Religious Affairs",
        43 to "Samir El-Hayek",
        45 to "Elmir Kuliev",
        83 to "Isa Garcia",
        229 to "Omar Sharif",
        77 to "Diyanet",
    )

    fun nameOf(id: Int): String =
        KNOWN_EDITIONS.find { it.first == id }?.second ?: "Translation $id"

    private const val PER_PAGE = 300
    private const val MAX_PAGES = 10

    private val gson = Gson()
    private val memory = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()

    private fun dir(): File = File(System.getProperty("user.home"), ".quran-nur/translations")

    private fun fileFor(chapterId: Int, resourceId: Int): File =
        File(dir(), "${resourceId}_${chapterId}.json")

    /**
     * Verse key ("1:1") -> translation text for [chapterId] in edition
     * [resourceId]. Null means "use the bundled translation" ([BUNDLED_ID])
     * or fetch/parse failure. Cache-first:
     * `~/.quran-nur/translations/{resourceId}_{chapter}.json`.
     */
    fun getChapter(chapterId: Int, resourceId: Int): Map<String, String>? {
        if (resourceId == BUNDLED_ID) return null
        if (chapterId !in 1..114) return null
        val key = "${resourceId}_$chapterId"
        memory[key]?.let { return it }
        readCache(chapterId, resourceId)?.let {
            memory[key] = it
            return it
        }
        return try {
            val verses = fetchAllVerses(chapterId, resourceId)
            val map = LinkedHashMap<String, String>(verses.size)
            for (v in verses) {
                // Prefer the exact edition; fall back to the first text (mirrors Android).
                val text = v.translations.firstOrNull { it.resourceId == resourceId }?.text
                    ?: v.translations.firstOrNull()?.text
                if (v.verseKey.isNotBlank() && !text.isNullOrBlank()) map[v.verseKey] = text
            }
            if (map.isEmpty()) return null
            writeCache(chapterId, resourceId, verses)
            memory[key] = map
            map
        } catch (_: Exception) {
            null
        }
    }

    /** True when a non-empty chapter file exists for ([chapterId], [resourceId]). */
    fun isCached(chapterId: Int, resourceId: Int): Boolean {
        if (chapterId !in 1..114) return false
        return try {
            fileFor(chapterId, resourceId).let { it.isFile && it.length() > 0 }
        } catch (_: Exception) {
            false
        }
    }

    /** True when any cached chapter file exists for [resourceId]. */
    fun hasAnyCache(resourceId: Int): Boolean {
        return try {
            dir().listFiles { f ->
                f.isFile && f.name.startsWith("${resourceId}_") && f.name.endsWith(".json")
            }?.any { it.length() > 0 } == true
        } catch (_: Exception) {
            false
        }
    }

    /** Deletes every cached chapter file for [resourceId]. True unless an error occurs. */
    fun deletePack(resourceId: Int): Boolean {
        return try {
            memory.keys.removeIf { it.startsWith("${resourceId}_") }
            dir().listFiles { f ->
                f.isFile && f.name.startsWith("${resourceId}_") && f.name.endsWith(".json")
            }?.forEach { it.delete() }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchAllVerses(chapterId: Int, resourceId: Int): List<VerseRowJson> {
        val out = ArrayList<VerseRowJson>()
        var page = 1
        var fetched = 0
        while (true) {
            val url = URL(
                "${BASE_URL}verses/by_chapter/$chapterId" +
                    "?translations=$resourceId&words=false&per_page=$PER_PAGE&page=$page"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
            }
            val body = try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    ?: error("HTTP $code")
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val parsed = gson.fromJson(body, VersesByChapterResponse::class.java)
            out += parsed.verses
            fetched++
            val next = parsed.pagination?.nextPage
            if (next == null || fetched >= MAX_PAGES) break
            page = next
        }
        return out
    }

    private fun readCache(chapterId: Int, resourceId: Int): Map<String, String>? {
        return try {
            val f = fileFor(chapterId, resourceId)
            if (!f.isFile || f.length() == 0L) return null
            val parsed = f.bufferedReader(Charsets.UTF_8).use {
                gson.fromJson(it, VersesByChapterResponse::class.java)
            } ?: return null
            parsed.verses.mapNotNull { v ->
                val text = v.translations.firstOrNull { it.resourceId == resourceId }?.text
                    ?: v.translations.firstOrNull()?.text
                if (v.verseKey.isBlank() || text.isNullOrBlank()) null else v.verseKey to text
            }.toMap().ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(chapterId: Int, resourceId: Int, verses: List<VerseRowJson>) {
        try {
            dir().mkdirs()
            // Raw merged verses envelope — same DTO shape the network parses,
            // so cache hits reuse [readCache] unchanged.
            fileFor(chapterId, resourceId).bufferedWriter(Charsets.UTF_8).use {
                gson.toJson(VersesByChapterResponse(verses, null), it)
            }
        } catch (_: Exception) {
            // Cache is best-effort; the in-memory + returned map still works.
        }
    }
}

// ── API DTOs (subset of Android `QuranApi` verses shape) ────────────────────

private data class VersesByChapterResponse(
    val verses: List<VerseRowJson> = emptyList(),
    val pagination: PaginationJson? = null
)

private data class VerseRowJson(
    @SerializedName("verse_key") val verseKey: String = "",
    val translations: List<TranslationRowJson> = emptyList()
)

private data class TranslationRowJson(
    @SerializedName("resource_id") val resourceId: Int = 0,
    val text: String = ""
)

private data class PaginationJson(
    @SerializedName("next_page") val nextPage: Int? = null
)
