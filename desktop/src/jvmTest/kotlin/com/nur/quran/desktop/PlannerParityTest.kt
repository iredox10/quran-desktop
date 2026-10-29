package com.nur.quran.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.planner.AdjustPaceDialog
import com.nur.quran.desktop.ui.planner.JournalTab
import com.nur.quran.desktop.ui.planner.ProgressTab
import com.nur.quran.desktop.ui.planner.RebalanceDialog
import com.nur.quran.desktop.ui.screens.PlannerScreenDesktop
import com.nur.quran.desktop.ui.planner.PlannerSettingsDialog
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import org.junit.BeforeClass
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlannerParityTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun markWelcomed() {
            java.util.prefs.Preferences.userRoot().node("welcome_prefs").putBoolean("seen", true)
        }
    }

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

    private fun androidx.compose.ui.test.ComposeUiTest.captureBestImage(): ImageBitmap {
        val roots = onAllNodes(isRoot()).fetchSemanticsNodes()
        return if (roots.size > 1) {
            onAllNodes(isRoot())[roots.size - 1].captureToImage()
        } else {
            onAllNodes(isRoot()).onFirst().captureToImage()
        }
    }

    private fun saveScreenshot(image: ImageBitmap, name: String) {
        val pixels = image.toPixelMap()
        val out = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, pixels.width, pixels.height, pixels.buffer, 0, pixels.width)

        val targetFiles = listOf(
            File("desktop/build/screenshots/$name"),
            File("build/screenshots/$name")
        )
        for (targetFile in targetFiles) {
            targetFile.parentFile?.mkdirs()
            ImageIO.write(out, "png", targetFile)
        }
    }

    private fun buildSamplePlan(durationDays: Int = 30, daysAgo: Long = 0): ReadingPlan {
        val start = LocalDate.now().minusDays(daysAgo)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return PlannerEngine.buildReadingPlanner(
            unitType = "page",
            durationDays = durationDays,
            startDate = start,
            startUnit = 1,
            endUnit = 604,
            customTitle = "Steady Journey",
            chapters = chapters()
        )
    }

    @Test
    fun testIntentionViewParity() = runDesktopComposeUiTest(width = 1280, height = 2400) {
        PlannerStore.saveActivePlan(null)
        PlannerStore.saveAllPlans(emptyList())

        setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        waitForIdle()

        // 1. Hero & Subtitle
        onNodeWithText("THE FIRST STEP", substring = true).assertExists()
        onNodeWithText("Set Your Intention", substring = true).assertExists()
        onNodeWithText("Choose a pace that resonates", substring = true).assertExists()

        // 2. 3 Pace Cards
        onNodeWithText("Ramadan Pace", substring = true).assertExists()
        onNodeWithText("Steady Journey", substring = true).assertExists()
        onNodeWithText("MOST BALANCED", substring = true).assertExists()
        onNodeWithText("Devotional Path", substring = true).assertExists()
        onAllNodesWithText("pages per day", substring = true).onFirst().assertExists()
        onAllNodesWithText("pages per prayer", substring = true).onFirst().assertExists()
        onNodeWithText("Begin Journey", substring = true).assertExists()
        onAllNodesWithText("Select Path", substring = true).onFirst().assertExists()

        // 3. Template Library
        onNodeWithText("Plan Templates", substring = true).assertExists()
        onNodeWithText("CURATED PATHS", substring = true).assertExists()

        // 4. Custom Plan Section
        onNodeWithText("My Plans", substring = true).assertExists()
        onNodeWithContentDescription("Create custom plan").assertExists()

        // 5. Prophetic Wisdom footer
        onNodeWithText("PROPHETIC WISDOM", substring = true).assertExists()

        // 6. Screenshot
        val image = onAllNodes(isRoot()).onFirst().captureToImage()
        saveScreenshot(image, "planner_intention.png")
    }

    @Test
    fun testActiveViewTodayTabParity() = runDesktopComposeUiTest(width = 1280, height = 1800) {
        val plan = buildSamplePlan(durationDays = 30, daysAgo = 0).copy(lastReadPage = 1)
        PlannerStore.saveActivePlan(plan)
        PlannerStore.saveAllPlans(listOf(plan))

        setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        waitForIdle()

        // 1. Navigation Tabs
        onNodeWithText("Today").assertExists()
        onNodeWithText("Progress").assertExists()
        onNodeWithText("Journal").assertExists()

        // 2. RingProgress hero
        onAllNodesWithText("%", substring = true).onFirst().assertExists()
        onAllNodesWithText("PAGES TODAY", substring = true).onFirst().assertExists()

        // 3. Daily Ritual section & controls
        onNodeWithText("Daily Ritual", substring = true).assertExists()
        onNodeWithContentDescription("Share progress").assertExists()
        onNodeWithContentDescription("Planner settings").assertExists()

        // 4. Prayer slots
        onAllNodesWithText("Fajr", substring = true).onFirst().assertExists()
        onAllNodesWithText("Dhuhr", substring = true).onFirst().assertExists()
        onAllNodesWithText("Asr", substring = true).onFirst().assertExists()
        onAllNodesWithText("Maghrib", substring = true).onFirst().assertExists()
        onAllNodesWithText("Isha", substring = true).onFirst().assertExists()

        // 5. CTA button ("Start" or "Resume")
        onAllNodesWithText("Resume", substring = true).onFirst().assertExists()

        // 6. Screenshot
        val image = onAllNodes(isRoot()).onFirst().captureToImage()
        saveScreenshot(image, "planner_active_today.png")
    }

    @Test
    fun testProgressTabParity() = runDesktopComposeUiTest(width = 1280, height = 2200) {
        val plan = buildSamplePlan(durationDays = 30, daysAgo = 3)
        PlannerStore.saveActivePlan(plan)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    ProgressTab(
                        pal = pal,
                        plan = plan,
                        todayStr = todayStr,
                        onAdoptPlan = {}
                    )
                }
            }
        }
        waitForIdle()

        // 1. Timeline section header & counts
        onNodeWithText("Timeline", substring = true).assertExists()
        onNodeWithText("done)", substring = true).assertExists()

        // 2. Timeline actions
        onNodeWithText("Rebalance").assertExists()
        onNodeWithText("Adjust").assertExists()

        // 3. Timeline legend
        onNodeWithText("Done").assertExists()
        onNodeWithText("Today").assertExists()
        onNodeWithText("Partial").assertExists()
        onNodeWithText("Missed").assertExists()
        onNodeWithText("Upcoming").assertExists()

        // 4. Performance Insights cards
        onNodeWithText("Performance Insights", substring = true).assertExists()
        onNodeWithText("ON-TIME COMPLETION", substring = true).assertExists()
        onNodeWithText("CATCH-UPS USED", substring = true).assertExists()
        onNodeWithText("CURRENT PACE", substring = true).assertExists()

        // 5. Weekly Progress summary
        onNodeWithText("Weekly Progress", substring = true).assertExists()

        // 6. Action Required (due to past start date)
        onNodeWithText("Action Required", substring = true).assertExists()
        onNodeWithText("Shift Deadlines (Extend Plan)", substring = true).assertExists()
        onNodeWithText("Redistribute Missed Pages", substring = true).assertExists()

        // 7. Screenshot
        val image = onAllNodes(isRoot()).onFirst().captureToImage()
        saveScreenshot(image, "planner_progress_tab.png")
    }

    @Test
    fun testJournalTabParity() = runDesktopComposeUiTest(width = 1280, height = 1600) {
        val plan = buildSamplePlan(durationDays = 30, daysAgo = 0)
        PlannerStore.saveActivePlan(plan)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // Add a sample reflection and verse bookmark
        PlannerStore.saveReflection(plan.id, todayStr, "Felt profound peace while reflecting on Ayat al-Kursi.")
        PlannerStore.toggleMark(plan.id, "2:255", "Al-Baqarah")

        setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    JournalTab(
                        pal = pal,
                        plan = plan,
                        onOpenSurah = { _, _ -> }
                    )
                }
            }
        }
        waitForIdle()

        // 1. Reflections section
        onNodeWithText("Daily Reflections", substring = true).assertExists()
        onNodeWithText("Felt profound peace while reflecting on Ayat al-Kursi.", substring = true).assertExists()

        // 2. Highlights / Bookmarks section
        onNodeWithText("Plan Highlights", substring = true).assertExists()
        onNodeWithText("Al-Baqarah", substring = true).assertExists()
        onNodeWithText("2:255", substring = true).assertExists()

        // 3. Screenshot
        val image = onAllNodes(isRoot()).onFirst().captureToImage()
        saveScreenshot(image, "planner_journal_tab.png")
    }

    @Test
    fun testModalsAndDialogsParity() = runDesktopComposeUiTest(width = 1280, height = 1400) {
        val plan = buildSamplePlan(durationDays = 30, daysAgo = 0)

        // 1. Test AdjustPaceDialog
        setContent {
            NurTheme {
                AdjustPaceDialog(
                    pal = pal,
                    plan = plan,
                    onDismiss = {},
                    onApply = {}
                )
            }
        }
        waitForIdle()

        onNodeWithText("Adjust Pace", substring = true).assertExists()
        onNodeWithText("Change how many days you want to complete", substring = true).assertExists()
        onNodeWithText("NEW TOTAL DAYS", substring = true).assertExists()
        onNodeWithText("Apply").assertExists()
        onNodeWithText("Cancel").assertExists()

        val adjustImage = captureBestImage()
        saveScreenshot(adjustImage, "planner_dialog_adjust_pace.png")

        // 2. Test RebalanceDialog
        setContent {
            NurTheme {
                RebalanceDialog(
                    pal = pal,
                    onDismiss = {},
                    onPick = {}
                )
            }
        }
        waitForIdle()

        onNodeWithText("Rebalance Plan", substring = true).assertExists()
        onNodeWithText("You missed some pages from past assignments.", substring = true).assertExists()
        onNodeWithText("Extend Schedule", substring = true).assertExists()
        onNodeWithText("Redistribute Reading", substring = true).assertExists()

        val rebalanceImage = captureBestImage()
        saveScreenshot(rebalanceImage, "planner_dialog_rebalance.png")

        // 3. Test PlannerSettingsDialog
        setContent {
            NurTheme {
                PlannerSettingsDialog(
                    pal = pal,
                    city = "Mecca",
                    country = "Saudi Arabia",
                    loading = false,
                    onCityChange = {},
                    onCountryChange = {},
                    onRefresh = {},
                    onDismiss = {}
                )
            }
        }
        waitForIdle()

        onNodeWithText("Planner Settings", substring = true).assertExists()
        onNodeWithText("DAILY PRAYERS", substring = true).assertExists()
        onNodeWithText("READING PREFERENCE", substring = true).assertExists()
        onNodeWithText("INTENTION PROMPTS", substring = true).assertExists()
        onNodeWithText("PRAYER TIMES LOCATION", substring = true).assertExists()
        onNodeWithText("Export to Calendar (.ics)", substring = true).assertExists()
        onNodeWithText("Done", substring = true).assertExists()

        val settingsImage = captureBestImage()
        saveScreenshot(settingsImage, "planner_dialog_settings.png")
    }
}
