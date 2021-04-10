package com.sprd.ext.folder;

import android.graphics.Point;
import android.view.KeyEvent;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.tapl.AppIcon;
import com.android.launcher3.tapl.Workspace;
import com.android.launcher3.ui.AbstractLauncherUiTest;
import com.android.launcher3.ui.TaplTestsLauncher3;
import com.android.launcher3.ui.TestViewHelpers;
import com.android.launcher3.views.ScrimView;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.multimode.MultiModeController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class FolderUiTest extends AbstractLauncherUiTest {
    private static final String FOLDER_ICON_STYLE = "Folder icon style";
    private static final String AUTO_STYLE = "Auto";
    private static final String ANNULAR_STYLE = "Annular";
    private static final String GRID_STYLE = "Grid";

    private static final String FIRST_DRAG_APP_NAME = "Phone";
    private static final Pattern SECOND_DRAG_APP_NAME = Pattern.compile("Messages|Messaging");

    private FolderIconController mFic;

    @Before
    public void setup() throws Exception {
        super.setUp();
        // Do not test this file if feature is off.
        assumeTrue(FeatureOption.SPRD_FOLDER_ICON_MODE_SUPPORT.get());

        TaplTestsLauncher3.initialize(this);

        Utilities.setDeveloperModeEnableForTesting(true);
    }

    public void initializeFolder() {
        LauncherAppMonitor monitor = LauncherAppMonitor.getInstance(mTargetContext);
        mFic = monitor.getFolderIconController();
        assertNotNull(mFic);

        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(mTargetContext);
        mFic.backupOriginalFolderRowAndColumns(idp.numFolderRows, idp.numFolderColumns);

        createFolderOnWorkspace();
    }

    private void createFolderOnWorkspace() {
        Workspace workspace = mLauncher.getWorkspace();
        AppIcon firstApp = workspace.getHotseatAppIcon(FIRST_DRAG_APP_NAME);
        firstApp.dragToWorkspace();

        AppIcon secondApp = workspace.getHotseatAppIcon(SECOND_DRAG_APP_NAME);
        secondApp.createFolderFromDragToAppIcon(FIRST_DRAG_APP_NAME);

        // Find folder and open folder.
        mDevice.wait(Until.findObject(By.desc("Folder: ")),
                AbstractLauncherUiTest.DEFAULT_UI_TIMEOUT).click();

        // Find folder name and edit the folder name
        TestViewHelpers.findViewByEntryName("folder_name").click();
        mDevice.pressKeyCode(KeyEvent.KEYCODE_A);
        mDevice.pressHome();
    }

    public void changeFolderStyle(String styleText) throws UiObjectNotFoundException {
        TestViewHelpers.openLauncherSettings();
        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        settings.scrollTextIntoView(FOLDER_ICON_STYLE);
        mDevice.wait(Until.findObject(By.text(FOLDER_ICON_STYLE)), DEFAULT_UI_TIMEOUT).click();
        mDevice.wait(Until.hasObject(By.clazz(ListPreference.class).depth(0)), SHORT_UI_TIMEOUT);
        mDevice.wait(Until.findObject(By.text(styleText)), DEFAULT_UI_TIMEOUT).click();
        mDevice.pressHome();
        waitForState("Launcher internal state didn't switch to Home", LauncherState.NORMAL);
    }

    public void runTestFolderStyle(String styleText) throws Exception {
        changeFolderStyle(styleText);

        getDevice().wait(Until.findObject(By.text("a")),
                AbstractLauncherUiTest.DEFAULT_UI_TIMEOUT).click();

        Point folderContentPoint = TestViewHelpers.findViewByEntryName("folder_content").getVisibleCenter();
        Point folderNamePoint = TestViewHelpers.findViewByEntryName("folder_name").getVisibleCenter();

        switch (mFic.getFolderIconModel()) {
            case FolderIconController.GRID:
                assertTrue(folderContentPoint.y > folderNamePoint.y);
                executeOnLauncher(launcher -> {
                    assertEquals(launcher.getWorkspace().getVisibility(), View.INVISIBLE);
                    assertEquals(launcher.getWorkspace().getPageIndicator().getVisibility(), View.INVISIBLE);
                    assertEquals(launcher.getHotseat().getVisibility(), View.INVISIBLE);
                    ScrimView scrimView = launcher.findViewById(R.id.scrim_view);
                    if (scrimView != null) {
                        assertEquals(scrimView.getVisibility(), View.INVISIBLE);
                    }
                });
                break;
            case FolderIconController.ANNULAR:
                assertTrue(folderContentPoint.y < folderNamePoint.y);
                executeOnLauncher(launcher -> {
                    assertEquals(launcher.getWorkspace().getVisibility(), View.VISIBLE);
                    assertEquals(launcher.getHotseat().getVisibility(), View.VISIBLE);
                });
                break;
            default:
                break;
        }

        if (styleText.equals(AUTO_STYLE)) {
            if (MultiModeController.isSingleLayerMode(mTargetContext)) {
                assertEquals(FolderIconController.GRID, mFic.getFolderIconModelFromPref());
                assertEquals(FolderIconController.GRID, mFic.getFolderIconModel());
            } else {
                assertEquals(FolderIconController.ANNULAR, mFic.getFolderIconModelFromPref());
                assertEquals(FolderIconController.ANNULAR, mFic.getFolderIconModel());
            }
        }
    }

    @Test
    public void testGridFolder() throws Exception {
        initializeFolder();
        if (mFic.isGridFolderIcon()) {
            changeFolderStyle(ANNULAR_STYLE);
        }
        runTestFolderStyle(GRID_STYLE);
    }

    @Test
    public void testAnnularFolder() throws Exception {
        initializeFolder();
        if (mFic.isNativeFolderIcon()) {
            changeFolderStyle(GRID_STYLE);
        }
        runTestFolderStyle(ANNULAR_STYLE);
    }

    @Test
    public void testAutoFolder() throws Exception {
        assumeTrue(MultiModeController.isSupportDynamicChange());

        initializeFolder();
        if (MultiModeController.isSingleLayerMode(mTargetContext)) {
            changeFolderStyle(ANNULAR_STYLE);
        } else {
            changeFolderStyle(GRID_STYLE);
        }
        runTestFolderStyle(AUTO_STYLE);
    }

    @Test
    public void testFolderIconStylePrefUi() throws Exception {
        TestViewHelpers.openLauncherSettings();
        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        settings.scrollTextIntoView(FOLDER_ICON_STYLE);
        assertTrue(mDevice.wait(Until.hasObject(By.text(FOLDER_ICON_STYLE)), DEFAULT_UI_TIMEOUT));
        mDevice.pressBack();

        FeatureOption.SPRD_FOLDER_ICON_MODE_SUPPORT.setForTests(false);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        LauncherAppMonitor.getInstance(mTargetContext);
        TestViewHelpers.openLauncherSettings();
        settings.scrollTextIntoView(FOLDER_ICON_STYLE);
        assertFalse(mDevice.wait(Until.hasObject(By.text(FOLDER_ICON_STYLE)), SHORT_UI_TIMEOUT));
        mDevice.pressBack();

        // restore
        FeatureOption.SPRD_FOLDER_ICON_MODE_SUPPORT.setForTests(true);
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
    }

    @Test
    public void testFolderIconAutoStylePrefUi() throws Exception {
        MultiModeController.setSupportDynamicChange(true);
        TestViewHelpers.openLauncherSettings();
        final UiScrollable settings = new UiScrollable(
                new UiSelector().className("android.widget.FrameLayout"));
        settings.scrollTextIntoView(FOLDER_ICON_STYLE);
        mDevice.wait(Until.findObject(By.text(FOLDER_ICON_STYLE)), DEFAULT_UI_TIMEOUT).click();
        assertTrue(mDevice.wait(Until.hasObject(By.text(AUTO_STYLE)), DEFAULT_UI_TIMEOUT));
        mDevice.pressBack();
        mDevice.pressBack();

        MultiModeController.setSupportDynamicChange(false);
        TestViewHelpers.openLauncherSettings();
        settings.scrollTextIntoView(FOLDER_ICON_STYLE);
        mDevice.wait(Until.findObject(By.text(FOLDER_ICON_STYLE)), DEFAULT_UI_TIMEOUT).click();
        assertFalse(mDevice.wait(Until.hasObject(By.text(AUTO_STYLE)), SHORT_UI_TIMEOUT));
    }
}
