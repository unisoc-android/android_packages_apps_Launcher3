package com.sprd.ext.dynamicicon;

import android.content.Context;
import android.os.Process;

import com.android.launcher3.R;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import static com.sprd.ext.dynamicicon.DynamicIconController.SEPARATOR;
import static com.sprd.ext.dynamicicon.DynamicIconSettings.PREF_KEY_GOOGLE_CALENDAR;
import static com.sprd.ext.dynamicicon.DynamicIconSettings.PREF_KEY_GOOGLE_CLOCK;
import static com.sprd.ext.dynamicicon.DynamicIconSettings.PREF_KEY_ORIGINAL_CALENDAR;
import static com.sprd.ext.dynamicicon.DynamicIconSettings.PREF_KEY_ORIGINAL_CLOCK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DynamicIconControllerTest {

    private Context mContext;
    private DynamicIconController mController;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        mContext = RuntimeEnvironment.application;
        LauncherAppMonitor monitor = LauncherAppMonitor.getInstance(mContext);
        mController = monitor.getDynamicIconController();
        assertNotNull(mController);
    }

    @After
    public void tearDown() throws Exception {
        mController = null;
    }

    @Test
    public void testInitDynamicInfos() {
        mController.clearDynamicIcons();

        // Check Unisoc calendar dynamic info
        String oriCalPkg = mContext.getString(R.string.original_calendar_pkg);
        mController.initDynamicInfos(PREF_KEY_ORIGINAL_CALENDAR, oriCalPkg, mController.mCalendarPkgs);
        assertEquals(mController.getPackageNameByPrefKey(PREF_KEY_ORIGINAL_CALENDAR), oriCalPkg);
        assertTrue(mController.getInstalledDynamicPkgs().contains(oriCalPkg));

        // Check Google calendar dynamic info
        String googleCalPkg = mContext.getString(R.string.google_calendar_pkg);
        mController.initDynamicInfos(PREF_KEY_GOOGLE_CALENDAR, googleCalPkg, mController.mCalendarPkgs);
        assertEquals(mController.getPackageNameByPrefKey(PREF_KEY_GOOGLE_CALENDAR), googleCalPkg);
        assertTrue(mController.getInstalledDynamicPkgs().contains(googleCalPkg));

        // Check Unisoc clock dynamic info
        String oriClockPkg = mContext.getString(R.string.original_clock_pkg);
        mController.initDynamicInfos(PREF_KEY_ORIGINAL_CLOCK, oriClockPkg, mController.mCalendarPkgs);
        assertEquals(mController.getPackageNameByPrefKey(PREF_KEY_ORIGINAL_CLOCK), oriClockPkg);
        assertTrue(mController.getInstalledDynamicPkgs().contains(oriClockPkg));

        // Check Google clock dynamic info
        String googleClockPkg = mContext.getString(R.string.google_clock_pkg);
        mController.initDynamicInfos(PREF_KEY_GOOGLE_CLOCK, googleClockPkg, mController.mCalendarPkgs);
        assertEquals(mController.getPackageNameByPrefKey(PREF_KEY_GOOGLE_CLOCK), googleClockPkg);
        assertTrue(mController.getInstalledDynamicPkgs().contains(googleClockPkg));
    }

    @Test
    public void testAddDynamicCallbacksForPkg() {
        mController.clearDynamicIcons();

        String oriCalPkg = mContext.getString(R.string.original_calendar_pkg);
        mController.mCalendarPkgs.add(oriCalPkg);
        mController.addDynamicCallbacksForPkg(oriCalPkg, Process.myUserHandle());

        assertEquals(1, mController.getInstalledDynamicPkgs().size());
        assertEquals(mController.getInstalledDynamicPkgs().get(0), oriCalPkg);
    }

    @Test
    public void testRemoveDynamicCallbacksForPkg() {
        String oriCalPkg = mContext.getString(R.string.original_calendar_pkg);
        assertTrue(mController.getInstalledDynamicPkgs().contains(oriCalPkg));

        mController.removeDynamicCallbacksForPkg(oriCalPkg, Process.myUserHandle());

        assertFalse(mController.getInstalledDynamicPkgs().contains(oriCalPkg));
    }


    @Test
    public void testGetSystemStateForPackage() {
        String oriCalPkg = mContext.getString(R.string.original_calendar_pkg);
        String OriSystemState = "test";

        mController.onSettingChanged(oriCalPkg, false);
        String systemState = mController.getSystemStateForPackage(OriSystemState, oriCalPkg);
        assertEquals(OriSystemState, systemState);

        mController.onSettingChanged(oriCalPkg, true);
        systemState = mController.getSystemStateForPackage(OriSystemState, oriCalPkg);
        assertNotEquals(OriSystemState, systemState);

        String[] state = systemState.split(SEPARATOR);
        assertEquals(Integer.toString(DynamicIconUtils.dayOfMonth()), state[state.length - 1]);
    }

}