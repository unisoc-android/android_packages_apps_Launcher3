package com.sprd.ext.folder;

import android.content.Context;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherSettingsExtension;
import com.sprd.ext.multimode.MultiModeController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class FolderIconControllerTest {
    private static final int TEST_ANNULAR_FOLDER_ROWS = 5;
    private static final int TEST_ANNULAR_FOLDER_COLUMNS = 6;

    protected Context mTargetContext;
    private FolderIconController mFic;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        mTargetContext = RuntimeEnvironment.application;

        FeatureOption.SPRD_FOLDER_ICON_MODE_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor monitor = LauncherAppMonitor.getInstance(mTargetContext);
        mFic = monitor.getFolderIconController();
        assertNotNull(mFic);
    }

    private void setFolderStyle(String style) {
        if (mTargetContext == null) {
            mTargetContext = RuntimeEnvironment.application;
        }
        Utilities.getPrefs(mTargetContext).edit()
                .putString(LauncherSettingsExtension.PREF_FOLDER_ICON_MODE_KEY, style)
                .commit();
    }

    @Test
    public void testGridFolder() {
        runFolderModelTest(FolderIconController.GRID);
    }

    @Test
    public void testAnnularFolder() {
        runFolderModelTest(FolderIconController.ANNULAR);
    }

    @Test
    public void testAutoFolder() {
        MultiModeController.setSingleLayerMode(mTargetContext, true);
        runFolderModelTest(FolderIconController.AUTO);

        MultiModeController.setSingleLayerMode(mTargetContext, false);
        runFolderModelTest(FolderIconController.AUTO);
    }

    private void runFolderModelTest(String style) {
        mFic.setFolderIconModel(style);
        setFolderStyle(style);

        switch (style) {
            case FolderIconController.GRID:
            case FolderIconController.ANNULAR:
                assertEquals(style, mFic.getFolderIconModel());
                assertEquals(style, mFic.getFolderIconModelFromPref());
                break;
            case FolderIconController.AUTO:
                if (MultiModeController.isSingleLayerMode()) {
                    assertEquals(FolderIconController.GRID, mFic.getFolderIconModel());
                    assertEquals(FolderIconController.GRID, mFic.getFolderIconModelFromPref());
                } else {
                    assertEquals(FolderIconController.ANNULAR, mFic.getFolderIconModel());
                    assertEquals(FolderIconController.ANNULAR, mFic.getFolderIconModelFromPref());
                }
                break;
            default:
                break;
        }
    }

    @Test
    public void testFolderRowAndColumns() {
        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(mTargetContext);
        idp.numFolderRows = TEST_ANNULAR_FOLDER_ROWS;
        idp.numFolderColumns = TEST_ANNULAR_FOLDER_COLUMNS;

        mFic.backupOriginalFolderRowAndColumns(idp.numFolderRows, idp.numFolderColumns);

        mFic.setFolderIconModel(FolderIconController.GRID);
        mFic.updateFolderRowAndColumns(idp);

        assertEquals(mTargetContext.getResources().getInteger(R.integer.grid_folder_page_rows), idp.numFolderRows);
        assertEquals(mTargetContext.getResources().getInteger(R.integer.grid_folder_icon_columns), idp.numFolderColumns);

        mFic.setFolderIconModel(FolderIconController.ANNULAR);
        mFic.updateFolderRowAndColumns(idp);

        assertEquals(TEST_ANNULAR_FOLDER_ROWS, idp.numFolderRows);
        assertEquals(TEST_ANNULAR_FOLDER_COLUMNS, idp.numFolderColumns);
    }
}
