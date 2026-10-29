package com.nur.quran.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.ImageComposeScene
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.screens.PlannerScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test

/** Temp: render planner states for visual QA (not an assertion suite). */
class PlannerShotTest {

    private val pal = NurPalette(false)

    private fun save(image: ImageBitmap, name: String) {
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)
        val dir = File("build/screenshots")
        dir.mkdirs()
        ImageIO.write(out, "png", File(dir, name))
    }

    private fun chapters(): List<PlanChapter> =
        QuranStore.chapters.map { c ->
            PlanChapter(
                id = c.id,
                versesCount = c.versesCount,
                startPage = c.pages.firstOrNull() ?: 0,
                endPage = c.pages.lastOrNull() ?: 0
            )
        }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun shoot(name: String, w: Int, h: Int, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = w, height = h) {
            NurTheme { Surface(modifier = Modifier.fillMaxSize(), color = pal.white) { content() } }
        }
        val png = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
            ?: error("PNG encode failed")
        val dirs = listOf(File("build/screenshots"), File("desktop/build/screenshots"))
        for (dir in dirs) {
            dir.mkdirs()
            File(dir, name).writeBytes(png.bytes)
        }
        scene.close()
    }

    @Test
    fun `planner intention state renders`() {
        PlannerStore.saveActivePlan(null)
        shoot("planner_intention.png", 1400, 2600) { PlannerScreenDesktop(pal = pal) }
    }

    @Test
    fun `planner active today state renders`() {
        val plan = PlannerEngine.buildReadingPlanner(
            unitType = "page",
            durationDays = 30,
            startDate = java.time.LocalDate.now()
                .minusDays(3)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            startUnit = 1,
            endUnit = 604,
            customTitle = "Steady Journey",
            chapters = chapters()
        )
        PlannerStore.saveActivePlan(plan)
        shoot("planner_active_today.png", 1400, 1700) { PlannerScreenDesktop(pal = pal) }
        val todayStr = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        shoot("planner_progress_tab.png", 1400, 2200) {
            com.nur.quran.desktop.ui.planner.ProgressTab(
                pal = pal,
                plan = plan,
                todayStr = todayStr,
                onAdoptPlan = {}
            )
        }
        shoot("planner_journal_tab.png", 1400, 1600) {
            com.nur.quran.desktop.ui.planner.JournalTab(
                pal = pal,
                plan = plan,
                onOpenSurah = { _, _ -> }
            )
        }
    }
}
