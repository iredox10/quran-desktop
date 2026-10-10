package com.nur.quran.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.screens.PlannerReaderScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors

/** Watchdog repro: reader with an ACTIVE plan present. */
class ReaderHangTest {

    @get:Rule
    val rule = createComposeRule()

    private val pal = NurPalette(false)

    private fun chapters(): List<PlanChapter> =
        QuranStore.chapters.map { c ->
            PlanChapter(
                id = c.id,
                versesCount = c.versesCount,
                startPage = c.pages.firstOrNull() ?: 0,
                endPage = c.pages.lastOrNull() ?: 0
            )
        }

    @Test
    fun `reader with active plan settles`() {
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
        rule.setContent {
            NurTheme {
                PlannerReaderScreenDesktop(
                    dayDate = PlannerEngine.formatPlannerDate(),
                    pal = pal
                )
            }
        }
        val watchdog = Executors.newSingleThreadExecutor()
        watchdog.submit {
            Thread.sleep(15_000)
            println("READERHANG: still rendering after 15s")
            Thread.getAllStackTraces().forEach { (t, st) ->
                if (t.name.contains("AWT-EventQueue")) {
                    st.take(14).forEach { println("READERHANG: $t: $it") }
                }
            }
            Runtime.getRuntime().halt(42)
        }
        rule.waitForIdle()
        rule.onRoot().captureToImage()
        watchdog.shutdownNow()
        println("READERHANG: settled OK")
    }
}
