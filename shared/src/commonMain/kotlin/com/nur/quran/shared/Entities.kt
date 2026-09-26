package com.nur.quran.shared

/**
 * Plain multiplatform copies of the Room entities from
 * `com.nur.quran.data.db.entities` (Android app).
 *
 * No Room annotations, no Android imports — pure Kotlin data classes so both
 * desktop targets and shared pure helpers (VerseText) can use them.
 */
data class Verse(
    val id: Int,
    val verseKey: String,
    val chapterId: Int,
    val verseNumber: Int,
    val textUthmani: String?,
    val textIndopak: String?,
    val textQpcHafs: String?,
    val pageNumber: Int,
    val juzNumber: Int
)

data class Word(
    val verseId: Int,
    val position: Int,
    val textUthmani: String?,
    val textIndopak: String?,
    val textQpcHafs: String?,
    val textUthmaniTajweed: String?,
    val charTypeName: String,
    /** Printed-mushaf line number for this word (0 = unknown/offline fallback). */
    val lineNumber: Int = 0
)
