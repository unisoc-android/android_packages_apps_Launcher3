/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.tapl;

import android.graphics.Point;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.android.launcher3.testing.TestProtocol;

/**
 * Ancestor for AppIcon and AppMenuItem.
 */
abstract class Launchable {
    private static final int WAIT_TIME_MS = 60000;
    protected final LauncherInstrumentation mLauncher;

    protected final UiObject2 mObject;

    Launchable(LauncherInstrumentation launcher, UiObject2 object) {
        mObject = object;
        mLauncher = launcher;
    }

    public UiObject2 getObject() {
        return mObject;
    }

    /**
     * Clicks the object to launch its app.
     */
    public Background launch(String expectedPackageName) {
        return launch(By.pkg(expectedPackageName));
    }

    private Background launch(BySelector selector) {
        LauncherInstrumentation.log("Launchable.launch before click " +
                mObject.getVisibleCenter() + " in " + mObject.getVisibleBounds());
        mLauncher.getTestInfo(TestProtocol.REQUEST_ENABLE_DEBUG_TRACING);
        mLauncher.assertTrue(
                "Launching an app didn't open a new window: " + mObject.getText(),
                mObject.clickAndWait(Until.newWindow(), WAIT_TIME_MS));
        mLauncher.getTestInfo(TestProtocol.REQUEST_DISABLE_DEBUG_TRACING);
        mLauncher.assertTrue(
                "App didn't start: " + selector,
                mLauncher.getDevice().wait(Until.hasObject(selector),
                        LauncherInstrumentation.WAIT_TIME_MS));
        return new Background(mLauncher);
    }

    /**
     * Drags an object to the center of homescreen.
     */
    public Workspace dragToWorkspace() {
        final Point launchableCenter = getObject().getVisibleCenter();
        final Point displaySize = mLauncher.getRealDisplaySize();
        final int width = displaySize.x / 2;
        Workspace.dragIconToWorkspace(
                mLauncher,
                this,
                new Point(
                        launchableCenter.x >= width ?
                                launchableCenter.x - width / 2 : launchableCenter.x + width / 2,
                        displaySize.y / 2),
                getLongPressIndicator());
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "dragged launchable to workspace")) {
            return new Workspace(mLauncher);
        }
    }

    /**
     * Drags an object to the position of workspace.
     */
    public void dragToWorkspacePosition(Point position) {
        Workspace.dragIconToWorkspace(
                mLauncher,
                this,
                position,
                getLongPressIndicator());
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "dragged launchable to workspace new screen.")) {
            new Workspace(mLauncher);
        }
    }

    /**
     * Drags an object over exist icon with folder.
     */
    public Workspace dragToHotseatOverExistIcon(Point center) {
        Workspace.dragIconToHotseat(
                mLauncher,
                this,
                center,
                getLongPressIndicator());
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "dragged launchable over exist icon with folder.")) {
            return new Workspace(mLauncher);
        }
    }

    /**
     * Drags a app icon over another icon to create folder.
     */
    public Workspace createFolderFromDragToAppIcon(String appName) {
        Point point = TestUtils.getAppVisibleCenter(appName);
        TestUtils.dragAppIconToCreateFolder(
                mLauncher,
                this,
                point,
                getLongPressIndicator());
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "create folder from drag a app icon to another app icon.")) {
            return new Workspace(mLauncher);
        }
    }

    protected abstract String getLongPressIndicator();
}
