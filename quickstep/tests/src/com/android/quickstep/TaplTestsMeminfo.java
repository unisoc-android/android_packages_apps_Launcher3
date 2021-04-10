package com.android.quickstep;

import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.OVERVIEW;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sprd.ext.FeatureOption;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TaplTestsMeminfo extends TaplTestsQuickstepBase {

    @Test
    @PortraitLandscape
    public void testShowMeminfoInEmptyRecentList() {
        if (FeatureOption.SPRD_SHOW_MEMINFO_SUPPORT.get()) {
            mLauncher.pressHome();
            assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            if (!isInSingleMode) {
                mLauncher.pressHome().switchToAllApps();
                assertTrue("Switch to All Apps", isInState(ALL_APPS));
                assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            }
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(OVERVIEW));
            assertTrue("has recents_memoryinfo", hasObject("recents_memoryinfo"));
        } else {
            mLauncher.pressHome();
            assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            if (!isInSingleMode) {
                mLauncher.pressHome().switchToAllApps();
                assertTrue("Switch to All Apps", isInState(ALL_APPS));
                assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            }
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(OVERVIEW));
            assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
        }
    }

    @Test
    @PortraitLandscape
    public void testShowMeminfoInRecentList() {
        startTestApps();
        if (FeatureOption.SPRD_SHOW_MEMINFO_SUPPORT.get()) {
            mLauncher.pressHome();
            assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            if (!isInSingleMode) {
                mLauncher.pressHome().switchToAllApps();
                assertTrue("Switch to All Apps", isInState(ALL_APPS));
                assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            }
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(OVERVIEW));
            assertTrue("has recents_memoryinfo", hasObject("recents_memoryinfo"));
        } else {
            mLauncher.pressHome();
            assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            if (!isInSingleMode) {
                mLauncher.pressHome().switchToAllApps();
                assertTrue("Switch to All Apps", isInState(ALL_APPS));
                assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
            }
            mLauncher.pressHome().switchToOverview();
            assertTrue("Switch to Overview", isInState(OVERVIEW));
            assertFalse("no recents_memoryinfo", hasObject("recents_memoryinfo"));
        }
    }
}
