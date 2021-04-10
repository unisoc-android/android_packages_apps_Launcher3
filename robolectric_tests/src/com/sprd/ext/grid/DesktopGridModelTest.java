package com.sprd.ext.grid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import com.android.launcher3.Utilities;
import com.sprd.ext.LauncherAppMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DesktopGridModelTest {

    private Context mContext;
    private DesktopGridController mController;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        mContext = RuntimeEnvironment.application;
        mController = spy(new DesktopGridController(mContext, mock(LauncherAppMonitor.class)));
    }

    @After
    public void tearDown() {
        mController = null;
    }

    @Test
    public void testGetGridOptions() {
        List<String> gridNames = mController.getGridOptionNames();
        assertNotNull(gridNames);
        int gridNameSize = gridNames.size();
        assertTrue(gridNameSize > 0);

        List<String> gridEntries = mController.getGridOptionEntries();
        assertNotNull(gridEntries);
        int gridEntriesSize = gridEntries.size();
        assertTrue(gridEntries.size() > 0);
        assertEquals(gridEntriesSize, gridNameSize);
    }

    @Test
    public void testInitPreference_show_grid_pref() {
        final String listPrefKey = "test1";
        final ListPreference listPref = spy(new ListPreference(mContext));
        doNothing().when(listPref).setValue(anyString());
        listPref.setKey(listPrefKey);

        final TestPreferenceGroup prefScreen = spy(new TestPreferenceGroup(mContext, null));
        final PreferenceManager prefManager = mock(PreferenceManager.class);
        doReturn(prefManager).when(prefScreen).getPreferenceManager();
        when(prefManager.getSharedPreferences()).thenReturn(mock(SharedPreferences.class));
        prefScreen.addPreference(listPref);

        mController.initPreference(listPref);
        assertNotNull(prefScreen.findPreference(listPrefKey));
    }

    @Test
    public void testInitPreference_show_grid_pref_with_only_one_option() {
        Utilities.setDeveloperModeEnableForTesting(true);

        ArrayList<String> gridEntryList = new ArrayList<>(1);
        gridEntryList.add("4x4");
        when(mController.getGridOptionEntries()).thenReturn(gridEntryList);
        ArrayList<String> gridNameList = new ArrayList<>(1);
        gridNameList.add("4_by_4");
        when(mController.getGridOptionNames()).thenReturn(gridNameList);

        ListPreference listPref = new ListPreference(mContext);
        // Avoid NullPointerException in {@link Preference#persistString}
        listPref.setPersistent(false);
        final String listPrefKey = "test2";
        listPref.setKey(listPrefKey);

        final TestPreferenceGroup prefScreen = spy(new TestPreferenceGroup(mContext, null));
        final PreferenceManager prefManager = mock(PreferenceManager.class);
        doReturn(prefManager).when(prefScreen).getPreferenceManager();
        when(prefManager.getSharedPreferences()).thenReturn(mock(SharedPreferences.class));
        prefScreen.addPreference(listPref);

        mController.initPreference(listPref);
        assertNotNull(prefScreen.findPreference(listPrefKey));
        assertEquals(1, listPref.getEntries().length);

        Utilities.setDeveloperModeEnableForTesting(false);
    }

    @Test
    public void testInitPreference_remove_grid_pref() {
        when(mController.getGridOptionEntries()).thenReturn(new ArrayList<>());

        ListPreference listPref = new ListPreference(mContext);
        final String listPrefKey = "test3";
        listPref.setKey(listPrefKey);

        final TestPreferenceGroup prefScreen = spy(new TestPreferenceGroup(mContext, null));
        final PreferenceManager prefManager = mock(PreferenceManager.class);
        doReturn(prefManager).when(prefScreen).getPreferenceManager();
        when(prefManager.getSharedPreferences()).thenReturn(mock(SharedPreferences.class));
        prefScreen.addPreference(listPref);

        mController.initPreference(listPref);
        assertNull(prefScreen.findPreference(listPrefKey));
    }

    public static class TestPreferenceGroup extends PreferenceGroup {

        public TestPreferenceGroup(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }
}
