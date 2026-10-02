package com.nur.quran.desktop.data

import com.google.gson.Gson
import com.nur.quran.shared.PrayerTimings
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.prefs.Preferences

/**
 * Daily prayer times from Aladhan (timingsByCity, method=2), cached for the
 * day per city/country at ~/.quran-nur/prayer_timings.json. City/country are
 * persisted in Preferences node "prayer_prefs". Returns null on any failure —
 * callers must degrade gracefully (offline-first).
 */
object PrayerStore {
    private const val BASE_URL = "https://api.aladhan.com/v1/timingsByCity"
    private val PRAYER_KEYS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    private val prefs: Preferences = Preferences.userRoot().node("prayer_prefs")
    private val gson = Gson()

    // ── Location prefs ──
    fun getCity(): String = prefs.get("city", "Mecca") ?: "Mecca"
    fun putCity(value: String) {
        prefs.put("city", value)
    }

    fun getCountry(): String = prefs.get("country", "Saudi Arabia") ?: "Saudi Arabia"
    fun putCountry(value: String) {
        prefs.put("country", value)
    }

    /**
     * Today's timings, cache-first. Cache key includes date + city + country so
     * a location change refetches. Null when offline and nothing cached.
     */
    @Synchronized
    fun getToday(): PrayerTimings? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val city = getCity()
        val country = getCountry()
        val key = "$today|$city|$country"
        val cacheFile = File(System.getProperty("user.home"), ".quran-nur/prayer_timings.json")

        // Cache hit?
        readCache(cacheFile)?.let { cached ->
            if (cached.key == key) return cached.toTimings(today)
        }

        // Fetch fresh.
        val fetched = fetch(city, country) ?: return readCache(cacheFile)?.toTimings(today)
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(gson.toJson(CacheEntry(key = key, timings = fetched)))
        } catch (_: Exception) {
            // Cache write is best-effort.
        }
        return CacheEntry(key = key, timings = fetched).toTimings(today)
    }

    private fun fetch(city: String, country: String): Map<String, String>? {
        return try {
            val url = "$BASE_URL?city=${URLEncoder.encode(city, "UTF-8")}" +
                "&country=${URLEncoder.encode(country, "UTF-8")}&method=2"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "QuranNur-Desktop/1.0")
            val body = conn.inputStream.use { it.readBytes().decodeToString() }
            conn.disconnect()
            val parsed = gson.fromJson(body, AladhanResponse::class.java)
            val timings = parsed?.data?.timings ?: return null
            val cleaned = PRAYER_KEYS.mapNotNull { name ->
                timings[name]?.let { name to it.substringBefore(" ").trim() }
            }.toMap()
            if (cleaned.size == PRAYER_KEYS.size) cleaned else null
        } catch (_: Exception) {
            null
        }
    }

    private fun readCache(cacheFile: File): CacheEntry? {
        return try {
            if (!cacheFile.isFile) return null
            gson.fromJson(cacheFile.readText(), CacheEntry::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private class CacheEntry(
        val key: String = "",
        val timings: Map<String, String> = emptyMap()
    )

    private fun CacheEntry.toTimings(date: String): PrayerTimings? =
        if (timings.isEmpty()) null else PrayerTimings(date = date, timings = timings)

    // Aladhan response shape (only what we consume).
    private class AladhanResponse(val data: AladhanData?)
    private class AladhanData(val timings: Map<String, String>?)
}
