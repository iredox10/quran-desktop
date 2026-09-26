package com.nur.quran.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.screens.App
import com.nur.quran.desktop.ui.screens.SurahScreenDesktop
import com.nur.quran.desktop.ui.theme.NurTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Headless render tests: compose the real screens offscreen, save PNGs under
 * build/screenshots, and assert the bundled data loads. Runs without a
 * display (Skia software rendering).
 */
class ScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `data loads 114 chapters and 6236 verses`() {        assertEquals(114, QuranStore.chapters.size)
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
                SurahScreenDesktop(
                    chapterId = 1,
                    pal = com.nur.quran.desktop.ui.theme.NurPalette(false)
                )
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
                SurahScreenDesktop(
                    chapterId = 2,
                    pal = com.nur.quran.desktop.ui.theme.NurPalette(false)
                )
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
                SurahScreenDesktop(
                    chapterId = 2,
                    targetVerseKey = "2:255",
                    pal = com.nur.quran.desktop.ui.theme.NurPalette(false)
                )
            }
        }
        rule.waitForIdle()
        save(rule.onRoot().captureToImage(), "surah2_255.png")
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

    private fun save(image: ImageBitmap, name: String) {
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)
        val dir = File("build/screenshots").apply { mkdirs() }
        ImageIO.write(out, "png", File(dir, name))
    }
}
