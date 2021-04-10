package com.android.quickstep;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.launcher3.LauncherState;
import com.android.launcher3.tapl.Overview;
import com.android.launcher3.tapl.OverviewTask;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sprd.ext.FeatureOption;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TaplTestsLockTask extends TaplTestsQuickstepBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Do not test this file if feature is off.
        assumeTrue(FeatureOption.SPRD_TASK_LOCK_SUPPORT.get());
    }

    @Test
    @PortraitLandscape
    public void testClearAllWhenTaskLocked() {
        startTestApps();
        Overview overview = mLauncher.pressHome().switchToOverview();
        assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
        executeOnLauncher(launcher -> assertTrue("Has tasks",
                getTaskCount(launcher) >= 2));

        OverviewTask task = overview.getCurrentTask();
        assertNotNull("get current task ", task);
        task.lockAndUnlock();
        if (FeatureOption.SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT.get()) {
            overview.dismissAllTasksByBottomButton();
        } else {
            overview.dismissAllTasks();
        }
        // Test dismissing all tasks, remain a locked task.
        waitForState("Switch to Home", LauncherState.NORMAL);
        executeOnLauncher(launcher -> assertEquals("Remain a locked task",
                1, getTaskCount(launcher)));

        // unlock the task.
        overview = mLauncher.pressHome().switchToOverview();
        assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
        task = overview.getCurrentTask();
        assertNotNull("get current task ", task);
        task.lockAndUnlock();
        if (FeatureOption.SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT.get()) {
            overview.dismissAllTasksByBottomButton();
        } else {
            overview.dismissAllTasks();
        }
        waitForState("Switch to Home", LauncherState.NORMAL);
        executeOnLauncher(launcher -> assertEquals("clear all the tasks",
                0, getTaskCount(launcher)));
    }

    @Test
    @PortraitLandscape
    public void testDismissWhenTaskLocked() {
        startTestApps();
        Overview overview = mLauncher.pressHome().switchToOverview();
        assertTrue("Switch to Overview", isInState(LauncherState.OVERVIEW));
        executeOnLauncher(launcher -> assertTrue("at least 2 tasks",
                getTaskCount(launcher) >= 2));

        final int numTasks = getFromLauncher(launcher -> getTaskCount(launcher));
        OverviewTask task = overview.getCurrentTask();
        assertNotNull("get current task ", task);
        task.lockAndUnlock();
        task.dismiss();
        executeOnLauncher(launcher -> assertEquals("Locked task was not removed  ",
                numTasks, getTaskCount(launcher)));

        task.lockAndUnlock();
        task.dismiss();
        executeOnLauncher(launcher -> assertEquals("dismiss unlock task",
                numTasks - 1, getTaskCount(launcher)));
    }
}
