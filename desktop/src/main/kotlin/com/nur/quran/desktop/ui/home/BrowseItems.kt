package com.nur.quran.desktop.ui.home

import com.nur.quran.desktop.data.ChapterJson
import com.nur.quran.shared.HIZB_STARTS
import com.nur.quran.shared.JUZ_STARTS
import com.nur.quran.shared.MUSHAF_PAGE_COUNT
import com.nur.quran.shared.getJuzByPage

/** Browse item model — mirrors Android `BrowseItem` (web `getBrowseItems`). */
data class BrowseItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val meta: String,
    val arabic: String?,
    val prefix: Int?, // null → hash icon (page mode has prefixes; kept for parity)
    val chapterId: Int? = null,
    val pageNumber: Int? = null
)

/** Mirrors Android `buildBrowseItems()`. */
fun buildBrowseItems(mode: String, chapters: List<ChapterJson>): List<BrowseItem> {
    val chapMap = chapters.associateBy { it.id }
    return when (mode) {
        "page" -> (1..MUSHAF_PAGE_COUNT).map { n ->
            val juz = getJuzByPage(n)
            BrowseItem(
                key = "page-$n",
                title = "Page $n",
                subtitle = "Juz ${juz.id} · Mushaf View",
                meta = "Page ${n.toString().padStart(3, '0')}",
                arabic = "صفحة $n",
                prefix = n,
                pageNumber = n
            )
        }
        "juz" -> JUZ_STARTS.map { juz ->
            val chapId = juz.verseKey.substringBefore(":").toIntOrNull() ?: 1
            val surahName = chapMap[chapId]?.nameSimple ?: "Surah $chapId"
            BrowseItem(
                key = "juz-${juz.id}",
                title = "Juz ${juz.id}",
                subtitle = "$surahName (${juz.verseKey})",
                meta = "Page ${juz.pageNumber}",
                arabic = "الجزء ${juz.id}",
                prefix = juz.id,
                pageNumber = juz.pageNumber,
                chapterId = chapId
            )
        }
        "hizb" -> HIZB_STARTS.map { hizb ->
            val chapId = hizb.verseKey.substringBefore(":").toIntOrNull() ?: 1
            val surahName = chapMap[chapId]?.nameSimple ?: "Surah $chapId"
            BrowseItem(
                key = "hizb-${hizb.id}",
                title = "Hizb ${hizb.id}",
                subtitle = "$surahName (${hizb.verseKey})",
                meta = "Page ${hizb.pageNumber}",
                arabic = "حزب ${hizb.id}",
                prefix = hizb.id,
                pageNumber = hizb.pageNumber,
                chapterId = chapId
            )
        }
        else -> chapters.map { chapter ->
            val place = if (chapter.revelationPlace.equals("makkah", ignoreCase = true)) "Meccan" else "Medinan"
            BrowseItem(
                key = "surah-${chapter.id}",
                title = chapter.nameSimple,
                subtitle = chapter.translatedNameText.ifEmpty { "Surah ${chapter.id}" },
                meta = "$place • ${chapter.versesCount} Ayahs",
                arabic = chapter.nameArabic,
                prefix = chapter.id,
                chapterId = chapter.id
            )
        }
    }
}

/** Mirrors Android `filterBrowseItems()` (title/subtitle/meta/arabic/number). */
fun filterBrowseItems(items: List<BrowseItem>, query: String): List<BrowseItem> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return items
    return items.filter { item ->
        listOfNotNull(
            item.title,
            item.subtitle,
            item.meta,
            item.arabic,
            item.prefix?.toString(),
            item.chapterId?.toString(),
            item.pageNumber?.toString()
        ).any { it.lowercase().contains(q) }
    }
}
