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
    const val AMIRI_QURAN = "Amiri Quran"
    const val NOTO_NASKH_ARABIC = "Noto Naskh Arabic"
    const val SCHEHERAZADE_NEW = "Scheherazade New"
    const val SYSTEM_DEFAULT = "System Default"

    /** All selectable font display names, in settings order. */
    val names: List<String> = listOf(
        KFGQPC_HAFS,
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
     *
     * NOTE: "Uthman Taha Naskh" was dropped — the only TTF sources at hand
     * were corrupt 404 pages (same as the Android repo's copies) or an old
     * Ver10 without Uthmani mark coverage. Saved "uthman…" prefs resolve to
     * Scheherazade below.
     */
    fun fileFor(name: String): String? {
        return when (name.trim().lowercase()) {
            "kfgqpc-hafs", "kfgqpc hafs" -> "fonts/kfgqpc_hafs.ttf"
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
     * classpath bytes (desktop has no R.font ids), followed by Amiri and
     * Scheherazade fallbacks so a glyph missing from the primary font (or a
     * corrupt/placeholder file, which this repo has shipped before) renders
     * from a font that has it instead of showing tofu. Falls back to
     * [FontFamily.Default] when nothing loads.
     */
    @Composable
    fun rememberFontFamily(name: String): FontFamily {
        val path = remember(name) { fileFor(name) }
        return remember(path) {
            val fonts = mutableListOf<androidx.compose.ui.text.font.Font>()
            if (path != null) {
                loadValidatedFont(path)?.let { fonts += it }
            }
            if (path != "fonts/amiri_regular.ttf") {
                loadValidatedFont("fonts/amiri_regular.ttf")?.let { fonts += it }
            }
            if (path != "fonts/scheherazade_regular.ttf") {
                loadValidatedFont("fonts/scheherazade_regular.ttf")?.let { fonts += it }
            }
            if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
        }
    }

    /**
     * Reads a bundled font and rejects non-font bytes (the repo previously
     * shipped HTML 404 pages with .ttf names — magic bytes catch those).
     */
    fun loadValidatedFont(path: String): androidx.compose.ui.text.font.Font? {
        return try {
            val bytes = DesktopFonts::class.java.getResourceAsStream("/$path")?.readBytes()
                ?: return null
            if (!isSfnt(bytes)) return null
            Font(identity = path, data = bytes, weight = FontWeight.Normal)
        } catch (_: Exception) {
            null
        }
    }

    private fun isSfnt(b: ByteArray): Boolean {
        if (b.size < 256) return false
        val magic = ((b[0].toInt() and 0xFF) shl 24) or
            ((b[1].toInt() and 0xFF) shl 16) or
            ((b[2].toInt() and 0xFF) shl 8) or
            (b[3].toInt() and 0xFF)
        // 00010000 (TrueType), OTTO (CFF-OpenType), true/typ1 (legacy Mac).
        return magic == 0x00010000 || magic == 0x4F54544F ||
            magic == 0x74727565 || magic == 0x74797031
    }
}
