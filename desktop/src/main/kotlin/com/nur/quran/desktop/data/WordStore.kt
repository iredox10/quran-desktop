package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** One word of a mushaf page: display text + Madani line number. */
data class PageWord(
    val verseKey: String,
    val text: String,
    val lineNumber: Int
)

/** Private JSON models for `GET /verses/by_page/{page}` (only fields used). */
private data class PageWordsResponse(
    val verses: List<PageVerseJson>? = null
)

private data class PageVerseJson(
    @SerializedName("verse_key") val verseKey: String = "",
    val words: List<PageWordJson>? = null
)

private data class PageWordJson(
    @SerializedName("text_uthmani") val textUthmani: String? = null,
    @SerializedName("text_qpc_hafs") val textQpcHafs: String? = null,
    @SerializedName("text_indopak") val textIndopak: String? = null,
    @SerializedName("line_number") val lineNumber: Int? = null
)

/**
 * Page-accurate word layout source for the desktop Mushaf view.
 *
 * Mirrors the Android `QuranRepository.getVersesByPage` request (Madani
 * Standard defaults): `words=true` + `word_fields` incl. `text_uthmani` and
 * `line_number`, `per_page=50`, `mushaf=5`.
 *
 * Cache-first: raw JSON is cached at `~/.quran-nur/words/page_{n}.json`;
 * network is only hit on cache miss. Returns [emptyList] on any failure.
 */
object WordStore {
    private const val BASE_URL = "https://api.quran.com/api/v4"
    private const val WORD_FIELDS =
        "text_qpc_hafs,text_uthmani,page_number,line_number,translation,text_uthmani_tajweed"
    private const val MUSHAF_ID = 5 // Madani Standard (Android default)
    private const val PER_PAGE = 50

    private val gson = Gson()

    private fun cacheFile(page: Int): File {
        val dir = File(System.getProperty("user.home"), ".quran-nur/words")
        return File(dir, "page_$page.json")
    }

    fun getPageWords(page: Int): List<PageWord> {
        if (page !in 1..604) return emptyList()
        val file = cacheFile(page)
        // Cache first.
        try {
            if (file.exists()) {
                val raw = file.readText(Charsets.UTF_8)
                if (raw.isNotBlank()) return parse(raw)
            }
        } catch (_: Exception) {
            // Fall through to network.
        }
        // Network on miss.
        val raw = try {
            fetch(page)
        } catch (_: Exception) {
            return emptyList()
        }
        try {
            file.parentFile?.mkdirs()
            file.writeText(raw, Charsets.UTF_8)
        } catch (_: Exception) {
            // Cache write is best-effort.
        }
        return try {
            parse(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetch(page: Int): String {
        val url = URL(
            "$BASE_URL/verses/by_page/$page" +
                "?words=true&word_fields=$WORD_FIELDS&mushaf=$MUSHAF_ID&per_page=$PER_PAGE"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "quran-desktop-kotlin/1.0 (desktop)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(raw: String): List<PageWord> {
        if (raw.isBlank()) return emptyList()
        val res = gson.fromJson(raw, PageWordsResponse::class.java) ?: return emptyList()
        return res.verses.orEmpty().flatMap { verse ->
            verse.words.orEmpty().mapNotNull { word ->
                val text = word.textUthmani?.takeIf { it.isNotBlank() }
                    ?: word.textQpcHafs?.takeIf { it.isNotBlank() }
                    ?: word.textIndopak?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                PageWord(
                    verseKey = verse.verseKey,
                    text = text,
                    lineNumber = word.lineNumber ?: 0
                )
            }
        }
    }
}
