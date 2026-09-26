package com.nur.quran.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Desktop font registry mirroring the Android font map in
 * `SurahScreen.kt` (getArabicFontFamily / usesEmbeddedEndMarker).
 *
 * Display names are the user-facing settings values; [fileFor] resolves
 * them to a resource path under `desktop/src/main/resources/`.
 *
 * Compose loading is intentionally left as a TODO — when wiring up, use:
 * ```
 * // TODO: Font("fonts/kfgqpc_hafs.ttf") etc. via Compose Font loading
 * ```
 * (Desktop Compose loads fonts from resource paths/files, not R.font ids.)
 */
object DesktopFonts {

    const val KFGQPC_HAFS = "KFGQPC Hafs"
    const val UTHMAN_TAHA_NASKH = "Uthman Taha Naskh"
    const val AMIRI_QURAN = "Amiri Quran"
    const val NOTO_NASKH_ARABIC = "Noto Naskh Arabic"
    const val SCHEHERAZADE_NEW = "Scheherazade New"
    const val SYSTEM_DEFAULT = "System Default"

    /** All selectable font display names, in settings order. */
    val names: List<String> = listOf(
        KFGQPC_HAFS,
        UTHMAN_TAHA_NASKH,
        AMIRI_QURAN,
        NOTO_NASKH_ARABIC,
        SCHEHERAZADE_NEW,
        SYSTEM_DEFAULT,
    )

    /**
     * Resolves a display [name] (case/space/dash-insensitive, same aliases as
     * Android's getArabicFontFamily) to its regular-weight resource path.
     * Returns null for [SYSTEM_DEFAULT] (no bundled file).
     * See `fonts/FONTS.md` for the bold-weight companions.
     */
    fun fileFor(name: String): String? {
        return when (name.trim().lowercase()) {
            "kfgqpc-hafs", "kfgqpc hafs" -> "fonts/kfgqpc_hafs.ttf"
            "uthman-taha-naskh", "uthman taha naskh" -> "fonts/uthman_taha_naskh.ttf"
            "amiri-quran", "amiri quran" -> "fonts/amiri_regular.ttf"
            "noto-naskh-arabic", "noto naskh arabic" -> "fonts/noto_regular.ttf"
            "scheherazade-new", "scheherazade new" -> "fonts/scheherazade_regular.ttf"
            "system default" -> null
            else -> "fonts/scheherazade_regular.ttf"
        }
    }

    /**
     * Mirrors Android's `usesEmbeddedEndMarker`: the KFGQPC Hafs font draws
     * the full end-of-ayah medallion from the digits alone (GSUB substitutes
     * each digit with the ornament composite), so callers must NOT prepend
     * the U+06DD mark for this font.
     */
    fun isEmbeddedEndMarker(fontName: String): Boolean {
        val name = fontName.trim().lowercase()
        return name == "kfgqpc-hafs" || name == "kfgqpc hafs"
    }

    /**
     * Loads the bundled TTF for [name] as a Compose [FontFamily] by reading the
     * classpath bytes (desktop has no R.font ids). Falls back to
     * [FontFamily.Default] for "System Default" or any load failure.
     */
    @Composable
    fun rememberFontFamily(name: String): FontFamily {
        val path = remember(name) { fileFor(name) }
        return remember(path) {
            if (path == null) {
                FontFamily.Default
            } else {
                try {
                    val bytes = DesktopFonts::class.java.getResourceAsStream("/$path")?.readBytes()
                    if (bytes == null) FontFamily.Default
                    else FontFamily(Font(identity = path, data = bytes, weight = FontWeight.Normal))
                } catch (_: Exception) {
                    FontFamily.Default
                }
            }
        }
    }
}
