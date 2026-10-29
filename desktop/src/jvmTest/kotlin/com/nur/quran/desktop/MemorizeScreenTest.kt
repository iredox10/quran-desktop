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
import com.nur.quran.desktop.ui.screens.MemorizeScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MemorizeScreenTest {

    private val pal = NurPalette(false)

    @Test
    fun testMemorizeScreenParity() = runDesktopComposeUiTest(width = 1280, height = 1200) {
        setContent {
            NurTheme {
                MemorizeScreenDesktop(pal = pal)
            }
        }

        waitForIdle()

        // 1. Assert title & header
        onNodeWithText("MEMORIZATION HUB").assertExists()
        onNodeWithText("Hifdh Tracker").assertExists()

        // 2. Assert stat cards ("SURAHS", "AYAHS", "PROGRESS")
        onNodeWithText("SURAHS").assertExists()
        onAllNodesWithText("AYAHS").onFirst().assertExists()
        onNodeWithText("PROGRESS").assertExists()

        // 3. Assert action buttons ("Test My Hifdh", "Set Goal")
        onNodeWithText("Test My Hifdh").assertExists()
        onNodeWithText("Set Goal").assertExists()

        // 4. Assert spaced repetition queue cards ("Sabaq", "Sabqi", "Manzil")
        onNodeWithText("Sabaq", substring = true).assertExists()
        onNodeWithText("Sabqi", substring = true).assertExists()
        onNodeWithText("Manzil", substring = true).assertExists()

        // 5. Assert tabs ("Surahs", "Ajzaa (Juz)")
        onNodeWithText("Surahs").assertExists()
        onNodeWithText("Ajzaa (Juz)").assertExists()

        // 6. Save screenshot to desktop/build/screenshots/memorize_parity.png
        val image: ImageBitmap = onRoot().captureToImage()
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)

        val targetFiles = listOf(
            File("desktop/build/screenshots/memorize_parity.png"),
            File("build/screenshots/memorize_parity.png")
        )
        for (targetFile in targetFiles) {
            targetFile.parentFile?.mkdirs()
            ImageIO.write(out, "png", targetFile)
        }
    }
}
