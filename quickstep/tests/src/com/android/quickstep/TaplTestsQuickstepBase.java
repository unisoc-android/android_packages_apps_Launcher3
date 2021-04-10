package com.android.quickstep;

import static com.android.launcher3.ui.TaplTestsLauncher3.getAppPackageName;

import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;

import com.android.launcher3.Launcher;
import com.android.launcher3.ui.TaplTestsLauncher3;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.system.ActivityManagerWrapper;

import org.junit.Before;

public class TaplTestsQuickstepBase extends AbstractQuickStepTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ActivityManagerWrapper.getInstance().removeAllRecentTasks();
        TaplTestsLauncher3.initialize(this);
    }

    void startTestApps() {
        startAppFast(getAppPackageName());
        startAppFast(resolveSystemApp(Intent.CATEGORY_APP_CALCULATOR));
        startTestActivity(2);
        executeOnLauncher(launcher -> assertTrue(
                "Launcher activity is the top activity; expecting another activity to be the top "
                        + "one",
                isInBackground(launcher)));
    }

    int getTaskCount(Launcher launcher) {
        return launcher.<RecentsView>getOverviewPanel().getTaskViewCount();
    }

    BySelector getSelector(String resName) {
        return By.res(mDevice.getLauncherPackageName(), resName);
    }

    boolean hasObject(String resName) {
        return mDevice.hasObject(getSelector(resName));
    }
}
