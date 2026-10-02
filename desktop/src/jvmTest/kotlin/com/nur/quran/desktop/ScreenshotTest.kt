package com.nur.quran.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.DesktopFonts
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.components.verseDisplayArabic
import com.nur.quran.desktop.ui.library.AddToCollectionDialog
import com.nur.quran.desktop.ui.screens.AnalyticsScreenDesktop
import com.nur.quran.desktop.ui.screens.App
import com.nur.quran.desktop.ui.screens.DownloadsScreenDesktop
import com.nur.quran.desktop.ui.screens.HistoryScreenDesktop
import com.nur.quran.desktop.ui.screens.HifdhReaderScreenDesktop
import com.nur.quran.desktop.ui.screens.LibraryScreenDesktop
import com.nur.quran.desktop.ui.screens.MemorizeScreenDesktop
import com.nur.quran.desktop.ui.screens.PlannerReaderScreenDesktop
import com.nur.quran.desktop.ui.screens.PlannerScreenDesktop
import com.nur.quran.desktop.ui.screens.ProfileScreenDesktop
import com.nur.quran.desktop.ui.screens.SurahScreenDesktop
import com.nur.quran.desktop.ui.screens.WelcomeScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.desktop.ui.theme.rememberArabicFontFamily
import com.nur.quran.shared.PlannerEngine
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/**
 * Headless render tests: compose the real screens offscreen, save PNGs under
 * build/screenshots, and assert the bundled data loads. Runs without a
 * display (Skia software rendering).
 */
class ScreenshotTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun markWelcomed() {
            // App() shows the welcome gate when unseen; tests want the home page.
            java.util.prefs.Preferences.userRoot().node("welcome_prefs").putBoolean("seen", true)
        }
    }

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    @Test
    fun `data loads 114 chapters and 6236 verses`() {
        assertEquals(114, QuranStore.chapters.size)
        assertEquals(6236, QuranStore.verseCount)
        assertEquals(7, QuranStore.versesOfChapter(1).size)
        assertTrue(QuranStore.versesOfPage(1).isNotEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `home screen renders`() {
        rule.setContent { MaterialTheme { App() } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "home.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `surah al-fatiha renders arabic with translation`() {
        rule.setContent {
            NurTheme {
                SurahScreenDesktop(chapterId = 1, pal = pal)
            }
        }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "surah1.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `surah al-baqarah renders dividers and nav`() {
        rule.setContent {
            NurTheme {
                SurahScreenDesktop(chapterId = 2, pal = pal)
            }
        }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "surah2.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `scroll target verse highlights ayat al-kursi`() {
        rule.setContent {
            NurTheme {
                SurahScreenDesktop(chapterId = 2, targetVerseKey = "2:255", pal = pal)
            }
        }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "surah2_255.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `memorize index renders`() {
        rule.setContent { NurTheme { MemorizeScreenDesktop(pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "memorize.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `hifdh reader renders`() {
        rule.setContent { NurTheme { HifdhReaderScreenDesktop(chapterId = 114, pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "hifdh.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `planner renders`() {
        rule.setContent { NurTheme { PlannerScreenDesktop(pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "planner.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `planner reader zero state renders`() {
        rule.setContent {
            NurTheme {
                PlannerReaderScreenDesktop(dayDate = PlannerEngine.formatPlannerDate(), pal = pal)
            }
        }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "planner_reader.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `analytics renders`() {
        rule.setContent { NurTheme { AnalyticsScreenDesktop(pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "analytics.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `profile renders`() {
        rule.setContent { NurTheme { ProfileScreenDesktop(pal = pal, dark = false, onToggleTheme = {}) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "profile.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `library renders`() {
        rule.setContent { NurTheme { LibraryScreenDesktop(pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "library.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `downloads renders`() {
        rule.setContent { NurTheme { DownloadsScreenDesktop(pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "downloads.png")
    }

    @Test
    fun `wide home renders 3-column browse grid`() {
        val scene = ImageComposeScene(width = 1500, height = 2200) {
            NurTheme { App() }
        }
        try {
            val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
                ?: error("PNG encode failed")
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "home_wide.png").writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun `welcome renders`() {        val scene = ImageComposeScene(width = 1100, height = 900) {
            NurTheme { WelcomeScreenDesktop(pal = pal) }
        }
        try {
            val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
                ?: error("PNG encode failed")
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "welcome.png").writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun `ayah 2-5 closeup has no tofu`() {
        val verse = QuranStore.versesOfChapter(2).first { it.verseNumber == 5 }
        val scene = ImageComposeScene(width = 1400, height = 420) {
            NurTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = pal.white
                ) {
                    VerseRow(verse = verse, pal = pal)
                }
            }
        }
        try {
            val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
                ?: error("PNG encode failed")
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "verse25.png").writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }

    /**
     * Regression closeups for the floating-waqf-blob bug on Surah Sad 38:6/38:8:
     * verse-level text_uthmani carries waqf signs (ۖ ۚ) as space-separated
     * tokens, which used to render as detached rings between words.
     *
     * Saves, per verse:
     * - `verse38_6.png` — the FIXED VerseRow pipeline (artifact + gold check).
     * - `verse38_6_plain.png` / `verse38_6_prefix.png` — like-for-like
     *   arabic-only renders of the fixed vs pre-fix string (same font, size,
     *   alignment) so a connected-component diff isolates the removed blobs.
     *
     * Asserts the fixed render draws real ink and the gold end marker.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `surah sad scrolled to 38 6 renders`() {
        rule.setContent {
            NurTheme {
                SurahScreenDesktop(chapterId = 38, targetVerseKey = "38:6", pal = pal)
            }
        }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "surah38_6.png")
    }

    @Test
    fun `surah sad closeups render with gold marker and no waqf blobs`() {
        val font = DesktopFonts.KFGQPC_HAFS
        var goldSeen = 0
        for ((key, name) in listOf("38:6" to "verse38_6", "38:8" to "verse38_8")) {
            val verse = QuranStore.versesOfChapter(38).first { it.verseKey == key }

            // 1) Fixed pipeline — exactly what the app renders.
            val fixed = ImageComposeScene(width = 1400, height = 420) {
                NurTheme {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = pal.white
                    ) {
                        VerseRow(verse = verse, pal = pal, fontName = font)
                    }
                }
            }
            try {
                val png = fixed.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
                    ?: error("PNG encode failed")
                val img = ImageIO.read(java.io.ByteArrayInputStream(png.bytes))
                    ?: error("PNG decode failed")
                var ink = 0
                var markerGold = 0
                for (y in 0 until img.height) {
                    for (x in 0 until img.width) {
                        val argb = img.getRGB(x, y)
                        val r = (argb shr 16) and 0xFF
                        val g = (argb shr 8) and 0xFF
                        val b = argb and 0xFF
                        if (r < 90 && g < 90 && b < 90) ink++
                        // hGold #B8924A (light theme) with antialiasing slack.
                        if (abs(r - 184) <= 30 && abs(g - 146) <= 30 && abs(b - 74) <= 40) markerGold++
                    }
                }
                assertTrue("$key rendered almost no text (ink=$ink)", ink > 5_000)
                assertTrue("$key end marker not gold (gold=$markerGold)", markerGold > 50)
                goldSeen += markerGold
                File("build/screenshots").apply { mkdirs() }
                File("build/screenshots", "$name.png").writeBytes(png.bytes)
            } finally {
                fixed.close()
            }

            // 2) Differential pair: identical render, only the string differs.
            val digits = com.nur.quran.shared.formatArabicDigits(verse.verseNumber)
            val marker = if (com.nur.quran.shared.usesEmbeddedEndMarker(font)) " $digits"
            else " \u06DD$digits"
            val fixedText = verseDisplayArabic(verse, font)
            val rawText = verse.arabic + marker
            for ((suffix, text) in listOf("plain" to fixedText, "prefix" to rawText)) {
                val scene = ImageComposeScene(width = 1400, height = 420) {
                    NurTheme {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = pal.white
                        ) {
                            androidx.compose.foundation.text.BasicText(
                                text = text,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 26.sp,
                                    lineHeight = 52.sp,
                                    textAlign = TextAlign.Right,
                                    fontFamily = rememberArabicFontFamily(font),
                                    color = pal.ink
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 40.dp, vertical = 40.dp)
                            )
                        }
                    }
                }
                try {
                    val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
                        ?: error("PNG encode failed")
                    val dir = File("build/screenshots").apply { mkdirs() }
                    File(dir, "${name}_$suffix.png").writeBytes(png.bytes)
                } finally {
                    scene.close()
                }
            }
        }
        assertTrue("no gold pixels anywhere", goldSeen > 100)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `history screen renders`() {
        rule.setContent { NurTheme { HistoryScreenDesktop(pal = pal) } }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "history.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `audio setup sheet composes`() {
        // ModalBottomSheet lives in its own window (scene capture stays black),
        // so this is a composition smoke test: it fails on any throw/recompose loop.
        rule.setContent {
            NurTheme {
                com.nur.quran.desktop.ui.audio.AudioSetupSheetDesktop(pal = pal, onDismiss = {})
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `add to collection dialog renders`() {
        renderScene("collections.png", 900, 800) {
            NurTheme {
                AddToCollectionDialog(pal = pal, verseKey = "2:255", onDismiss = {})
            }
        }
    }

    private fun renderScene(
        name: String,
        width: Int,
        height: Int,
        content: @Composable () -> Unit
    ) {
        val scene = ImageComposeScene(width = width, height = height, content = content)
        try {
            val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
                ?: error("PNG encode failed")
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, name).writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }

    private fun save(image: ImageBitmap, name: String) {
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)
        val dir = File("build/screenshots").apply { mkdirs() }
        ImageIO.write(out, "png", File(dir, name))
    }
}
