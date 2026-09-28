package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/** One row of `data/chapters.json` (only the fields the desktop UI needs). */
data class ChapterJson(
    val id: Int = 0,
    @SerializedName("name_simple") val nameSimple: String = "",
    @SerializedName("name_arabic") val nameArabic: String = "",
    @SerializedName("verses_count") val versesCount: Int = 0,
    val pages: List<Int> = emptyList(),
    @SerializedName("translated_name") val translatedName: TranslatedNameJson? = null,
    @SerializedName("revelation_place") val revelationPlace: String = ""
) {
    val translatedNameText: String get() = translatedName?.name.orEmpty()
}

data class TranslatedNameJson(val name: String = "")

/** One row of `data/quran_full.json`. */
data class VerseJson(
    val id: Int = 0,
    @SerializedName("verse_key") val verseKey: String = "",
    @SerializedName("text_uthmani") val textUthmani: String = "",
    @SerializedName("text_indopak") val textIndopak: String = "",
    @SerializedName("page_number") val pageNumber: Int = 0,
    @SerializedName("juz_number") val juzNumber: Int = 0,
    val translation: String = "",
    @SerializedName("text_uthmani_tajweed") val textUthmaniTajweed: String? = null
)

/** Verse ready for the desktop UI (chapter/verse numbers parsed from `verse_key`). */
data class DeskVerse(
    val verseKey: String,
    val chapterId: Int,
    val verseNumber: Int,
    val arabic: String,
    val textIndopak: String = "",
    val tajweedHtml: String? = null,
    val translation: String,
    val pageNumber: Int
)

/**
 * Loads the bundled `data/chapters.json` + `data/quran_full.json` from the
 * classpath (desktop/src/main/resources) once and serves filtered views.
 */
object QuranStore {
    private val gson = Gson()

    val chapters: List<ChapterJson> by lazy {
        read<List<ChapterJson>>("/data/chapters.json", object : TypeToken<List<ChapterJson>>() {})
            .sortedBy { it.id }
    }

    private val allVerses: List<DeskVerse> by lazy {
        read<List<VerseJson>>("/data/quran_full.json", object : TypeToken<List<VerseJson>>() {})
            .mapNotNull { v ->
                val parts = v.verseKey.split(":")
                if (parts.size != 2) return@mapNotNull null
                val chapterId = parts[0].toIntOrNull() ?: return@mapNotNull null
                val verseNumber = parts[1].toIntOrNull() ?: return@mapNotNull null
                DeskVerse(
                    verseKey = v.verseKey,
                    chapterId = chapterId,
                    verseNumber = verseNumber,
                    arabic = v.textUthmani,
                    textIndopak = v.textIndopak,
                    tajweedHtml = v.textUthmaniTajweed,
                    translation = v.translation,
                    pageNumber = v.pageNumber
                )
            }
    }

    private val byChapter: Map<Int, List<DeskVerse>> by lazy { allVerses.groupBy { it.chapterId } }
    private val byPage: Map<Int, List<DeskVerse>> by lazy { allVerses.groupBy { it.pageNumber } }

    fun chapter(id: Int): ChapterJson? = chapters.firstOrNull { it.id == id }

    fun versesOfChapter(chapterId: Int): List<DeskVerse> =
        byChapter[chapterId].orEmpty().sortedBy { it.verseNumber }

    fun versesOfPage(page: Int): List<DeskVerse> =
        byPage[page].orEmpty().sortedWith(compareBy({ it.chapterId }, { it.verseNumber }))

    val verseCount: Int by lazy { allVerses.size }

    /** Full-text search over Arabic + translation, newest-relevance: chapter order. */
    fun searchVerses(query: String, limit: Int = 50): List<DeskVerse> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val hits = allVerses.filter {
            it.arabic.contains(query.trim()) || it.translation.lowercase().contains(q)
        }
        return hits.take(limit)
    }

    /** Deterministic verse of the day (rotates with the day of year). */
    fun verseOfDay(): DeskVerse {
        val day = java.time.LocalDate.now().dayOfYear
        return allVerses[day % allVerses.size]
    }

    private fun <T> read(path: String, type: TypeToken<T>): T {
        val stream = QuranStore::class.java.getResourceAsStream(path)
            ?: error("Bundled resource missing: $path")
        return stream.bufferedReader(Charsets.UTF_8).use { gson.fromJson(it, type.type) }
    }
}
