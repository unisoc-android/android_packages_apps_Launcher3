package com.sprd.ext.dynamicicon;

import com.android.launcher3.ui.AbstractLauncherUiTest;
import com.android.launcher3.ui.TaplTestsLauncher3;
import com.android.launcher3.ui.TestViewHelpers;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class DynamicIconUiTest extends AbstractLauncherUiTest {

    private static final String DYNAMIC_ICON = "Dynamic icon";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        assumeTrue(FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT.get());
        TaplTestsLauncher3.initialize(this);
    }

    @Test
    public void testDynamicIconSettingsItem() throws UiObjectNotFoundException {
        // Open Launcher settings.
        TestViewHelpers.openLauncherSettings();

        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        settings.scrollTextIntoView(DYNAMIC_ICON);
        assertTrue(mDevice.wait(Until.hasObject(By.text(DYNAMIC_ICON)), DEFAULT_UI_TIMEOUT));

        mDevice.pressBack();
        FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT.setForTests(false);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);

        TestViewHelpers.openLauncherSettings();

        settings.scrollTextIntoView(DYNAMIC_ICON);
        assertFalse(mDevice.wait(Until.hasObject(By.text(DYNAMIC_ICON)), DEFAULT_UI_TIMEOUT));

        FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
    }
}
