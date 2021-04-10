package com.sprd.ext.gestures;

import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.android.launcher3.LauncherState;
import com.android.launcher3.ui.AbstractLauncherUiTest;
import com.android.launcher3.ui.TestViewHelpers;
import com.sprd.ext.FeatureOption;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by unisoc on 2019/11/13
 */
@RunWith(AndroidJUnit4.class)
public class PullDownGesturesTest extends AbstractLauncherUiTest {
    private static final long UI_TIMEOUT = 3000;

    private static final String PULL_DOWN_TITLE = "Pull down action";
    private static final String OPEN_SEARCH = "Open search";
    private static final String OPEN_NOTIFICATION = "Show notifications";
    private static final String OPEN_NONE = "Do nothing";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        FeatureOption.SPRD_GESTURE_SUPPORT.setForTests(true);
        initialize();
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
    public void testPullDownToOpenNotification() throws Throwable {
        // Open settings activity
        TestViewHelpers.openLauncherSettings();

        // select pull down action
        selectPullDownAction(OPEN_NOTIFICATION);

        // Back to home.
        mDevice.pressHome();

        // Swip down in workspace.
        mLauncher.getWorkspace().swipDownInWorkspace();

        // Verify notification is open.
        assertTrue("Notification panel is not open : ", mDevice.wait(Until.hasObject(
                By.desc("Bluetooth.")),
                UI_TIMEOUT));
    }

    @Test
    public void testPullDownToOpenSearch() throws Throwable {
        // Open settings activity
        TestViewHelpers.openLauncherSettings();

        // select pull down action
        selectPullDownAction(OPEN_SEARCH);

        // Back to home.
        mDevice.pressHome();

        // Swip down in workspace.
        mLauncher.getWorkspace().swipDownInWorkspace();

        // Verify search is open.
        executeOnLauncher(launcher -> assertTrue(
                "Launcher activity is the top activity; expecting another activity to be the top "
                        + "one",
                isInBackground(launcher)));
    }

    @Test
    public void testPullDownToOpenNone() throws Throwable {
        // Open settings activity
        TestViewHelpers.openLauncherSettings();

        // select pull down action
        selectPullDownAction(OPEN_NONE);

        // Back to home.
        mDevice.pressHome();

        // Swip down in workspace.
        mLauncher.getWorkspace().swipDownInWorkspace();

        // Verify nothing is open.
        assertNotNull("Pull down to open none failed : ", TestViewHelpers.findViewByEntryName("search_container_workspace"));
    }

    private void selectPullDownAction(String action) throws Throwable {
        // Check if pull down action is selected.
        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        boolean isSelected = settings.scrollTextIntoView(action);
        if (!isSelected) {
            // If not selected, select it.
            settings.scrollTextIntoView(PULL_DOWN_TITLE);
            mDevice.wait(Until.findObject(By.text(PULL_DOWN_TITLE)), UI_TIMEOUT).click();
            mDevice.wait(Until.findObject(By.text(action)), UI_TIMEOUT).click();
        }
    }

}