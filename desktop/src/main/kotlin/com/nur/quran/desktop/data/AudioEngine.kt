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
    private const val KEY_REPEAT_MODE = "repeat_mode"
    private const val KEY_SLEEP_MINUTES = "sleep_minutes"

    /** Repeat behavior at track/queue end: "off" / "ayah" / "chapter". */
    val repeatMode: String get() = repeatModeState.value

    /** Sleep-timer length in minutes; 0 disables. Stops playback at the deadline. */
    val sleepMinutes: Int get() = sleepMinutesState.value

    @Volatile
    private var sleepDeadlineMs: Long = 0L

    var queue: List<Track> by mutableStateOf(emptyList())
        private set

    var index: Int by mutableStateOf(0)
        private set

    var playing: Boolean by mutableStateOf(false)
        private set

    private val reciterState = mutableStateOf(loadReciterId())
    private val repeatModeState = mutableStateOf(loadPref(KEY_REPEAT_MODE, "off"))
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

    /** Repeat mode: "off" | "ayah" | "chapter"; setting persists. */
    fun setRepeatMode(mode: String) {
        repeatModeState.value = mode
        persistPref(KEY_REPEAT_MODE, mode)
    }

    /** Sleep timer minutes (0 clears); restarts the deadline from now. */
    fun setSleepMinutes(minutes: Int) {
        sleepMinutesState.value = minutes
        persistPref(KEY_SLEEP_MINUTES, minutes)
        sleepDeadlineMs = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0L
    }

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

    /** Queue the whole chapter from [startVerse] and play from there. */
    fun playChapter(chapterId: Int, startVerse: Int = 1) {
        val verses = QuranStore.versesOfChapter(chapterId)
            .filter { it.verseNumber >= startVerse }
        if (verses.isEmpty()) return
        synchronized(lock) {
            queue = verses.map { Track(it.verseKey, it.chapterId, it.verseNumber) }
            launchLocked(0)
        }
    }

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
            while (i < snapshot.size) {
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
                // Track finished: sleep timer > ayah repeat > advance / chapter repeat.
                if (sleepExpired()) {
                    System.err.println("AudioEngine: sleep timer elapsed")
                    requestStop(gen)
                    return
                }
                when {
                    repeatMode == "ayah" -> i-- // replay same index
                    i + 1 >= snapshot.size && repeatMode == "chapter" -> i = 0
                    else -> i++
                }
            }
            synchronized(lock) {
                if (gen == generation && repeatMode != "chapter") playing = false
            }
        } catch (t: Throwable) {
            System.err.println("AudioEngine: ${t.message}")
            requestStop(gen)
        }
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
