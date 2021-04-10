package com.sprd.ext.grid;

import android.text.TextUtils;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Utilities;
import com.android.launcher3.ui.AbstractLauncherUiTest;
import com.android.launcher3.ui.TaplTestsLauncher3;
import com.android.launcher3.ui.TestViewHelpers;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.multimode.MultiModeController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.preference.ListPreference;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static com.android.launcher3.InvariantDeviceProfile.KEY_IDP_GRID_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class DesktopGridUITest extends AbstractLauncherUiTest {

    private static final String DESKTOP_GRID = "Desktop grid";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Do not test this file if feature is off.
        assumeTrue(FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.get());
        TaplTestsLauncher3.initialize(this);
    }

    @Test
    public void testDesktopGridSettingsItem() throws UiObjectNotFoundException {
        // Open Launcher settings.
        TestViewHelpers.openLauncherSettings();

        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        settings.scrollTextIntoView(DESKTOP_GRID);
        assertTrue(mDevice.wait(Until.hasObject(By.text(DESKTOP_GRID)), DEFAULT_UI_TIMEOUT));

        mDevice.pressBack();
        FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.setForTests(false);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);

        TestViewHelpers.openLauncherSettings();

        settings.scrollTextIntoView(DESKTOP_GRID);
        assertFalse(mDevice.wait(Until.hasObject(By.text(DESKTOP_GRID)), DEFAULT_UI_TIMEOUT));

        FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
    }

    @Test
    public void testChangeDesktopGridOption() throws UiObjectNotFoundException {
        // Open Launcher settings.
        TestViewHelpers.openLauncherSettings();

        // Open Desktop grid list preference
        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        settings.scrollTextIntoView(DESKTOP_GRID);
        mDevice.wait(Until.findObject(By.text(DESKTOP_GRID)), DEFAULT_UI_TIMEOUT).click();
        mDevice.wait(Until.hasObject(By.clazz(ListPreference.class).depth(0)), SHORT_UI_TIMEOUT);

        // Change desktop grid size
        String gridKey = MultiModeController.getKeyByMode(mTargetContext, KEY_IDP_GRID_NAME);
        String curGrid = Utilities.getPrefs(mTargetContext) .getString(gridKey, "");
        assertFalse(TextUtils.isEmpty(curGrid));
        if (!TextUtils.isEmpty(curGrid)) {
            curGrid = curGrid.replace("_by_", "x");
        }
        String toGrid = "4x4";
        if (curGrid.equals(toGrid)) {
            toGrid = "4x5";
        }
        mDevice.wait(Until.findObject(By.text(toGrid)), DEFAULT_UI_TIMEOUT).click();

        mDevice.pressBack();

        String[] gridSize = toGrid.split("x");
        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(mTargetContext);
        assertEquals(Integer.valueOf(gridSize[1]).intValue(), idp.numRows);
        assertEquals(Integer.valueOf(gridSize[0]).intValue(), idp.numColumns);
    }
}
