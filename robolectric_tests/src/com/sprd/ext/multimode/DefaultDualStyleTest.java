package com.sprd.ext.multimode;

import com.android.launcher3.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class DefaultDualStyleTest extends BaseMultiModeStyleTestCase {

    @Before
    @Override
    public void setup() {
        setSingleLayerMode(false);
        super.setup();
    }

    @Test
    public void testDualStyleEnvironment() {
        // MultiModeController is not null.
        assertNotNull(mMonitor.getMultiModeController());

        // Launcher style is dual.
        assertFalse(MultiModeController.isSingleLayerMode());

        // Support dynamic change.
        assertEquals(MultiModeController.isSupportDynamicChange(),
                mTargetContext.getResources().getBoolean(R.bool.show_home_screen_style_settings));
    }

    @Test
    public void testDualDbName() {
        String dbName = mProvider.getDataBaseHelper().getDatabaseName();
        assertEquals("launcher.db", dbName);
    }
}
