package com.android.launcher3.tapl;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.android.launcher3.testing.TestProtocol;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

public class TestUtils {
    private static final long DEFAULT_UI_TIMEOUT = 10000;

    private static final int QUICK_DRAG_DURACTION = 350;
    private static final int CREATE_FOLDER_DURACTION = 1000;

    private static UiDevice getDevice() {
        return UiDevice.getInstance(getInstrumentation());
    }

    static Point getAppVisibleCenter(String appName) {
        final UiDevice device = getDevice();
        return device.wait(Until.findObject(By.text(appName)), DEFAULT_UI_TIMEOUT).getVisibleCenter();
    }

    static void dragAppIconToCreateFolder(
            LauncherInstrumentation launcher, Launchable launchable, Point dest,
            String longPressIndicator) {
        launcher.getTestInfo(TestProtocol.REQUEST_ENABLE_DEBUG_TRACING);
        LauncherInstrumentation.log("dragAppIconToCreateFolder: begin");
        final Point launchableCenter = launchable.getObject().getVisibleCenter();
        final long downTime = SystemClock.uptimeMillis();
        launcher.sendPointer(downTime, downTime, MotionEvent.ACTION_DOWN, launchableCenter);
        LauncherInstrumentation.log("dragAppIconToCreateFolder: sent down");
        launcher.waitForLauncherObject(longPressIndicator);
        LauncherInstrumentation.log("dragAppIconToCreateFolder: indicator");
        launcher.movePointer(
                downTime, SystemClock.uptimeMillis(), QUICK_DRAG_DURACTION, launchableCenter, dest);
        LauncherInstrumentation.sleep(CREATE_FOLDER_DURACTION);
        LauncherInstrumentation.log("dragAppIconToCreateFolder: moved pointer");
        launcher.sendPointer(
                downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, dest);
        LauncherInstrumentation.log("dragAppIconToCreateFolder: end");
        launcher.waitUntilGone("drop_target_bar");
        launcher.getTestInfo(TestProtocol.REQUEST_DISABLE_DEBUG_TRACING);
    }
}
