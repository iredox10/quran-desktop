package com.nur.quran.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nur.quran.desktop.data.PlannerStore
import com.nur.quran.desktop.data.QuranStore
import com.nur.quran.desktop.ui.screens.PlannerScreenDesktop
import com.nur.quran.desktop.ui.theme.NurPalette
import com.nur.quran.desktop.ui.theme.NurTheme
import com.nur.quran.shared.PlanChapter
import com.nur.quran.shared.PlannerEngine
import com.nur.quran.shared.ReadingPlan
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTestApi::class)
class PlannerInteractionTest {

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

    private fun buildSamplePlan(title: String = "Steady Journey"): ReadingPlan {
        val start = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return PlannerEngine.buildReadingPlanner(
            unitType = "page",
            durationDays = 60,
            startDate = start,
            startUnit = 1,
            endUnit = 604,
            customTitle = title,
            chapters = chapters()
        )
    }

    @Test
    fun testExistingPlanClickBeginJourney() {
        val existing = buildSamplePlan("Steady Journey")
        PlannerStore.saveActivePlan(existing)
        PlannerStore.saveAllPlans(listOf(existing))

        rule.setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        rule.waitForIdle()

        // If it starts in active view, let's click Back to get to intention view
        val backBtn = rule.onAllNodesWithContentDescription("Back")
        if (backBtn.fetchSemanticsNodes().isNotEmpty()) {
            backBtn.onFirst().performClick()
            rule.waitForIdle()
        }

        // Now we are in Intention view
        rule.onNodeWithText("Begin Journey").assertExists()
        // Click Begin Journey on Steady Journey
        rule.onNodeWithText("Begin Journey").performClick()
        rule.waitForIdle()

        // Zero-progress existing plan begins journey immediately and opens Today tab!
        rule.onNodeWithText("Today").assertExists()
    }

    @Test
    fun testClickPlanTemplateStartsPlan() {
        PlannerStore.saveActivePlan(null)
        PlannerStore.saveAllPlans(emptyList())

        rule.setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        rule.waitForIdle()

        // Click template
        rule.onNodeWithText("Ramadan Last 10", substring = true).performScrollTo().performClick()
        rule.waitForIdle()

        // Preview modal button "Create Plan" is pinned in footer
        rule.onNodeWithText("Create Plan", substring = true).assertExists()
        rule.onNodeWithText("Create Plan", substring = true).performClick()
        rule.waitForIdle()

        // Should switch to active view Today tab!
        rule.onNodeWithText("Today").assertExists()
    }

    @Test
    fun testClickExistingPlanInMyPlansList() {
        val existing = buildSamplePlan("Steady Journey")
        PlannerStore.saveActivePlan(existing)
        PlannerStore.saveAllPlans(listOf(existing))

        rule.setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        rule.waitForIdle()

        // If it starts in active view, go Back to intention view
        val backBtn = rule.onAllNodesWithContentDescription("Back")
        if (backBtn.fetchSemanticsNodes().isNotEmpty()) {
            backBtn.onFirst().performClick()
            rule.waitForIdle()
        }

        // Now in Intention view, find "Steady Journey" card in My Plans or "Continue my active plan"
        println("Clicking 'Continue my active plan'...")
        rule.onNodeWithText("Continue my active plan", substring = true).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Today").assertExists()
    }

    @Test
    fun testCreateCustomPlanFlow() {
        PlannerStore.saveActivePlan(null)
        PlannerStore.saveAllPlans(emptyList())

        rule.setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        rule.waitForIdle()

        // Click + (Create custom plan)
        rule.onNodeWithContentDescription("Create custom plan").performMouseInput { click() }
        rule.waitForIdle()

        // Check if modal is open
        rule.onNodeWithText("CREATE PLAN").assertExists()

        // CREATE PLAN is pinned in footer
        rule.onNodeWithText("CREATE PLAN").performMouseInput { click() }
        rule.waitForIdle()

        println("PlannerStore active plan: ${PlannerStore.getActivePlan()?.title}")
        println("PlannerStore all plans: ${PlannerStore.getAllPlans().map { it.title }}")

        // Should now be on Today tab
        rule.onNodeWithText("Today").assertExists()
    }

    @Test
    fun testFreshStartClickBeginJourney() {
        PlannerStore.saveActivePlan(null)
        PlannerStore.saveAllPlans(emptyList())

        rule.setContent {
            NurTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = pal.white) {
                    PlannerScreenDesktop(pal = pal)
                }
            }
        }
        rule.waitForIdle()

        // Verify we are in Intention view (no "Today" tab, "Set Your Intention" visible)
        rule.onNodeWithText("Set Your Intention").assertExists()
        rule.onNodeWithText("Begin Journey").assertExists()

        println("Clicking Begin Journey with performMouseInput { click() }...")
        rule.onNodeWithText("Begin Journey").performMouseInput { click() }
        rule.waitForIdle()

        val active = PlannerStore.getActivePlan()
        println("Active plan after click: ${active?.title}")
        println("All plans after click: ${PlannerStore.getAllPlans().map { it.title }}")

        // Should switch to active view Today tab!
        rule.onNodeWithText("Today").assertExists()
    }

    @Test
    fun testRealJsonDeserialization() {
        val active = PlannerStore.getActivePlan()
        val all = PlannerStore.getAllPlans()
        println("REAL ACTIVE PLAN: $active")
        println("REAL ALL PLANS: ${all.size}")
    }
}
