package com.sprd.ext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for {@link LauncherAppMonitor}
 */
@RunWith(RobolectricTestRunner.class)
public class LauncherAppMonitorTest {

    @Spy
    protected Context mContext = RuntimeEnvironment.application.getApplicationContext();

    @Test
    public void testAllFeaturesOpened() {
        initAllFeaturesState(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor monitor = LauncherAppMonitor.getInstance(mContext);

        assertNotNull(monitor.getMultiModeController());
        assertNotNull(monitor.getDesktopGridController());
        assertNotNull(monitor.getNotifiDotsNumController());
        assertNotNull(monitor.getUnreadInfoController());
        assertNotNull(monitor.getDynamicIconController());
        assertNotNull(monitor.getGesturesController());
        assertNotNull(monitor.getCycleScrollController());
        assertNotNull(monitor.getFolderIconController());
        assertNotNull(monitor.getHotseatController());
        assertNotNull(monitor.getCustomizeAppSortController());
        assertNotNull(monitor.getIconLabelController());
    }

    @Test
    public void testAllFeaturesClosed() {
        initAllFeaturesState(false);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor monitor = LauncherAppMonitor.getInstance(mContext);

        assertNull(monitor.getMultiModeController());
        assertNull(monitor.getDesktopGridController());
        assertNull(monitor.getNotifiDotsNumController());
        assertNull(monitor.getUnreadInfoController());
        assertNull(monitor.getDynamicIconController());
        assertNull(monitor.getGesturesController());
        assertNull(monitor.getCycleScrollController());
        assertNull(monitor.getFolderIconController());
        assertNull(monitor.getHotseatController());
        assertNull(monitor.getCustomizeAppSortController());
        assertNull(monitor.getIconLabelController());
    }

    private static void initAllFeaturesState(boolean enable) {
        FeatureOption.SPRD_MULTI_MODE_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_NOTIFICATION_DOT_COUNT.setForTests(enable);
        FeatureOption.SPRD_BADGE_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_DISABLE_ROTATION.setForTests(enable);
        FeatureOption.SPRD_GESTURE_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_CYCLE_SCROLL_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_FOLDER_ICON_MODE_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_HOTSEAT_ICON_ADAPTIVE_LAYOUT.setForTests(enable);
        FeatureOption.SPRD_ALLAPP_CUSTOMIZE_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_TASK_LOCK_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_SHOW_MEMINFO_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_SHOW_CLEAR_MEM_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_APP_REMOTE_ANIM_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_FAST_UPDATE_LABEL.setForTests(enable);
        FeatureOption.SPRD_ALLAPP_FUZZY_SEARCH_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_DEBUG_SEARCH_CUSTOMIZE_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_ALLAPP_BG_TRANSPARENT_SUPPORT.setForTests(enable);
        FeatureOption.SPRD_ICON_LABEL_LINE_SUPPORT.setForTests(enable);
    }
}
