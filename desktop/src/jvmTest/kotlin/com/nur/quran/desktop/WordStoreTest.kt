package com.nur.quran.desktop

import com.nur.quran.desktop.data.WordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordStoreTest {

    @Test
    fun chapterOneLoadsAllVersesAndWords() {
        val words = WordStore.wordsForChapter(1)
        assertEquals(7, words.size)

        val fatihah = words["1:1"].orEmpty()
        assertEquals(5, fatihah.size)
        assertEquals("بِسْمِ", fatihah.first().arabic)
        assertEquals("In (the) name", fatihah.first().translation)
        assertEquals("bis'mi", fatihah.first().transliteration)
        assertTrue(fatihah.last().isEndMarker)
    }

    @Test
    fun wordOrderAndTajweedMarkupArePreserved() {
        val words = WordStore.wordsForVerse("1:2").orEmpty()
        assertEquals(listOf(1, 2, 3, 4, 5), words.map { it.position })
        assertEquals("ٱلْحَمْدُ", words.first().arabic)
        assertTrue(words.any { it.tajweedHtml.contains("<rule class=") || it.tajweedHtml.isNotEmpty() })
    }

    @Test
    fun longVersesAndBoundaryChaptersResolve() {
        assertTrue(WordStore.wordsForVerse("2:255").size > 20)
        assertTrue(WordStore.wordsForVerse("114:1").isNotEmpty())
        assertFalse(WordStore.wordsForVerse("114:7").isNotEmpty())
        assertTrue(WordStore.wordsForChapter(0).isEmpty())
        assertTrue(WordStore.wordsForChapter(115).isEmpty())
    }

    @Test
    fun everyAyahEndsWithAnEndMarkerWord() {
        val chapter36 = WordStore.wordsForChapter(36)
        assertEquals(83, chapter36.size)
        chapter36.forEach { (key, words) ->
            assertTrue("$key should end with a marker", words.last().isEndMarker)
        }
    }
}
