package com.nur.quran.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.components.VerseRow
import com.nur.quran.desktop.ui.screens.AnalyticsScreenDesktop
import com.nur.quran.desktop.ui.screens.App
import com.nur.quran.desktop.ui.screens.DownloadsScreenDesktop
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
import com.nur.quran.shared.PlannerEngine
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
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

    private fun save(image: ImageBitmap, name: String) {
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)
        val dir = File("build/screenshots").apply { mkdirs() }
        ImageIO.write(out, "png", File(dir, name))
    }
}
