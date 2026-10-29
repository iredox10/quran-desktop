package com.nur.quran.desktop.data

import java.util.prefs.Preferences

/**
 * Persisted onboarding tours and dismissal state, backed by [Preferences] node `"onboarding_prefs"`.
 * Mirrors web `useAppStore` tours:
 * - "home-tour" (Home Page 🏠)
 * - "surah-tour" (Reading & Audio 📖)
 * - "memorization-tour" (Hifdh Mode 🧠)
 * - "planner-tour" (Study Planner 📅)
 * - "library-tour" (Library 📚)
 */
object OnboardingStore {
    private const val PREFS_NODE = "onboarding_prefs"
    private const val KEY_DISMISSED = "dismissed"
    private const val KEY_COMPLETED = "completed_tours"

    private val prefs: Preferences = Preferences.userRoot().node(PREFS_NODE)

    val ALL_TOURS = listOf(
        TourItem("home-tour", "Home Page", "🏠"),
        TourItem("surah-tour", "Reading & Audio", "📖"),
        TourItem("memorization-tour", "Hifdh Mode", "🧠"),
        TourItem("planner-tour", "Study Planner", "📅"),
        TourItem("library-tour", "Library", "📚")
    )

    data class TourItem(
        val id: String,
        val label: String,
        val icon: String
    )

    @Synchronized
    fun isDismissed(): Boolean = prefs.getBoolean(KEY_DISMISSED, false)

    @Synchronized
    fun setDismissed(dismissed: Boolean) {
        prefs.putBoolean(KEY_DISMISSED, dismissed)
    }

    @Synchronized
    fun completedTours(): Set<String> {
        val raw = prefs.get(KEY_COMPLETED, "")
        if (raw.isBlank()) return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    @Synchronized
    fun completeTour(tourId: String) {
        val set = completedTours().toMutableSet()
        if (set.add(tourId)) {
            prefs.put(KEY_COMPLETED, set.joinToString(","))
        }
    }

    @Synchronized
    fun isCompleted(tourId: String): Boolean = completedTours().contains(tourId)

    @Synchronized
    fun reset() {
        prefs.remove(KEY_DISMISSED)
        prefs.remove(KEY_COMPLETED)
    }
}
