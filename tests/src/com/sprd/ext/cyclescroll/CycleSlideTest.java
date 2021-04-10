package com.sprd.ext.cyclescroll;

import android.graphics.Point;
import android.view.WindowManager;

import androidx.preference.Preference;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.ui.AbstractLauncherUiTest;
import com.android.launcher3.ui.TestViewHelpers;
import com.android.launcher3.util.rule.SettingsActivityRule;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Objects;

import static com.sprd.ext.LauncherSettingsExtension.PREF_CIRCULAR_SLIDE_KEY;
import static com.sprd.ext.LauncherSettingsExtension.PREF_ENABLE_MINUS_ONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by unisoc on 2019/12/27
 */
@RunWith(AndroidJUnit4.class)
public class CycleSlideTest extends AbstractLauncherUiTest {
    private static final String TAG = "CycleSlideTest";
    private static final String SECOND_SCREEN_ICON = "Phone";
    private static final String FIRST_SCREEN_ICON = "Settings";
    private static final String MINUES_NAME = "Show Google App";
    private static final String CYCLE_NAME = "Home screen loop";

    private final SettingsActivityRule mSettingsActivityMonitor = new SettingsActivityRule();

    @Rule
    public TestRule mSettingsSensitiveRules = RuleChain.
            outerRule(mSettingsActivityMonitor);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        initialize();

        // Do not test this file if feature is off.
        assumeTrue(FeatureOption.SPRD_CYCLE_SCROLL_SUPPORT.get());
    }

    public void initialize() throws Exception {
        clearLauncherData();
        mDevice.pressHome();
        waitForLauncherCondition("Launcher didn't start", launcher -> launcher != null);
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        waitForResumed("Launcher internal state is still Background");
        // Check that we switched to home.
        mLauncher.getWorkspace();
    }

    @Test
    public void testCycleSetting() {
        // Feature close
        FeatureOption.SPRD_CYCLE_SCROLL_SUPPORT.setForTests(false);
        // Init LauncherAppMonitor
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor.getInstance(InstrumentationRegistry.getTargetContext());
        // Start SettingsActivity
        mSettingsActivityMonitor.startHomeSetting();
        // Find Feature item.
        Preference pref = mSettingsActivityMonitor.getPreference(
                PREF_CIRCULAR_SLIDE_KEY);
        assertNull(pref);
        mSettingsActivityMonitor.getActivity().finish();

        // Feature open
        FeatureOption.SPRD_CYCLE_SCROLL_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor.getInstance(InstrumentationRegistry.getTargetContext());
        mSettingsActivityMonitor.startHomeSetting();
        pref = mSettingsActivityMonitor.getPreference(
                PREF_CIRCULAR_SLIDE_KEY);
        assertNotNull(pref);
    }

    @Test
    public void testOnePageCycleSlide() throws UiObjectNotFoundException {
        assumeFalse(isInSingleMode);
        executeOnLauncher(launcher -> launcher.getWorkspace().removeAllWorkspaceScreens());
        mDevice.pressHome();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        mLauncher.getWorkspace().switchToAllApps().getAppIcon(FIRST_SCREEN_ICON).dragToWorkspace();
        setCycleSlideEnabled(false);
        assertCycleSlideForbid();

        setCycleSlideEnabled(true);
        assertCycleSlideForbid();
    }

    @Test
    public void testMultiPageCycleSlide() throws UiObjectNotFoundException {
        mDevice.pressHome();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        if (!isInSingleMode) {
            mLauncher.getWorkspace().switchToAllApps().getAppIcon(FIRST_SCREEN_ICON).dragToWorkspace();
        }
        // Drag Phone icon to a new Screen.
        Point point = getRealDisplaySize();
        point.x -= 10;
        point.y = point.y >> 1;
        mLauncher.getWorkspace().getHotseatAppIcon(SECOND_SCREEN_ICON).dragToWorkspacePosition(point);
        executeOnLauncher(launcher -> {
            if (launcher != null) {
                assertTrue(launcher.getWorkspace().getChildCount() > 1);
            }
        });

        setCycleSlideEnabled(false);
        assertCycleSlideForbid();

        setCycleSlideEnabled(true);
        assertCycleSlideEnabled();
    }

    @Test
    public void testMultiPageCycleSlide_hasOverLay() throws UiObjectNotFoundException {
        mDevice.pressHome();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        setCycleSlideEnabled(true);
        if (!isInSingleMode) {
            mLauncher.getWorkspace().switchToAllApps().getAppIcon(FIRST_SCREEN_ICON).dragToWorkspace();
        }
        Launcher.LauncherOverlay overlay = mock(Launcher.LauncherOverlay.class);
        // Drag Phone icon to a new Screen.
        Point point = getRealDisplaySize();
        point.x -= 10;
        point.y = point.y >> 1;
        mLauncher.getWorkspace().getHotseatAppIcon(SECOND_SCREEN_ICON).dragToWorkspacePosition(point);
        executeOnLauncher(launcher -> {
            if (launcher != null) {
                // Set a Launcher overlay.
                launcher.getWorkspace().setLauncherOverlay(overlay);
                assertTrue(launcher.getWorkspace().getChildCount() > 1);
            }
        });
        assertCycleSlideForbid();
    }

    private void setCycleSlideEnabled(boolean isOpen) throws UiObjectNotFoundException {
        TestViewHelpers.openLauncherSettings();
        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));

        // Find Google now item. Check and make it to closed.
        boolean minusOpen = Utilities.getPrefs(mTargetContext).getBoolean(PREF_ENABLE_MINUS_ONE, false);
        if (minusOpen) {
            settings.scrollTextIntoView(MINUES_NAME);
            UiObject2 minusPref = mDevice.wait(Until.findObject(By.text(MINUES_NAME)), DEFAULT_UI_TIMEOUT);
            assumeNotNull(minusPref);
            minusPref.click();
        }

        // Check cycleItem is enable.
        settings.scrollTextIntoView(CYCLE_NAME);
        UiObject2 cyclePref = mDevice.wait(Until.findObject(By.text(CYCLE_NAME)), DEFAULT_UI_TIMEOUT);
        if (isOpen != Utilities.getPrefs(mTargetContext).getBoolean(PREF_CIRCULAR_SLIDE_KEY,
                mTargetContext.getResources().getBoolean(R.bool.default_circle_slide))) {
            cyclePref.click();
        }
        mDevice.pressBack();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
    }

    private void assertCycleSlideForbid() {
        mDevice.pressHome();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        UiObject2 referIcon = mLauncher.getWorkspace().getWorkspaceAppIcon(FIRST_SCREEN_ICON).getObject();
        Point beforePoint = referIcon.getVisibleCenter();
        int centerY = getRealDisplaySize().y / 2;
        Point start = new Point(10, centerY);
        Point dest = new Point(getRealDisplaySize().x - 10, centerY);
        mLauncher.getWorkspace().touchLeftToRight(start, dest, false);

        Point afterPoint = referIcon.getVisibleCenter();
        assertTrue((afterPoint.x - beforePoint.x) < (dest.x - start.x) / 3);
    }

    private void assertCycleSlideEnabled() {
        mDevice.pressHome();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        executeOnLauncher(launcher -> {
            if (launcher != null) {
                assertEquals(0, launcher.getWorkspace().getCurrentPage());
            }
        });

        int centerY = getRealDisplaySize().y / 2;
        Point start = new Point(10, centerY);
        Point dest = new Point(getRealDisplaySize().x - 10, centerY);
        mLauncher.getWorkspace().touchLeftToRight(start, dest, true);
        mDevice.wait(Until.findObject(By.text(SECOND_SCREEN_ICON)), AbstractLauncherUiTest.DEFAULT_UI_TIMEOUT);

        executeOnLauncher(launcher -> {
            if (launcher != null) {
                assertNotEquals(0, launcher.getWorkspace().getCurrentPage());
            }
        });
    }

    private Point getRealDisplaySize() {
        final Point size = new Point();
        Objects.requireNonNull(mTargetContext.getSystemService(WindowManager.class)).getDefaultDisplay().getRealSize(size);
        return size;
    }
}