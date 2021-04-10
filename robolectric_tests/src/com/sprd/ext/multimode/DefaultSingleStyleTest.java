package com.sprd.ext.multimode;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DefaultSingleStyleTest extends BaseMultiModeStyleTestCase {

    @Override
    public void setup() {
        setSingleLayerMode(true);
        super.setup();
    }

    @Test
    public void testSingleStyleEnvironment() {
        // MultiModeController is not null.
        assertNotNull(mMonitor.getMultiModeController());

        // Launcher style is single.
        assertTrue(MultiModeController.isSingleLayerMode());

        // Support dynamic change.
        assertEquals(MultiModeController.isSupportDynamicChange(),
                mTargetContext.getResources().getBoolean(R.bool.show_home_screen_style_settings));
    }

    @Test
    public void testSingleDbName() {
        String dbName = mProvider.getDataBaseHelper().getDatabaseName();
        assertEquals("sl_launcher.db", dbName);
    }

    @Test
    public void testPrefKey() {
        String key = "test";
        String singleValue = "single value";
        String dualValue = "dual value";
        String newSingleValue = "new single value";
        String newDualValue = "new dual value";

        // Test single mode
        String curKey = MultiModeController.getKeyByMode(mTargetContext, key);
        putPrefString(curKey, singleValue);
        assertEquals(singleValue, getPrefString(curKey));

        String preKey = MultiModeController.getKeyByPreMode(mTargetContext, key);
        putPrefString(preKey, dualValue);
        assertEquals(dualValue, getPrefString(preKey));

        // Test dual mode
        setSingleLayerMode(false);
        curKey = MultiModeController.getKeyByMode(mTargetContext, key);
        assertEquals(dualValue, getPrefString(curKey));
        putPrefString(curKey, newDualValue);

        preKey = MultiModeController.getKeyByPreMode(mTargetContext, key);
        assertEquals(singleValue, getPrefString(preKey));
        putPrefString(preKey, newSingleValue);

        // ReTest single mode
        setSingleLayerMode(true);
        curKey = MultiModeController.getKeyByMode(mTargetContext, key);
        assertEquals(newSingleValue, getPrefString(curKey));

        preKey = MultiModeController.getKeyByPreMode(mTargetContext, key);
        assertEquals(newDualValue, getPrefString(preKey));
    }

    private String getPrefString(String key) {
        return Utilities.getPrefs(mTargetContext).getString(key, "");
    }

    private void putPrefString(String key, String value) {
        Utilities.getPrefs(mTargetContext).edit()
                .putString(key, value)
                .commit();
    }

}
