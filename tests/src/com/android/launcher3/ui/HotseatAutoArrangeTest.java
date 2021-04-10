package com.android.launcher3.ui;

import android.graphics.Point;

import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.tapl.AllApps;
import com.android.launcher3.tapl.AppIconMenuItem;
import com.android.launcher3.util.Condition;
import com.android.launcher3.util.Wait;
import com.android.launcher3.widget.WidgetCell;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.grid.HotseatController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Created by unisoc on 2019/11/12
 */
@RunWith(AndroidJUnit4.class)
public class HotseatAutoArrangeTest extends AbstractLauncherUiTest {

    private static final String PHONE_NAME = "Phone";
    private static final String APP_NAME = "LauncherTestApp";

    private int mHotseatNums;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        initialize(this);

        // Do not test this file if feature is off.
        assumeTrue(FeatureOption.SPRD_HOTSEAT_ICON_ADAPTIVE_LAYOUT.get());
    }

    public static void initialize(AbstractLauncherUiTest test) throws Exception {
        test.clearLauncherData();
        test.mDevice.pressHome();
        test.waitForLauncherCondition("Launcher didn't start", launcher -> launcher != null);
        test.waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
        test.waitForResumed("Launcher internal state is still Background");
        // Check that we switched to home.
        test.mLauncher.getWorkspace();
    }

    @Test
    @PortraitLandscape
    public void testClearEmptyGrid() {
        mHotseatNums = 0;
        executeOnLauncher(launcher -> getHotseatNums(launcher));

        int numsBef = mHotseatNums;
        mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME).dragToWorkspace();
        executeOnLauncher(launcher -> getHotseatNums(launcher));
        assertTrue("Empty grid is clearred : ", mHotseatNums == (numsBef - 1));
    }

    @Test
    @PortraitLandscape
    public void testResetDragView() {
        assertNotNull("Check Phone is exist : ", mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME));

        mLauncher.getWorkspace().interruptDragHotseatIconToWorkspace(PHONE_NAME);
        assertNotNull("Failed to reset drag view to hotseat : ", mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME));
    }

    @Test
    @PortraitLandscape
    public void testInsetNewIconToHotseat() {
        assertNotNull("Check Phone is exist : ", mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME));

        // Ensure hotseat icon is not full
        mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME).dragToWorkspace();
        assertTrue("Hotseat is full : ", hotseatCanInsert());

        mLauncher.getWorkspace().dragIconToHotseatIfCanInset(PHONE_NAME);
        assertNotNull(mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME));
    }

    /**
     * Do not test this case in single mode without allapps.
     */
    @Test
    public void testCreateFolderInHotseat() {
        // There's no allapps in single mode, so ignore.
        assumeFalse(isInSingleMode);

        assertFalse("Hotseat is not full : ", hotseatCanInsert());
        Point phoneCenter = TestViewHelpers.getAppVisibleCenter(PHONE_NAME);
        final AllApps allApps = mLauncher.
                getWorkspace().
                switchToAllApps();
        allApps.freeze();
        try {
            final AppIconMenuItem menuItem = allApps.
                    getAppIcon(APP_NAME).
                    openMenu().
                    getMenuItem(0);
            menuItem.dragToHotseatOverExistIcon(phoneCenter);
        } finally {
            allApps.unfreeze();
        }
        assertNotNull(mLauncher.getWorkspace().getHotseatFolder("Folder: "));
    }

    @Test
    public void testDragDeepShortcutToHotseat() {
        // Ensure hotseat icon is not full
        mLauncher.getWorkspace().getHotseatAppIcon(PHONE_NAME).dragToWorkspace();
        assertTrue("Hotseat is full : ", hotseatCanInsert());

        // Open widget tray and wait for load complete.
        final UiObject2 widgetContainer = TestViewHelpers.openWidgetsTray();
        Wait.atMost(null, Condition.minChildCount(widgetContainer, 2), DEFAULT_UI_TIMEOUT);

        // Drag widget to homescreen
        UiObject2 widget = scrollAndFind(widgetContainer, By.clazz(WidgetCell.class)
                .hasDescendant(By.text("Settings shortcut")));
        TestViewHelpers.dragToHotseat(widget);
        // click "VPN" item
        getDevice().wait(Until.findObject(By.text("VPN")),
                AbstractLauncherUiTest.DEFAULT_UI_TIMEOUT).click();
        assertNotNull(mLauncher.getWorkspace().getHotseatAppIcon("VPN"));
    }

    @After
    public void cleanup() throws Exception {
        clearLauncherData();
    }

    private void getHotseatNums(Launcher launcher) {
        mHotseatNums = launcher.getHotseat().getShortcutsAndWidgets().getChildCount();
        assertTrue("Hotseat nums is not 0 : ", mHotseatNums != 0);
    }

    private boolean hotseatCanInsert() {
        return getFromLauncher(launcher -> {
            HotseatController hc = LauncherAppMonitor.getInstance(mTargetContext).getHotseatController();
            return hc.canInsert(launcher);
        });
    }
}