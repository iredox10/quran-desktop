package com.nur.quran.desktop.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nur.quran.shared.Reciters
import javazoom.jl.player.Player
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.prefs.Preferences

/**
 * Desktop audio backend: per-ayah MP3 streaming with disk cache.
 *
 * Public API:
 * - data class Track(verseKey, chapterId, verseNumber)
 * - var queue: List<Track>, var index: Int, var playing: Boolean (Compose-observable)
 * - var reciterId: Int (observable, persisted in Preferences node "audio_prefs", key "reciter_id")
 * - var repeatAyah: Int (1 = once, N = repeat the ayah N times, -1 = infinite; persisted)
 * - var repeatSelection: Int (1 = once, N = repeat the whole queue N times, -1 = infinite; persisted)
 * - var delaySeconds: Int (0..30 pause between ayahs; persisted)
 * - var sleepMinutes: Int (persisted key "sleep_minutes", default 0; >0 arms a stop deadline)
 * - val current: Track?
 * - playChapter(chapterId, startVerse = 1), playVerse(verseKey),
 *   togglePlayPause(), next(), prev(), stop()
 *
 * Stream URLs come from [Reciters.buildAudioUrl]. Playback runs on a single
 * daemon worker thread decoding with JLayer [Player] (blocking play()).
 * stop/next/prev/pause close the active player and bump a generation token so
 * the stale worker exits; the fresh state launches a new worker. All failures
 * stop quietly (message to stderr).
 */
object AudioEngine {

    data class Track(
        val verseKey: String,
        val chapterId: Int,
        val verseNumber: Int
    )

    private const val PREFS_NODE = "audio_prefs"
    private const val KEY_RECITER_ID = "reciter_id"
    private const val KEY_REPEAT_AYAH = "repeat_ayah"
    private const val KEY_REPEAT_SELECTION = "repeat_selection"
    private const val KEY_DELAY_SECONDS = "delay_seconds"
    private const val KEY_SLEEP_MINUTES = "sleep_minutes"

    /**
     * How many times each ayah plays before advancing: 1 = once, N = N times,
     * -1 = repeat forever. Persisted (web `audioSettings.repeatAya`).
     */
    var repeatAyah: Int
        get() = repeatAyahState.value
        set(value) {
            val normalized = normalizeRepeatCount(value)
            repeatAyahState.value = normalized
            persistPref(KEY_REPEAT_AYAH, normalized)
        }

    /**
     * How many times the whole queue plays: 1 = once, N = N times,
     * -1 = repeat forever. Persisted (web `audioSettings.repeatSelection`).
     */
    var repeatSelection: Int
        get() = repeatSelectionState.value
        set(value) {
            val normalized = normalizeRepeatCount(value)
            repeatSelectionState.value = normalized
            persistPref(KEY_REPEAT_SELECTION, normalized)
        }

    /** Pause between ayahs in seconds (0..30). Persisted. */
    var delaySeconds: Int
        get() = delaySecondsState.value
        set(value) {
            val normalized = value.coerceIn(0, 30)
            delaySecondsState.value = normalized
            persistPref(KEY_DELAY_SECONDS, normalized)
        }

    /**
     * Sleep-timer length in minutes; 0 disables. Setting > 0 arms the stop
     * deadline from now; 0 clears it. Persisted. Stops playback at the deadline.
     */
    var sleepMinutes: Int
        get() = sleepMinutesState.value
        set(value) {
            sleepMinutesState.value = value
            persistPref(KEY_SLEEP_MINUTES, value)
            sleepDeadlineMs = if (value > 0) System.currentTimeMillis() + value * 60_000L else 0L
        }

    @Volatile
    private var sleepDeadlineMs: Long = 0L

    var queue: List<Track> by mutableStateOf(emptyList())
        private set

    var index: Int by mutableStateOf(0)
        private set

    var playing: Boolean by mutableStateOf(false)
        private set

    private val reciterState = mutableStateOf(loadReciterId())
    private val repeatAyahState = mutableStateOf(normalizeRepeatCount(loadPref(KEY_REPEAT_AYAH, 1)))
    private val repeatSelectionState = mutableStateOf(normalizeRepeatCount(loadPref(KEY_REPEAT_SELECTION, 1)))
    private val delaySecondsState = mutableStateOf(loadPref(KEY_DELAY_SECONDS, 0).coerceIn(0, 30))
    private val sleepMinutesState = mutableStateOf(loadPref(KEY_SLEEP_MINUTES, 0))

    /** Currently selected reciter; setting persists to Preferences. */
    var reciterId: Int
        get() = reciterState.value
        set(value) {
            reciterState.value = value
            persistReciterId(value)
        }

    val current: Track?
        get() = queue.getOrNull(index)

    val sleepRemainingMs: Long
        get() = if (sleepDeadlineMs <= 0L) 0L
        else (sleepDeadlineMs - System.currentTimeMillis()).coerceAtLeast(0L)

    private val lock = Any()

    @Volatile
    private var generation = 0

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var activePlayer: Player? = null

    /**
     * Queue the chapter from [startVerse] through [endVerse] (inclusive, null =
     * end of chapter) and play from the start of that range.
     */
    fun playChapter(chapterId: Int, startVerse: Int = 1, endVerse: Int? = null) {
        val verses = chapterTracks(chapterId, startVerse, endVerse)
        if (verses.isEmpty()) return
        synchronized(lock) {
            queue = verses
            launchLocked(0)
        }
    }

    /** The verse range a chapter play would queue — also used by tests. */
    fun chapterTracks(chapterId: Int, startVerse: Int = 1, endVerse: Int? = null): List<Track> =
        QuranStore.versesOfChapter(chapterId)
            .filter { it.verseNumber >= startVerse && (endVerse == null || it.verseNumber <= endVerse) }
            .map { Track(it.verseKey, it.chapterId, it.verseNumber) }

    /** Queue the verse's chapter and start playback at [verseKey]. */
    fun playVerse(verseKey: String) {
        val chapterId = verseKey.substringBefore(":").toIntOrNull() ?: return
        val verses = QuranStore.versesOfChapter(chapterId)
        if (verses.isEmpty()) return
        val pos = verses.indexOfFirst { it.verseKey == verseKey }.takeIf { it >= 0 } ?: 0
        synchronized(lock) {
            queue = verses.map { Track(it.verseKey, it.chapterId, it.verseNumber) }
            launchLocked(pos)
        }
    }

    /** Replace the queue with [tracks] and start playback from the first one. */
    fun playTracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        synchronized(lock) {
            queue = tracks.toList()
            launchLocked(0)
        }
    }

    /** Pause if playing, resume the current track if paused. No-op when empty. */
    fun togglePlayPause() {
        synchronized(lock) {
            if (queue.isEmpty()) return
            if (playing) {
                generation++
                playing = false
                closePlayerLocked()
                worker?.interrupt()
                worker = null
            } else {
                launchLocked(index.coerceIn(0, queue.size - 1))
            }
        }
    }

    /** Advance one verse; stops at the end of the queue. */
    fun next() {
        synchronized(lock) {
            if (queue.isEmpty()) return
            if (index + 1 >= queue.size) {
                stopLocked()
            } else {
                launchLocked(index + 1)
            }
        }
    }

    /** Replay the previous verse (or restart the first one). */
    fun prev() {
        synchronized(lock) {
            if (queue.isEmpty()) return
            launchLocked(if (index > 0) index - 1 else 0)
        }
    }

    /** Stop playback and clear the queue. */
    fun stop() {
        synchronized(lock) { stopLocked() }
    }

    private fun sleepExpired(): Boolean =
        sleepDeadlineMs in 1..System.currentTimeMillis()

    // ── Internals (lock must be held for *Locked fns) ────────────────────────

    private fun stopLocked() {
        generation++
        playing = false
        queue = emptyList()
        index = 0
        sleepDeadlineMs = 0L
        closePlayerLocked()
        worker?.interrupt()
        worker = null
    }

    private fun closePlayerLocked() {
        try {
            activePlayer?.close()
        } catch (_: Exception) {
        }
        activePlayer = null
    }

    private fun launchLocked(from: Int) {
        generation++
        closePlayerLocked()
        worker?.interrupt()
        val gen = generation
        val snapshot = queue.toList()
        if (snapshot.isEmpty()) {
            playing = false
            return
        }
        val reciter = reciterState.value
        val start = from.coerceIn(0, snapshot.size - 1)
        index = start
        playing = true
        // Restored sleep preference becomes active once playback starts.
        if (sleepMinutes > 0 && sleepDeadlineMs == 0L) {
            sleepDeadlineMs = System.currentTimeMillis() + sleepMinutes * 60_000L
        }
        val t = Thread({ runQueue(snapshot, reciter, start, gen) }, "quran-audio-playback")
        t.isDaemon = true
        worker = t
        t.start()
    }

    /** Worker body: play snapshot[start..] sequentially. Never holds [lock]. */
    private fun runQueue(snapshot: List<Track>, reciter: Int, start: Int, gen: Int) {
        try {
            var i = start
            var ayahPlayed = 0
            var selectionPass = 0
            while (true) {
                if (gen != generation) return
                index = i
                val track = snapshot[i]
                val bytes: ByteArray = try {
                    resolveBytes(track, reciter)
                } catch (e: Exception) {
                    System.err.println("AudioEngine: load failed ${track.verseKey}: ${e.message}")
                    requestStop(gen)
                    return
                }
                if (gen != generation) return
                val player: Player = try {
                    Player(ByteArrayInputStream(bytes))
                } catch (e: Exception) {
                    System.err.println("AudioEngine: decode failed ${track.verseKey}: ${e.message}")
                    requestStop(gen)
                    return
                }
                activePlayer = player
                try {
                    player.play()
                } catch (e: Exception) {
                    // User navigation (close) also surfaces here; generation tells them apart.
                    if (gen != generation) return
                    System.err.println("AudioEngine: playback failed ${track.verseKey}: ${e.message}")
                    requestStop(gen)
                    return
                } finally {
                    try {
                        player.close()
                    } catch (_: Exception) {
                    }
                    if (activePlayer === player) activePlayer = null
                }
                if (gen != generation) return
                // Track finished: sleep timer > ayah repeat > advance / queue repeat.
                if (sleepExpired()) {
                    System.err.println("AudioEngine: sleep timer elapsed")
                    requestStop(gen)
                    return
                }
                ayahPlayed++
                if (repeatAyah == -1 || ayahPlayed < repeatAyah) {
                    if (!waitDelaySeconds(gen)) return
                    continue
                }
                ayahPlayed = 0
                if (i + 1 >= snapshot.size) {
                    selectionPass++
                    if (repeatSelection == -1 || selectionPass < repeatSelection) {
                        i = 0
                    } else {
                        break
                    }
                } else {
                    i++
                }
                if (!waitDelaySeconds(gen)) return
            }
            synchronized(lock) {
                if (gen == generation) playing = false
            }
        } catch (t: Throwable) {
            System.err.println("AudioEngine: ${t.message}")
            requestStop(gen)
        }
    }

    /** Sleeps [delaySeconds] between tracks; false when playback was cancelled. */
    private fun waitDelaySeconds(gen: Int): Boolean {
        val seconds = delaySeconds
        if (seconds <= 0) return true
        if (gen != generation) return false
        try {
            Thread.sleep(seconds * 1000L)
        } catch (_: InterruptedException) {
            return false
        }
        return gen == generation
    }

    private fun requestStop(gen: Int) {
        synchronized(lock) {
            if (gen == generation) stopLocked()
        }
    }

    /** Cached MP3 bytes, downloading via [Reciters.buildAudioUrl] on miss. */
    private fun resolveBytes(track: Track, reciter: Int): ByteArray {
        val dir = File(System.getProperty("user.home"), ".quran-nur/audio/$reciter")
        val file = File(dir, "${track.chapterId}_${track.verseNumber}.mp3")
        if (file.isFile && file.length() > 0) return file.readBytes()
        val url = Reciters.buildAudioUrl(reciter, track.verseKey)
            ?: throw IllegalStateException("No audio URL for reciter $reciter")
        val bytes = URL(url).openStream().use { it.readBytes() }
        try {
            dir.mkdirs()
            file.writeBytes(bytes)
        } catch (_: Exception) {
            // Cache is best-effort; playback uses the in-memory bytes.
        }
        return bytes
    }

    /** 1..99 plays, or -1 for infinite. Anything else collapses to "once". */
    private fun normalizeRepeatCount(value: Int): Int = when {
        value == -1 -> -1
        value < 1 -> 1
        value > 99 -> 99
        else -> value
    }

    private fun loadReciterId(): Int {
        return try {
            Preferences.userRoot().node(PREFS_NODE).getInt(KEY_RECITER_ID, Reciters.DEFAULT_ID)
        } catch (_: Exception) {
            Reciters.DEFAULT_ID
        }
    }

    private fun persistReciterId(id: Int) {
        try {
            Preferences.userRoot().node(PREFS_NODE).putInt(KEY_RECITER_ID, id)
        } catch (e: Exception) {
            System.err.println("AudioEngine: persist reciter failed: ${e.message}")
        }
    }

    private fun loadPref(key: String, def: String): String = try {
        Preferences.userRoot().node(PREFS_NODE).get(key, def) ?: def
    } catch (_: Exception) {
        def
    }

    private fun loadPref(key: String, def: Int): Int = try {
        Preferences.userRoot().node(PREFS_NODE).getInt(key, def)
    } catch (_: Exception) {
        def
    }

    private fun persistPref(key: String, value: String) {
        try {
            Preferences.userRoot().node(PREFS_NODE).put(key, value)
        } catch (_: Exception) {
        }
    }

    private fun persistPref(key: String, value: Int) {
        try {
            Preferences.userRoot().node(PREFS_NODE).putInt(key, value)
        } catch (_: Exception) {
        }
    }
}
