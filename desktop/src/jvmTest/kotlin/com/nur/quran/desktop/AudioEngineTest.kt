package com.nur.quran.desktop

import com.nur.quran.desktop.data.AudioEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/** Audio parity: verse-range queues and repeat/delay setting normalization. */
class AudioEngineTest {

    @Test
    fun chapterTracksHonoursVerseRange() {
        val all = AudioEngine.chapterTracks(18)
        assertEquals(110, all.size)

        val range = AudioEngine.chapterTracks(18, startVerse = 10, endVerse = 12)
        assertEquals(listOf("18:10", "18:11", "18:12"), range.map { it.verseKey })

        val openEnd = AudioEngine.chapterTracks(18, startVerse = 108)
        assertEquals(listOf("18:108", "18:109", "18:110"), openEnd.map { it.verseKey })
    }

    @Test
    fun repeatCountsNormalizeToOnceOrInfinity() {
        AudioEngine.repeatAyah = 4
        assertEquals(4, AudioEngine.repeatAyah)
        AudioEngine.repeatAyah = -1
        assertEquals(-1, AudioEngine.repeatAyah)
        AudioEngine.repeatAyah = 0
        assertEquals(1, AudioEngine.repeatAyah)
        AudioEngine.repeatAyah = 500
        assertEquals(99, AudioEngine.repeatAyah)

        AudioEngine.repeatSelection = 3
        assertEquals(3, AudioEngine.repeatSelection)
        AudioEngine.repeatSelection = -5
        assertEquals(1, AudioEngine.repeatSelection)

        // Restore defaults so other tests see a neutral engine.
        AudioEngine.repeatAyah = 1
        AudioEngine.repeatSelection = 1
    }

    @Test
    fun delaySecondsClampsToThirty() {
        AudioEngine.delaySeconds = 3
        assertEquals(3, AudioEngine.delaySeconds)
        AudioEngine.delaySeconds = -2
        assertEquals(0, AudioEngine.delaySeconds)
        AudioEngine.delaySeconds = 90
        assertEquals(30, AudioEngine.delaySeconds)
        AudioEngine.delaySeconds = 0
    }
}
