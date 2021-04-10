package com.android.quickstep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.launcher3.LauncherState;
import com.android.launcher3.tapl.Overview;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sprd.ext.FeatureOption;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TaplTestsBottomClearAll extends TaplTestsQuickstepBase {

    @Test
    @PortraitLandscape
    public void testShowBottomClearAllButton() throws Exception {
        if (FeatureOption.SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT.get()) {
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
            assertFalse("No task, no button", hasObject("clear_all_button"));

            startTestApps();
            Overview overview = mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
            assertTrue("Has clear_all_button", hasObject("clear_all_button"));

            final Integer numTasks = getFromLauncher(launcher -> getTaskCount(launcher));
            assertTrue("Has tasks", numTasks >= 2);
            overview.flingForwardToEnd(numTasks);
            assertFalse("Should not have another clear_all", hasObject("clear_all"));

            // Test dismissing all tasks.
            overview.dismissAllTasksByBottomButton();
            waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
            executeOnLauncher(
                    launcher -> assertEquals("Still have tasks after dismissing all",
                            0, getTaskCount(launcher)));
            assertFalse("No task, no button", hasObject("clear_all_button"));
        } else {
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
            assertFalse("No task, no button", hasObject("clear_all_button"));

            startTestApps();
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
            assertFalse("Not support, no button", hasObject("clear_all_button"));
        }
    }
}
