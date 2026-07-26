package com.nzsk.pureskip.engine

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nzsk.pureskip.rules.ConditionType
import com.nzsk.pureskip.rules.MatchType
import com.nzsk.pureskip.rules.RuleCondition
import com.nzsk.pureskip.rules.RuleProvider
import com.nzsk.pureskip.testing.RecognitionFixtureActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CandidateIdentifierInstrumentedTest {

    @Test
    fun findsVisibleTextButton() {
        withScenario(RecognitionFixtureActivity.SCENARIO_TEXT) {
            val result = identify(
                RuleCondition(ConditionType.TEXT, "跳过广告", MatchType.EXACT),
                requireConfidence = false
            )
            assertNotNull(result.node)
        }
    }

    @Test
    fun findsImageButtonByAccessibilityDescription() {
        withScenario(RecognitionFixtureActivity.SCENARIO_DESCRIPTION) {
            val result = identify(
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "关闭广告", MatchType.EXACT),
                requireConfidence = false
            )
            assertNotNull(result.node)
        }
    }

    @Test
    fun enhancedUniversalRuleFindsAdSdkButtonByResourceIdOnly() {
        withScenario(RecognitionFixtureActivity.SCENARIO_RESOURCE_ID_ONLY) {
            val root = waitForActiveWindowRoot()
            try {
                RuleProvider.initialize()
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                val metrics = context.resources.displayMetrics
                val result = RuleMatcher().match(
                    packageName = "com.example.unlisted",
                    rootNode = root,
                    enhancedEnabled = true,
                    activityName = RecognitionFixtureActivity::class.java.name,
                    windowTitle = root.window?.title?.toString().orEmpty(),
                    screenWidthPx = metrics.widthPixels,
                    screenHeightPx = metrics.heightPixels
                )

                assertTrue(result.matched)
                assertTrue(result.matchedSignals.any { it.startsWith("VIEW_ID:") })
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        }
    }

    @Test
    fun returnsClickableParentForTextChild() {
        withScenario(RecognitionFixtureActivity.SCENARIO_CLICKABLE_PARENT) {
            val result = identify(
                RuleCondition(ConditionType.TEXT, "跳过广告", MatchType.EXACT),
                requireConfidence = false
            )
            assertTrue(result.node?.isClickable == true)
        }
    }

    @Test
    fun doesNotReplaceCloseGlyphWithWholeClickableAdvertisement() {
        withScenario(RecognitionFixtureActivity.SCENARIO_CLICKABLE_AD_PARENT) {
            val result = identify(
                RuleCondition(ConditionType.TEXT, "^[×xX✕]$", MatchType.REGEX),
                requireConfidence = true
            )
            val node = checkNotNull(result.node)
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            val metrics = ApplicationProvider.getApplicationContext<android.content.Context>()
                .resources.displayMetrics

            assertFalse(node.isClickable)
            assertTrue(bounds.width() < metrics.widthPixels / 3)
            assertTrue(bounds.height() < metrics.heightPixels / 3)
        }
    }

    @Test
    fun rejectsTwoEquallyLikelyCloseButtons() {
        withScenario(RecognitionFixtureActivity.SCENARIO_AMBIGUOUS) {
            val result = identify(
                RuleCondition(ConditionType.TEXT, "^[×xX✕]$", MatchType.REGEX),
                requireConfidence = true
            )
            assertNull(result.node)
            assertTrue(result.ambiguous)
        }
    }

    @Test
    fun customViewWithoutSignalsProducesNoCandidate() {
        withScenario(RecognitionFixtureActivity.SCENARIO_CUSTOM) {
            val result = identify(
                RuleCondition(ConditionType.TEXT, "跳过广告", MatchType.EXACT),
                requireConfidence = true
            )
            assertNull(result.node)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun manifestHasAppVisibilityButNoInternetPermission() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = info.requestedPermissions?.toSet().orEmpty()
        assertTrue("android.permission.QUERY_ALL_PACKAGES" in permissions)
        assertFalse("android.permission.INTERNET" in permissions)
    }

    private fun withScenario(
        scenario: String,
        assertion: () -> Unit
    ) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, RecognitionFixtureActivity::class.java)
            .putExtra(RecognitionFixtureActivity.EXTRA_SCENARIO, scenario)
        ActivityScenario.launch<RecognitionFixtureActivity>(intent).use { scenarioHandle ->
            scenarioHandle.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            SystemClock.sleep(200)
            assertion()
        }
    }

    private fun identify(
        condition: RuleCondition,
        requireConfidence: Boolean
    ): CandidateIdentifier.CandidateResult {
        val root = waitForActiveWindowRoot()
        return try {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val metrics = context.resources.displayMetrics
            CandidateIdentifier().findCandidates(
                rootNode = root,
                conditions = listOf(condition),
                windowContext = CandidateIdentifier.WindowContext(
                    activityName = RecognitionFixtureActivity::class.java.name,
                    windowTitle = root.window?.title?.toString().orEmpty(),
                    screenWidthPx = metrics.widthPixels,
                    screenHeightPx = metrics.heightPixels
                ),
                requireConfidence = requireConfidence
            )
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun waitForActiveWindowRoot(): android.view.accessibility.AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val expectedPackage = ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .packageName
        val seenPackages = linkedSetOf<String>()
        repeat(30) {
            val root = automation.rootInActiveWindow
            val rootPackage = root?.packageName?.toString().orEmpty()
            if (rootPackage.isNotBlank()) seenPackages += rootPackage
            if (rootPackage == expectedPackage) return root
            @Suppress("DEPRECATION")
            root?.recycle()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            SystemClock.sleep(100)
        }
        error("未获取到测试应用的无障碍节点: $expectedPackage; 当前窗口=$seenPackages")
    }
}
