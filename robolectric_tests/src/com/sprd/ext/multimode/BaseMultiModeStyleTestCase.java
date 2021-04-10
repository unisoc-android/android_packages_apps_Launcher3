package com.sprd.ext.multimode;

import android.content.Context;

import com.android.launcher3.LauncherProvider;
import com.android.launcher3.util.TestLauncherProvider;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;

import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLog;

public class BaseMultiModeStyleTestCase {
    protected Context mTargetContext;
    TestLauncherProvider mProvider;
    LauncherAppMonitor mMonitor;


    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        FeatureOption.SPRD_MULTI_MODE_SUPPORT.setForTests(true);
        mTargetContext = RuntimeEnvironment.application;
        LauncherAppMonitor.INSTANCE.initializeForTesting(null);
        mMonitor = LauncherAppMonitor.getInstance(mTargetContext);

        // Init launcher provider
        mProvider = Robolectric.setupContentProvider(TestLauncherProvider.class);
        ShadowContentResolver.registerProviderInternal(LauncherProvider.AUTHORITY, mProvider);
    }

    void setSingleLayerMode(boolean isSingleMode) {
        if (mTargetContext == null) {
            mTargetContext = RuntimeEnvironment.application;
        }
        MultiModeController.setSingleLayerMode(mTargetContext, isSingleMode);
    }
}
