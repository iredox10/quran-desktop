package com.nur.quran.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.nur.quran.desktop.ui.screens.HomeScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.BeforeClass
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun markWelcomed() {
            java.util.prefs.Preferences.userRoot().node("welcome_prefs").putBoolean("seen", true)
        }
    }

    private val pal = NurPalette(false)

    @Test
    fun testHomeScreenParity() = runDesktopComposeUiTest(width = 1280, height = 2400) {
        setContent {
            NurTheme {
                HomeScreenDesktop(pal = pal)
            }
        }

        waitForIdle()

        // 1. Assert Greeting ("Assalamu Alaikum")
        onNodeWithText("Assalamu Alaikum", substring = true).assertExists()

        // 2. Assert Weekly Heatmap ("This Week")
        onNodeWithText("This Week", substring = true).assertExists()

        // 3. Assert "Browse the Quran" header
        onNodeWithText("Browse the Quran", substring = true).assertExists()

        // 4. Assert Browse mode pills ("Surah", "Page", "Juz", "Hizb")
        onNodeWithText("Surah").assertExists()
        onNodeWithText("Page").assertExists()
        onNodeWithText("Juz").assertExists()
        onNodeWithText("Hizb").assertExists()

        // 5. Assert Search box
        onAllNodesWithText("Search", substring = true).onFirst().assertExists()

        // 6. Assert Chapter cards in the grid
        onAllNodesWithText("Al-Fatihah", substring = true).onFirst().assertExists()
        onAllNodesWithText("Al-Baqarah", substring = true).onFirst().assertExists()

        // 7. Capture and save high-resolution screenshot
        val image: ImageBitmap = onRoot().captureToImage()
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)

        val targetFiles = listOf(
            File("desktop/build/screenshots/home_parity.png"),
            File("build/screenshots/home_parity.png")
        )
        for (targetFile in targetFiles) {
            targetFile.parentFile?.mkdirs()
            ImageIO.write(out, "png", targetFile)
        }
    }
}
