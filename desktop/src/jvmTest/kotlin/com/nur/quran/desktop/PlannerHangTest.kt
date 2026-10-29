package com.nur.quran.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.captureToImage
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.screens.PlannerScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors

/** Watchdog repro: setContent + waitForIdle on the ACTIVE plan view. */
class PlannerHangTest {

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
    fun `active view settles with waitForIdle`() {
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
        rule.setContent { NurTheme { PlannerScreenDesktop(pal = pal) } }
        val watchdog = Executors.newSingleThreadExecutor()
        watchdog.submit {
            Thread.sleep(20_000)
            println("HANGTEST: still rendering after 20s - dumping")
            Thread.getAllStackTraces().forEach { (t, st) ->
                if (t.name.contains("AWT-EventQueue")) {
                    st.take(14).forEach { println("HANGTEST: $t: $it") }
                }
            }
            Runtime.getRuntime().halt(42)
        }
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onFirst().captureToImage()
        watchdog.shutdownNow()
        println("HANGTEST: settled OK")
    }
}
