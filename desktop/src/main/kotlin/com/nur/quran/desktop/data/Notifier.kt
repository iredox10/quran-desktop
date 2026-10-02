package com.nur.quran.desktop.data

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.prefs.Preferences

/**
 * Desktop system notifications via `java.awt.SystemTray`.
 *
 * No permission model on desktop JVM: fire-and-forget from the UI layer.
 */
object Notifier {
    private val prefs: Preferences = Preferences.userRoot().node("notify_prefs")
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Shows a system-tray notification. Returns false when the system tray
     * is unsupported or the display fails.
     */
    fun notify(title: String, message: String): Boolean {
        if (!SystemTray.isSupported()) return false
        try {
            val tray = SystemTray.getSystemTray()
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val icon = TrayIcon(image, title)
            icon.toolTip = title
            icon.isImageAutoSize = true
            tray.add(icon)
            try {
                icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
            } finally {
                tray.remove(icon)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Shows [notify] at most once per calendar day per [key]. Skips when the
     * last-shown date stamped for [key] equals today (`yyyy-MM-dd`),
     * otherwise shows and stamps today.
     */
    fun notifyOncePerDay(key: String, title: String, message: () -> String): Boolean {
        val today = LocalDate.now().format(dateFmt)
        if (prefs.get(key, null) == today) return false
        val shown = notify(title, message())
        prefs.put(key, today)
        runCatching { prefs.flush() }
        return shown
    }
}
